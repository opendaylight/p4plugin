/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
//import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.ReadInventoryInput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.ReadInventoryInputBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.ReadInventoryOutput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.WriteInventoryInput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.WriteInventoryInputBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.api.rev170908.WriteInventoryOutput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.NodeInterfacesState;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.Node;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class NetconfAdapterServiceImplTest extends AbstractDataBrokerTest {

    @Mock
    private MountPoint mountPoint;
    @Mock
    private MountPointService mountPointService;
    @Mock
    private RpcProviderRegistry rpcProviderRegistry;
    private DataProcess dataProcess;
    private DeviceInterfaceDataOperator deviceInterfaceDataOperator;
    private NetconfAdapterServiceImpl netconfAdapterServiceImpl;

    private static final NodeId NODE_ID = new NodeId("device0");
    private static final InstanceIdentifier<Node> NODE_PATH = InstanceIdentifier
            .create(NodeInterfacesState.class).child(Node.class, new NodeKey(NODE_ID.getValue()));
    private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
            .rev130819.nodes.Node> INVENTORY_NODE_ID = InstanceIdentifier.create(Nodes.class)
            .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                        new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(
                                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(
                                        NODE_ID.getValue())));

    @Before
    public void setUp() throws Exception {
        dataProcess = new DataProcess(getDataBroker(), mountPointService);
        deviceInterfaceDataOperator = new DeviceInterfaceDataOperator(dataProcess, rpcProviderRegistry);
        netconfAdapterServiceImpl = new NetconfAdapterServiceImpl(getDataBroker(), deviceInterfaceDataOperator);
    }

    @Test
    public void testWriteInventoryOne() throws Exception {
        setUp();

        Future<RpcResult<WriteInventoryOutput>> result = netconfAdapterServiceImpl
                .writeInventory(constructWriteInput());
        Assert.assertTrue(result.get().isSuccessful());
        Assert.assertEquals(result.get().getResult().getMessage(), "No data in controller data store");
    }

    @Test
    public void testWriteInventoryTwo() throws Exception {
        setUp();
        Node node = constructNode(NODE_ID.getValue(), "10.42.89.15", 50051, "1", constructInterfaces());
        writeInfoToDS(NODE_PATH, node);

        Future<RpcResult<WriteInventoryOutput>> result = netconfAdapterServiceImpl
                .writeInventory(constructWriteInput());
        Assert.assertTrue(result.get().isSuccessful());
        Assert.assertEquals(result.get().getResult().getMessage(), "Write data to inventory success");
    }

    @Test
    public void testReadInventoryOne() throws Exception {
        setUp();

        Future<RpcResult<ReadInventoryOutput>> result = netconfAdapterServiceImpl
                .readInventory(constructReadInput());
        Assert.assertTrue(result.get().isSuccessful());
        Assert.assertEquals(result.get().getResult().getMessage(), "Data from inventory data store is null");
    }

    @Test
    public void testReadInventoryTwo() throws Exception {
        setUp();

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
                inventoryNode = constructInventoryData(NODE_ID.getValue());
        writeInfoToDS(INVENTORY_NODE_ID, inventoryNode);
        Future<RpcResult<ReadInventoryOutput>> result = netconfAdapterServiceImpl
                .readInventory(constructReadInput());
        Assert.assertTrue(result.get().isSuccessful());
        Assert.assertEquals(result.get().getResult().getMessage(), "Read data from inventory data store success");
    }

    private <T extends DataObject> void writeInfoToDS(InstanceIdentifier<T> path, T data) {
        final WriteTransaction writeTransaction = getDataBroker().newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.OPERATIONAL, path, data, true);
        writeTransaction.submit();
    }

    private List<Interface> constructInterfaces() {
        Interface interface1 = constructInterface("Interface1", 1001, "819200", Interface.OperStatus.Up,
                Interface.AdminStatus.Up, "00:11:11:11:11:11");

        Interface interface2 = constructInterface("Interface2", 2002, "819200", Interface.OperStatus.Up,
                Interface.AdminStatus.Up, "00:00:00:00:00:11");
        List<Interface> list = new ArrayList<>();
        list.add(interface1);
        list.add(interface2);
        return list;
    }

    private Interface constructInterface(String name, int ifIndex, String speed, Interface.OperStatus operStatus,
                                         Interface.AdminStatus adminStatus, String py) {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.setKey(new InterfaceKey(name));
        builder.setName(name);
        builder.setOperStatus(operStatus);
        builder.setSpeed(new Gauge64(new BigInteger(speed)));
        builder.setAdminStatus(adminStatus);
        builder.setIfIndex(new Integer(ifIndex));
        builder.setType(InterfaceType.class);
        builder.setPhysAddress(new PhysAddress(py));
        return builder.build();
    }

    private Node constructNode(String nodeId, String ip, int port, String deviceId, List<Interface> interfaces) {
        NodeBuilder builder = new NodeBuilder();
        builder.setKey(new NodeKey(nodeId));
        builder.setNodeId(nodeId);
        builder.setGrpcServerIp(new Ipv4Address(ip));
        builder.setGrpcServerPort(new PortNumber(port));
        builder.setGrpcServerDeviceId(new BigInteger(deviceId));
        builder.setInterface(interfaces);
        return builder.build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node constructInventoryData(
            String nodeId) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder builder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder();
        builder.setKey(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey(new
                org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeId)));
        builder.setId(new org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId(nodeId));
        builder.setNodeConnector(constructNodeConnector());
        return builder.build();
    }

    private List<NodeConnector> constructNodeConnector() {
        NodeConnector nodeConnector1 = constructConnector("1");
        NodeConnector nodeConnector2 = constructConnector("2");
        List<NodeConnector> list = new ArrayList<>();
        list.add(nodeConnector1);
        list.add(nodeConnector2);
        return list;
    }

    private NodeConnector constructConnector(String id) {
        NodeConnectorBuilder builder = new NodeConnectorBuilder();
        builder.setKey(new NodeConnectorKey(new NodeConnectorId(id)));
        builder.setId(new NodeConnectorId(id));
        return builder.build();
    }

    private WriteInventoryInput constructWriteInput() {
        WriteInventoryInputBuilder builder = new WriteInventoryInputBuilder();
        return builder.build();
    }

    private ReadInventoryInput constructReadInput() {
        ReadInventoryInputBuilder builder = new ReadInventoryInputBuilder();
        return builder.build();
    }

}
