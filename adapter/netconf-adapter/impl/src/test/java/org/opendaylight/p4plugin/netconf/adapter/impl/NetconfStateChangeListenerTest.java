/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.SettableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
//import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.node.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.p4plugin.yang.p4device.grpc.rev170908.GrpcInfo;
import org.opendaylight.yang.gen.v1.urn.p4plugin.yang.p4device.grpc.rev170908.GrpcInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

@RunWith(MockitoJUnitRunner.class)
public class NetconfStateChangeListenerTest extends AbstractDataBrokerTest {

    @Mock
    private MountPoint mountPoint;
    @Mock
    private MountPointService mountPointService;
    @Mock
    private RpcProviderRegistry rpcProviderRegistry;
    @Mock
    DataTreeModification<Node> dataTreeModification;
    @Mock
    DataObjectModification<Node> dataObjectModification;
    Collection<DataTreeModification<Node>> modifications;

    private DataBroker dataBroker;
    private Optional<MountPoint> optionalMountPointObject;
    //private DataBroker nodeDataBroker;
    private Optional<DataBroker> optionalDataBrokerObject;
//    @Mock
//    private Optional<GrpcInfo> optionalGrpcInfo;

    private DataProcess dataProcess;
    private DeviceInterfaceDataOperator deviceInterfaceDataOperator;
    private NetconfStateChangeListener netconfStateChangeListener;
//    @Mock
//    private ReadTransaction readTransaction;

    private P4pluginRuntimeNodeServiceMock p4pluginRuntimeDeviceServiceMock;

    private static final NodeId CONTROLLER_CONFIG_ID = new NodeId("controller-config");
    private static final NodeId NODE_ID = new NodeId("device0");
    private static final InstanceIdentifier<GrpcInfo> GRPC_INFO_IID = InstanceIdentifier.create(GrpcInfo.class);
    private static final InstanceIdentifier<InterfacesState> IETF_INTERFACE_IID = InstanceIdentifier
            .create(InterfacesState.class);

    private void buildMock() {
        dataBroker = getDataBroker();
        modifications = Collections.singletonList(dataTreeModification);
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);

        optionalMountPointObject = mock(Optional.class);
        optionalDataBrokerObject = mock(Optional.class);
        //optionalGrpcInfo = mock(Optional.class);

        when(mountPointService.getMountPoint(any(InstanceIdentifier.class))).thenReturn(optionalMountPointObject);
        when(optionalMountPointObject.isPresent()).thenReturn(true);
        when(optionalMountPointObject.get()).thenReturn(mountPoint);

        when(mountPoint.getService(DataBroker.class)).thenReturn(optionalDataBrokerObject);
        when(optionalDataBrokerObject.isPresent()).thenReturn(true);
        when(optionalDataBrokerObject.get()).thenReturn(dataBroker);

        p4pluginRuntimeDeviceServiceMock = new P4pluginRuntimeNodeServiceMock();
        when(rpcProviderRegistry.getRpcService(P4pluginNodeService.class))
                .thenReturn(p4pluginRuntimeDeviceServiceMock);

        dataProcess = new DataProcess(dataBroker, mountPointService);
        deviceInterfaceDataOperator = new DeviceInterfaceDataOperator(dataProcess, rpcProviderRegistry);
        netconfStateChangeListener = new NetconfStateChangeListener(deviceInterfaceDataOperator);
    }

    private Node buildNodeControllerConfig() {
        return new NodeBuilder().setNodeId(new NodeId(CONTROLLER_CONFIG_ID)).addAugmentation(NetconfNode.class,
                new NetconfNodeBuilder().setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.Connected)
                        .build()).build();
    }

    private Node buildNodeBefore() {
        return new NodeBuilder().setNodeId(NODE_ID).addAugmentation(NetconfNode.class, new NetconfNodeBuilder()
                .setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.Connecting).build()).build();
    }

    private Node buildNodeAfter() {
        return new NodeBuilder().setNodeId(NODE_ID).addAugmentation(NetconfNode.class, new NetconfNodeBuilder()
                .setConnectionStatus(NetconfNodeConnectionStatus.ConnectionStatus.Connected).build()).build();
    }


    @Test
    public void testOnDataTreeChangedAddedNode() throws Exception {
        buildMock();

        netconfStateChangeListener.onDataTreeChanged(modifications);

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeControllerConfig());
        netconfStateChangeListener.onDataTreeChanged(modifications);

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);

        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeAddedMap().containsKey(NODE_ID));
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeOne() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        netconfStateChangeListener.onDataTreeChanged(modifications);

        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeTwo() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

//        when(readTransaction.read(LogicalDatastoreType.OPERATIONAL, GRPC_INFO_IID).checkedGet())
//                .thenReturn(optionalGrpcInfo);
//        when(optionalGrpcInfo.isPresent()).thenReturn(true);
//        when(optionalGrpcInfo.get()).thenReturn(constructGrpcInfo(NODE_ID, "127.0.0.1", 50051, "1"));
        writeTestDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "127.0.0.1", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);

        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        assertTestModifiedNodeTwoGrpcInfo(p4pluginRuntimeDeviceServiceMock.getAddNodeInputList());
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeThree() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        writeTestDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);

        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        assertTestModifiedNodeThreeGrpcInfo(p4pluginRuntimeDeviceServiceMock.getAddNodeInputList());
        assertTestModifiedNodeThreeSPCgInput(p4pluginRuntimeDeviceServiceMock.getSetPipelineConfigInputList());
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeFour() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        writeTestDataToDataStore(dataBroker, IETF_INTERFACE_IID, constructInterfaceState(Interface.OperStatus.Up,
                Interface.AdminStatus.Up));
        writeTestDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        assertTestModifiedNodeThreeGrpcInfo(p4pluginRuntimeDeviceServiceMock.getAddNodeInputList());
        assertTestModifiedNodeThreeSPCgInput(p4pluginRuntimeDeviceServiceMock.getSetPipelineConfigInputList());
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeFive() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        writeTestDataToDataStore(dataBroker, IETF_INTERFACE_IID, constructInterfaceState(
                Interface.OperStatus.NotPresent, Interface.AdminStatus.Down));
        writeTestDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        assertTestModifiedNodeThreeGrpcInfo(p4pluginRuntimeDeviceServiceMock.getAddNodeInputList());
        assertTestModifiedNodeThreeSPCgInput(p4pluginRuntimeDeviceServiceMock.getSetPipelineConfigInputList());
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeSix() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        writeTestDataToDataStore(dataBroker, IETF_INTERFACE_IID, constructInterfaceState(
                Interface.OperStatus.Down, Interface.AdminStatus.Down));
        writeTestDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        assertTestModifiedNodeThreeGrpcInfo(p4pluginRuntimeDeviceServiceMock.getAddNodeInputList());
        assertTestModifiedNodeThreeSPCgInput(p4pluginRuntimeDeviceServiceMock.getSetPipelineConfigInputList());
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeSeven() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        writeTestDataToDataStore(dataBroker, IETF_INTERFACE_IID, constructInterfaceState(
                Interface.OperStatus.Testing, Interface.AdminStatus.Down));
        writeTestDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        //(p4pluginRuntimeDeviceServiceMock.getAddNodeInputList());
        assertTestModifiedNodeThreeSPCgInput(p4pluginRuntimeDeviceServiceMock.getSetPipelineConfigInputList());
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeEight() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        writeTestDataToDataStore(dataBroker, IETF_INTERFACE_IID, constructInterfaceState(
                Interface.OperStatus.Dormant, Interface.AdminStatus.Testing));
        writeTestDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        assertTestModifiedNodeThreeGrpcInfo(p4pluginRuntimeDeviceServiceMock.getAddNodeInputList());
        assertTestModifiedNodeThreeSPCgInput(p4pluginRuntimeDeviceServiceMock.getSetPipelineConfigInputList());
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeNine() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        writeTestDataToDataStore(dataBroker, IETF_INTERFACE_IID, constructInterfaceState(
                Interface.OperStatus.Unknown, Interface.AdminStatus.Up));
        writeTestDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        assertTestModifiedNodeThreeGrpcInfo(p4pluginRuntimeDeviceServiceMock.getAddNodeInputList());
        assertTestModifiedNodeThreeSPCgInput(p4pluginRuntimeDeviceServiceMock.getSetPipelineConfigInputList());
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeTen() throws Exception {
        buildMock();

        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        writeTestDataToDataStore(dataBroker, IETF_INTERFACE_IID, constructInterfaceState(
                Interface.OperStatus.LowerLayerDown, Interface.AdminStatus.Down));
        writeTestDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        assertTestModifiedNodeThreeGrpcInfo(p4pluginRuntimeDeviceServiceMock.getAddNodeInputList());
        assertTestModifiedNodeThreeSPCgInput(p4pluginRuntimeDeviceServiceMock.getSetPipelineConfigInputList());
    }

    @Test
    public void testOnDataTreeChangedModifiedNodeEleven() throws Exception {
        modifications = Collections.singletonList(dataTreeModification);
        when(dataTreeModification.getRootNode()).thenReturn(dataObjectModification);
        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
                .ModificationType.SUBTREE_MODIFIED);

        dataProcess = new DataProcess(dataBroker, null);
        deviceInterfaceDataOperator = new DeviceInterfaceDataOperator(dataProcess, rpcProviderRegistry);
        netconfStateChangeListener = new NetconfStateChangeListener(deviceInterfaceDataOperator);
        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
    }

    @Test
    public void testOnDataTreeChangedDeletedNode() throws Exception {
        buildMock();

        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);

        netconfStateChangeListener.onDataTreeChanged(modifications);
        assertTrue(netconfStateChangeListener.getNodeDeletedMap().containsKey(NODE_ID));
    }

    private GrpcInfo constructGrpcInfo(NodeId nodeId, String ip, int port, String deviceId) {
        GrpcInfoBuilder builder = new GrpcInfoBuilder();
        builder.setNodeId(nodeId.getValue());
        builder.setGrpcIp(new Ipv4Address(ip));
        builder.setGrpcPort(new PortNumber(new Integer(port)));
        builder.setDeviceId(new BigInteger(deviceId));
        return builder.build();
    }

    private InterfacesState constructInterfaceState(Interface.OperStatus operStatus,
                                                    Interface.AdminStatus adminStatus) {
        Interface interface1 = constructInterface("Interface1", 1001, "819200", operStatus, adminStatus,
                "00:11:11:11:11:11");
        Interface interface2 = constructInterface("Interface2", 2002, "819200", operStatus, adminStatus,
                "00:00:00:00:00:11");
        List<Interface> list = new ArrayList<>();
        list.add(interface1);
        list.add(interface2);
        InterfacesStateBuilder builder = new InterfacesStateBuilder();
        builder.setInterface(list);
        return builder.build();
    }

    private Interface constructInterface(String name, int ifIndex, String speed, Interface.OperStatus operStatus,
                                         Interface.AdminStatus adminStatus, String py) {
        InterfaceBuilder builder = new InterfaceBuilder();
        builder.setKey(new InterfaceKey(name));
        builder.setName(name);
        builder.setType(InterfaceType.class);
        builder.setAdminStatus(adminStatus);
        builder.setOperStatus(operStatus);
        builder.setIfIndex(ifIndex);
        builder.setPhysAddress(new PhysAddress(py));
        builder.setSpeed(new Gauge64(new BigInteger(speed)));
        return builder.build();
    }

    private <T extends DataObject> void writeTestDataToDataStore(DataBroker dataBroker,
                                                             InstanceIdentifier<T> path, T data) {
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, path, data, true);
        writeTransaction.submit();
    }

    private static class P4pluginRuntimeNodeServiceMock implements P4pluginNodeService {

        private List<AddNodeInput> addDeviceInputList = new ArrayList<>();
        private List<SetPipelineConfigInput> setPipelineConfigInputList = new ArrayList<>();

        @Override
        public Future<RpcResult<Void>> addNode(AddNodeInput input) {
            addDeviceInputList.add(input);
//            AddDeviceOutputBuilder builder = new AddDeviceOutputBuilder();
//            if (input.getIp().getValue().equals("127.0.0.1")) {
//                builder.setResult(false);
//            } else {
//                builder.setResult(true);
//            }
            RpcResultBuilder<Void> rpcResultBuilder = RpcResultBuilder.success();
            //rpcResultBuilder.withResult(builder.build());
            SettableFuture<RpcResult<Void>> future = SettableFuture.create();
            future.set(rpcResultBuilder.build());
            return future;
        }

        private List<AddNodeInput> getAddNodeInputList() {
            return addDeviceInputList;
        }

        @Override
        public Future<RpcResult<Void>> setPipelineConfig(SetPipelineConfigInput input) {
            setPipelineConfigInputList.add(input);
//            SetPipelineConfigOutputBuilder builder = new SetPipelineConfigOutputBuilder();
//            builder.setResult(true);
            RpcResultBuilder<Void> rpcResultBuilder = RpcResultBuilder.success();
            //rpcResultBuilder.withResult(builder.build());
            SettableFuture<RpcResult<Void>> future = SettableFuture.create();
            future.set(rpcResultBuilder.build());
            return future;
        }

        private List<SetPipelineConfigInput> getSetPipelineConfigInputList() {
            return setPipelineConfigInputList;
        }

        @Override
        public Future<RpcResult<GetPipelineConfigOutput>> getPipelineConfig(GetPipelineConfigInput input) {
            return null;
        }

        @Override
        public Future<RpcResult<GetNodeStateOutput>> getNodeState(GetNodeStateInput input) {
            return null;
        }

        @Override
        public Future<RpcResult<Void>> openStreamChannel(OpenStreamChannelInput input) {
            return null;
        }

        @Override
        public Future<RpcResult<Void>> removeNode(RemoveNodeInput input) {
            return null;
        }

        @Override
        public Future<RpcResult<QueryNodesOutput>> queryNodes() {
            return null;
        }
    }

    private void assertTestModifiedNodeTwoGrpcInfo(List<AddNodeInput> list) {
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0).getNid(), NODE_ID.getValue());
        Assert.assertEquals(list.get(0).getIp(), new Ipv4Address("127.0.0.1"));
        Assert.assertEquals(list.get(0).getPort(), new PortNumber(50051));
        Assert.assertEquals(list.get(0).getConfigFilePath(), null);
    }

    private void assertTestModifiedNodeThreeGrpcInfo(List<AddNodeInput> list) {
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0).getNid(), NODE_ID.getValue());
        Assert.assertEquals(list.get(0).getIp(), new Ipv4Address("10.42.89.15"));
        Assert.assertEquals(list.get(0).getPort(), new PortNumber(50051));
        Assert.assertEquals(list.get(0).getConfigFilePath(), null);
    }

    private void assertTestModifiedNodeThreeSPCgInput(List<SetPipelineConfigInput> list) {
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0).getNid(), NODE_ID.getValue());
    }

}
