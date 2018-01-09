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
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.NodeInterfacesState;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.Node;
import org.opendaylight.yang.gen.v1.urn.p4plugin.netconf.adapter.rev170908.node.interfaces.state.NodeKey;
import org.opendaylight.yang.gen.v1.urn.p4plugin.yang.p4device.grpc.rev170908.GrpcInfo;
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

    private static final InstanceIdentifier<GrpcInfo> GRPC_INFO_IID = InstanceIdentifier
            .create(GrpcInfo.class);

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

    public GrpcInfo readGrpcFromDevice(String nodeId) {
        LOG.info("Start read grpc info from device");
        return dataProcess.readGrpcInfo(nodeId, GRPC_INFO_IID);
    }

    public void sendP4DeviceInfo(String nodeId, GrpcInfo grpcInfo) {
        try {
            Future<RpcResult<Void>> addDeviceRpcResult = rpcProviderRegistry
                    .getRpcService(P4pluginDeviceService.class)
                    .addDevice(constructRpcAddNodeInput(grpcInfo));
            if (addDeviceRpcResult.get().isSuccessful()) {
                Future<RpcResult<ConnectToDeviceOutput>> connectToDeviceRpcResult = rpcProviderRegistry
                        .getRpcService(P4pluginDeviceService.class)
                        .connectToDevice(constructRpcConnectToDeviceInput(nodeId));
                if (connectToDeviceRpcResult.get().getResult().isConnectStatus()) {
                    Future<RpcResult<Void>> setPipelineConfigRpcResult = rpcProviderRegistry
                            .getRpcService(P4pluginDeviceService.class)
                            .setPipelineConfig(constructRpcSetPipelineConfigInput(nodeId));
                    if (setPipelineConfigRpcResult.get().isSuccessful()) {
                        LOG.info("Rpc setPipelineConfig call success, node: {}", nodeId);
                    } else {
                        LOG.info("Rpc setPipelineConfig call failed, node: {}", nodeId);
                    }
                    LOG.info("Rpc connectToDevice call success, node: {}", nodeId);
                } else {
                    LOG.info("Rpc connectToDevice call failed, node: {}", nodeId);
                }
                LOG.info("Rpc addDevice call success, node: {}", nodeId);
            } else {
                LOG.info("Rpc addDevice call failed, node: {}", nodeId);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Rpc interrupted by {}", e);
        }
    }

    private ConnectToDeviceInput constructRpcConnectToDeviceInput(String nodeId) {
        ConnectToDeviceInputBuilder builder = new ConnectToDeviceInputBuilder();
        builder.setNid(nodeId);
        return builder.build();
    }

    private AddDeviceInput constructRpcAddNodeInput(GrpcInfo grpcInfo) {
        AddDeviceInputBuilder builder = new AddDeviceInputBuilder();
        builder.setNid(grpcInfo.getNodeId());
        builder.setIp(grpcInfo.getGrpcIp());
        builder.setPort(grpcInfo.getGrpcPort());
        builder.setDid(grpcInfo.getDeviceId());
        builder.setRuntimeFilePath("/home/opendaylight/odl/p4src/switch.proto.txt");
        builder.setConfigFilePath(null);
        return builder.build();
    }

    private SetPipelineConfigInput constructRpcSetPipelineConfigInput(String nodeId) {
        SetPipelineConfigInputBuilder builder = new SetPipelineConfigInputBuilder();
        builder.setNid(nodeId);
        return builder.build();
    }

    public void writeInterfacesToControllerDataStore(String nodeId, InterfacesState interfacesData,
                                                     GrpcInfo grpcInfo) {
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
