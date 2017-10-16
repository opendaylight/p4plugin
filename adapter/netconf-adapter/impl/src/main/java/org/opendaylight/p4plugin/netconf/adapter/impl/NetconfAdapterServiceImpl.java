/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.P4pluginNetconfAdapterApiService;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.ReadInventoryInput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.ReadInventoryOutput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.ReadInventoryOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.WriteInventoryInput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.WriteInventoryOutput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.WriteInventoryOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.NodeInterfacesState;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NetconfAdapterServiceImpl implements P4pluginNetconfAdapterApiService {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfAdapterServiceImpl.class);

    private final DataBroker dataBroker;
    private DeviceInterfaceDataOperator deviceInterfaceDataOperator;

    public NetconfAdapterServiceImpl(DataBroker dataBroker, DeviceInterfaceDataOperator deviceInterfaceDataOperator) {
        this.dataBroker = dataBroker;
        this.deviceInterfaceDataOperator = deviceInterfaceDataOperator;
    }

    @Override
    public Future<RpcResult<WriteInventoryOutput>> writeInventory(WriteInventoryInput var1) {
        LOG.info("Acquire interfaces data from controller data store");
        NodeInterfacesState data = deviceInterfaceDataOperator.readInterfacesFromControllerDataStore();
        WriteInventoryOutputBuilder outputBuilder = new WriteInventoryOutputBuilder();
        if (null == data || null == data.getNode()) {
            outputBuilder.setMessage("No data in controller data store");
            return Futures.immediateFuture(RpcResultBuilder.success(outputBuilder.build()).build());
        }

        LOG.info("Data is {}", data);
        boolean result = false;
        for (Node node : data.getNode()) {
            result = writeNodeToInventory(node);
            if (false == result) {
                outputBuilder.setMessage("Write data to inventory failed");
                return Futures.immediateFuture(RpcResultBuilder.success(outputBuilder.build()).build());
            }
        }

        outputBuilder.setMessage("Write data to inventory success");
        return Futures.immediateFuture(RpcResultBuilder.success(outputBuilder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadInventoryOutput>> readInventory(ReadInventoryInput var1) {
        LOG.info("Start read inventory data");
        InstanceIdentifier<Nodes> path = InstanceIdentifier.create(Nodes.class);
        final ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();
        Optional<Nodes> nodes = null;
        ReadInventoryOutputBuilder builder = new ReadInventoryOutputBuilder();
        try {
            nodes = readTransaction.read(LogicalDatastoreType.OPERATIONAL, path).checkedGet();
            if (nodes.isPresent()) {
                LOG.info("Data from inventory data store is {}", nodes.get());
                builder.setMessage("Read data from inventory data store success");
                return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        builder.setMessage("Data from inventory data store is null");
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    private boolean writeNodeToInventory(Node node) {
        LOG.info("Get node path");
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> path =
                getNodePath(node.getNodeId());
        if (null == path) {
            LOG.info("Path not exit");
            return false;
        }
        LOG.info("Convert node");
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node inventoryNode = convertData(node);
        LOG.info("Inventory node is {}", inventoryNode);
        if (null == inventoryNode || null == inventoryNode.getNodeConnector()
                || 0 == inventoryNode.getNodeConnector().size()) {
            LOG.info("Data converted failed");
            return false;
        }
        LOG.info("Write");
        return write(dataBroker, inventoryNode, path);
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819
            .nodes.Node> getNodePath(String nodeId) {
        return InstanceIdentifier.create(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        new NodeKey(new NodeId(nodeId)));
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node convertData(Node node) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(new NodeKey(new NodeId(node.getNodeId())));
        nodeBuilder.setId(new NodeId(node.getNodeId()));

        List<NodeConnector> list = new ArrayList<>();
        for (Interface infce : node.getInterface()) {
            NodeConnectorBuilder connectorBuilder = new NodeConnectorBuilder();
            connectorBuilder.setKey(new NodeConnectorKey(new NodeConnectorId(infce.getName())));
            connectorBuilder.setId(new NodeConnectorId(infce.getName()));
            list.add(connectorBuilder.build());
        }

        nodeBuilder.setNodeConnector(list);
        return nodeBuilder.build();
    }

    private boolean write(DataBroker dataBroker, org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819
            .nodes.Node node, InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819
            .nodes.Node> path) {
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, path, node, true);
        try {
            writeTransaction.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Inventory:write DS failed", e);
            return false;
        }
        return true;
    }

}
