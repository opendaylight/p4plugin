/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.NodeGrpcInfo;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.NodeInterfacesState;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataProcess {

    private static final Logger LOG = LoggerFactory.getLogger(DataProcess.class);

    private final DataBroker dataBroker;
    private MountPointService mountPointService = null;

    private static final InstanceIdentifier<Topology> NETCONF_TOPO_IID = InstanceIdentifier
            .create(NetworkTopology.class).child(Topology.class,
                    new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    public DataProcess(DataBroker dataBroker, MountPointService mountPointService) {
        this.dataBroker = dataBroker;
        this.mountPointService = mountPointService;
    }

    public InterfacesState readInterfaces(String nodeId, InstanceIdentifier<InterfacesState> path) {
        LOG.info("Get dataBroker");
        final DataBroker nodeDataBroker = getDataBroker(nodeId, mountPointService);
        if (null == nodeDataBroker) {
            LOG.info("Data broker is null, return");
            return null;
        }
        LOG.info("Process read data");
        return readData(nodeDataBroker, path);
    }

    public NodeGrpcInfo readGrpcInfo(String nodeId, InstanceIdentifier<NodeGrpcInfo> path) {
        final DataBroker nodeDataBroker = getDataBroker(nodeId, mountPointService);
        if (null == nodeDataBroker) {
            return null;
        }
        return readData(nodeDataBroker, path);
    }

    public void writeToDataStore(String nodeId, InterfacesState interfacesData, NodeGrpcInfo grpcInfo,
                                 InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.p4plugin.netconf
                                         .adapter.rev170908.node.interfaces.state.Node> path) {
        org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.Node node =
                constructNode(nodeId, interfacesData, grpcInfo);
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        LOG.info("Write to controller data store");
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, path, node, true);
        LOG.info("Submit");
        writeTransaction.submit();
    }

    private org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.Node
            constructNode(String nodeId, InterfacesState interfacesData, NodeGrpcInfo grpcInfo) {
        NodeBuilder builder = new NodeBuilder();
        builder.setKey(new org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node
                .interfaces.state.NodeKey(nodeId));
        builder.setNodeId(nodeId);
        builder.setGrpcServerIp(grpcInfo.getIp());
        builder.setGrpcServerPort(grpcInfo.getPort());
        builder.setDeviceId(grpcInfo.getId());
        builder.setInterface(constructInterfaceInfo(interfacesData));
        return builder.build();
    }

    private List<Interface> constructInterfaceInfo(InterfacesState interfacesData) {
        List<Interface> list = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                     .interfaces.state.Interface interfaceData : interfacesData.getInterface()) {
            InterfaceBuilder builder = new InterfaceBuilder();
            builder.setKey(new InterfaceKey(interfaceData.getKey().toString()));
            builder.setName(interfaceData.getName());
            builder.setType(InterfaceType.class);
            if (interfaceData.getAdminStatus().getIntValue() == 1) {
                builder.setAdminStatus(Interface.AdminStatus.Up);
            } else if (interfaceData.getAdminStatus().getIntValue() == 2) {
                builder.setAdminStatus(Interface.AdminStatus.Down);
            } else {
                builder.setAdminStatus(Interface.AdminStatus.Testing);
            }
            if (interfaceData.getOperStatus().getIntValue() == 1) {
                builder.setOperStatus(Interface.OperStatus.Up);
            } else if (interfaceData.getOperStatus().getIntValue() == 2) {
                builder.setOperStatus(Interface.OperStatus.Down);
            } else if (interfaceData.getOperStatus().getIntValue() == 3) {
                builder.setOperStatus(Interface.OperStatus.Testing);
            } else if (interfaceData.getOperStatus().getIntValue() == 4) {
                builder.setOperStatus(Interface.OperStatus.Unknown);
            } else if (interfaceData.getOperStatus().getIntValue() == 5) {
                builder.setOperStatus(Interface.OperStatus.Dormant);
            } else if (interfaceData.getOperStatus().getIntValue() == 6) {
                builder.setOperStatus(Interface.OperStatus.NotPresent);
            } else {
                builder.setOperStatus(Interface.OperStatus.LowerLayerDown);
            }
            builder.setIfIndex(interfaceData.getIfIndex());
            builder.setPhysAddress(interfaceData.getPhysAddress());
            builder.setSpeed(interfaceData.getSpeed());

            list.add(builder.build());
        }
        return list;
    }

    public NodeInterfacesState readFromDataStore(InstanceIdentifier<NodeInterfacesState> path) {
        final ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();
        Optional<NodeInterfacesState> interfaces = null;
        try {
            interfaces = readTransaction.read(LogicalDatastoreType.OPERATIONAL, path).checkedGet();
            if (interfaces.isPresent()) {
                LOG.info("NodeInterfacesState from controller data store is not null");
                return interfaces.get();
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        LOG.info("NodeInterfacesState from controller data store is null");
        return null;
    }

    private static DataBroker getDataBroker(String nodeId, MountPointService mountPointService) {
        LOG.info("Get mountPoint");
        MountPoint mountPoint = getMountPoint(nodeId, mountPointService);
        if (null == mountPoint) {
            LOG.info("MountPoint is null");
            return null;
        }
        LOG.info("Process get dataBroker");
        Optional<DataBroker> nodeDataBroker = mountPoint.getService(DataBroker.class);

        if (!nodeDataBroker.isPresent()) {
            LOG.info("DataBroker is not present");
            return null;
        }
        return nodeDataBroker.get();
    }

    private static MountPoint getMountPoint(String nodeId, MountPointService mountPointService) {
        if (null == mountPointService) {
            return null;
        }
        Optional<MountPoint> nodeMountPoint = mountPointService.getMountPoint(NETCONF_TOPO_IID
                .child(Node.class, new NodeKey(new NodeId(nodeId))));

        if (!nodeMountPoint.isPresent()) {
            return null;
        }
        return nodeMountPoint.get();
    }

    private <T extends DataObject> T readData(DataBroker nodeDataBroker, InstanceIdentifier<T> path) {
        final ReadTransaction readTransaction = nodeDataBroker.newReadOnlyTransaction();
        Optional<T> optionalData = null;
        try {
            optionalData = readTransaction.read(LogicalDatastoreType.OPERATIONAL, path).checkedGet();
            if (optionalData.isPresent()) {
                LOG.info("Data is not null");
                return optionalData.get();
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        LOG.info("Data is null");
        return null;
    }
}
