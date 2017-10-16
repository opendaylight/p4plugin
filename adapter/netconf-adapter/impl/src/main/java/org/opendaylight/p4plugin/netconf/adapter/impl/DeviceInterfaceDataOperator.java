/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.AddNodeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.AddNodeInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.AddNodeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.P4pluginCoreDeviceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.SetPipelineConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.SetPipelineConfigInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.device.rev170808.SetPipelineConfigOutput;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.NodeGrpcInfo;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.NodeInterfacesState;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.Node;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceInterfaceDataOperator {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceInterfaceDataOperator.class);

    private final DataProcess dataProcess;
    private final RpcProviderRegistry rpcProviderRegistry;

    private static final InstanceIdentifier<InterfacesState> IETF_INTERFACE_IID = InstanceIdentifier
            .create(InterfacesState.class);

    private static final InstanceIdentifier<NodeGrpcInfo> NODE_GRPC_INFO_IID = InstanceIdentifier
            .create(NodeGrpcInfo.class);

    private static final InstanceIdentifier<NodeInterfacesState> NODE_INTERFACE_STATE_IID = InstanceIdentifier
            .create(NodeInterfacesState.class);


    public DeviceInterfaceDataOperator(DataProcess dataProcess, RpcProviderRegistry rpcProviderRegistry) {
        this.dataProcess = dataProcess;
        this.rpcProviderRegistry = rpcProviderRegistry;
    }


    public InterfacesState readInterfacesFromDevice(String nodeId) {
        LOG.info("Start read data from device");
        return dataProcess.readInterfaces(nodeId, IETF_INTERFACE_IID);
    }

    public NodeGrpcInfo readGrpcFromDevice(String nodeId) {
        LOG.info("Start read grpc info from device");
        return dataProcess.readGrpcInfo(nodeId, NODE_GRPC_INFO_IID);
    }

    public void sendP4DeviceInfo(String nodeId, NodeGrpcInfo grpcInfo) {
        try {
            LOG.info("Call rpc addNode");
            Future<RpcResult<AddNodeOutput>> addNodeRpcResult = rpcProviderRegistry
                    .getRpcService(P4pluginCoreDeviceService.class).addNode(constructRpcAddNodeInput(nodeId, grpcInfo));
            if (addNodeRpcResult.get().isSuccessful()) {
                LOG.info("Rpc addNode called success, node: {}", nodeId);
                if (addNodeRpcResult.get().getResult().isResult()) {
                    LOG.info("Add node {} success, call rpc setPipelineConfig", nodeId);
                    Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfigRpcResult = rpcProviderRegistry
                            .getRpcService(P4pluginCoreDeviceService.class)
                            .setPipelineConfig(constructRpcSetPipelineConfigInput(nodeId));
                    if (setPipelineConfigRpcResult.get().isSuccessful()) {
                        LOG.info("Rpc setPipelineConfig called success, node: {}", nodeId);
                        if (setPipelineConfigRpcResult.get().getResult().isResult()) {
                            LOG.info("Set node {} forwarding pipeline config success", nodeId);
                        }
                    } else {
                        LOG.info("Rpc setPipelineConfig called failed, node: {}", nodeId);
                    }
                }
            } else {
                LOG.info("Rpc addNode called failed, node: {}", nodeId);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Rpc interrupted by {}", e);
        }
    }

    private AddNodeInput constructRpcAddNodeInput(String nodeId, NodeGrpcInfo grpcInfo) {
        AddNodeInputBuilder builder = new AddNodeInputBuilder();
        builder.setNodeId(nodeId);
        builder.setIp(grpcInfo.getIp());
        builder.setPort(grpcInfo.getPort());
        builder.setDeviceId(grpcInfo.getId());
        builder.setRuntimeFile("/home/opendaylight/odl/p4src/switch.proto.txt");
        builder.setConfigFile(null);
        return builder.build();
    }

    private SetPipelineConfigInput constructRpcSetPipelineConfigInput(String nodeId) {
        SetPipelineConfigInputBuilder builder = new SetPipelineConfigInputBuilder();
        builder.setNodeId(nodeId);
        return builder.build();
    }

    public void writeInterfacesToControllerDataStore(String nodeId, InterfacesState interfacesData,
                                                     NodeGrpcInfo grpcInfo) {
        LOG.info("Start write data to controller data store");
        InstanceIdentifier path = getNodePath(nodeId);
        dataProcess.writeToDataStore(nodeId, interfacesData, grpcInfo, path);

    }

    public NodeInterfacesState readInterfacesFromControllerDataStore() {
        LOG.info("Read data from controller data store");
        return dataProcess.readFromDataStore(NODE_INTERFACE_STATE_IID);
    }

    private InstanceIdentifier<Node> getNodePath(String nodeId) {
        return InstanceIdentifier.create(NodeInterfacesState.class).child(Node.class, new NodeKey(nodeId));
    }
}
