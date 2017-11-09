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
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.AddNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.AddNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.AddNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.GetPipelineConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.GetPipelineConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.P4pluginCoreDeviceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.QueryNodesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.RemoveNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.RemoveNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.SetPipelineConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.SetPipelineConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.SetPipelineConfigOutputBuilder;
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
public class NetconfStateChangeListenerTest extends AbstractConcurrentDataBrokerTest {

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

    private P4pluginCoreDeviceServiceMock p4pluginCoreDeviceServiceMock;

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

        p4pluginCoreDeviceServiceMock = new P4pluginCoreDeviceServiceMock();
        when(rpcProviderRegistry.getRpcService(P4pluginCoreDeviceService.class))
                .thenReturn(p4pluginCoreDeviceServiceMock);

        dataProcess = new DataProcess(getDataBroker(), mountPointService);
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

        writeDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "127.0.0.1", 50051, "1"));
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
        writeDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "127.0.0.1", 50051, "1"));
        netconfStateChangeListener.onDataTreeChanged(modifications);

        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
        assertTestModifiedNodeTwoGrpcInfo(p4pluginCoreDeviceServiceMock.getAddNodeInputList());
    }

//    @Test
//    public void testOnDataTreeChangedModifiedNodeThree() throws Exception {
//        buildMock();
//
//        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
//        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
//        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
//                .ModificationType.SUBTREE_MODIFIED);
//
//        writeDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
//        netconfStateChangeListener.onDataTreeChanged(modifications);
//
//        assertTrue(netconfStateChangeListener.getNodeModifiedMap().containsKey(NODE_ID));
//        assertTestModifiedNodeThreeGrpcInfo(p4pluginCoreDeviceServiceMock.getAddNodeInputList());
//        assertTestModifiedNodeThreeSPCgInput(p4pluginCoreDeviceServiceMock.getSetPipelineConfigInputList());
//    }

//    @Test
//    public void testOnDataTreeChangedModifiedNodeFour() throws Exception {
//        buildMock();
//
//        when(dataObjectModification.getDataAfter()).thenReturn(buildNodeAfter());
//        when(dataObjectModification.getDataBefore()).thenReturn(buildNodeBefore());
//        when(dataObjectModification.getModificationType()).thenReturn(DataObjectModification
//                .ModificationType.SUBTREE_MODIFIED);
//
//        writeDataToDataStore(dataBroker, IETF_INTERFACE_IID, constructInterfaceInfo());
//        writeDataToDataStore(dataBroker, GRPC_INFO_IID, constructGrpcInfo(NODE_ID, "10.42.89.15", 50051, "1"));
//        netconfStateChangeListener.onDataTreeChanged(modifications);
//    }

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

//    private InterfacesState constructInterfaceInfo() {
//        return null;
//    }

    private <T extends DataObject> void writeDataToDataStore(DataBroker dataBroker,
                                                             InstanceIdentifier<T> path, T data) {
        final WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.merge(LogicalDatastoreType.OPERATIONAL, path, data, true);
        writeTransaction.submit();
    }

    private static class P4pluginCoreDeviceServiceMock implements P4pluginCoreDeviceService {

        private List<AddNodeInput> addNodeInputList = new ArrayList<>();
        private List<SetPipelineConfigInput> setPipelineConfigInputList = new ArrayList<>();

        @Override
        public Future<RpcResult<AddNodeOutput>> addNode(AddNodeInput input) {
            addNodeInputList.add(input);
            AddNodeOutputBuilder builder = new AddNodeOutputBuilder();
            if (input.getIp().getValue().equals("127.0.0.1")) {
                builder.setResult(false);
            } else {
                builder.setResult(true);
            }
            RpcResultBuilder<AddNodeOutput> rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(builder.build());
            SettableFuture<RpcResult<AddNodeOutput>> future = SettableFuture.create();
            future.set(rpcResultBuilder.build());
            return future;
        }

        private List<AddNodeInput> getAddNodeInputList() {
            return addNodeInputList;
        }

        @Override
        public Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfig(SetPipelineConfigInput input) {
            setPipelineConfigInputList.add(input);
            SetPipelineConfigOutputBuilder builder = new SetPipelineConfigOutputBuilder();
            builder.setResult(true);
            RpcResultBuilder<SetPipelineConfigOutput> rpcResultBuilder = RpcResultBuilder.success();
            rpcResultBuilder.withResult(builder.build());
            SettableFuture<RpcResult<SetPipelineConfigOutput>> future = SettableFuture.create();
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
        public Future<RpcResult<RemoveNodeOutput>> removeNode(RemoveNodeInput input) {
            return null;
        }

        @Override
        public Future<RpcResult<QueryNodesOutput>> queryNodes() {
            return null;
        }
    }

    private void assertTestModifiedNodeTwoGrpcInfo(List<AddNodeInput> list) {
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0).getNodeId(), NODE_ID.getValue());
        Assert.assertEquals(list.get(0).getIp(), new Ipv4Address("127.0.0.1"));
        Assert.assertEquals(list.get(0).getPort(), new PortNumber(50051));
        Assert.assertEquals(list.get(0).getConfigFile(), null);
    }

    private void assertTestModifiedNodeThreeGrpcInfo(List<AddNodeInput> list) {
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0).getNodeId(), NODE_ID.getValue());
        Assert.assertEquals(list.get(0).getIp(), new Ipv4Address("10.42.89.15"));
        Assert.assertEquals(list.get(0).getPort(), new PortNumber(50051));
        Assert.assertEquals(list.get(0).getConfigFile(), null);
    }

    private void assertTestModifiedNodeThreeSPCgInput(List<SetPipelineConfigInput> list) {
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals(list.get(0).getNodeId(), NODE_ID.getValue());
    }

}
