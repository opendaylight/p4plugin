/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl;

import com.google.protobuf.TextFormat;
import org.opendaylight.p4plugin.p4runtime.proto.GetForwardingPipelineConfigResponse;
import org.opendaylight.p4plugin.runtime.impl.device.Device;
import org.opendaylight.p4plugin.runtime.impl.device.DeviceManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.node.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.types.rev170808.ChannelState;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

public class NodeServiceImpl implements P4pluginNodeService {
    private static final Logger LOG = LoggerFactory.getLogger(NodeServiceImpl.class);
    private DeviceManager deviceManager;
    private ExecutorService executorService;

    public void init() {
        executorService = Executors.newCachedThreadPool(new NodeServiceThreadFactory());
        deviceManager = DeviceManager.getInstance();
    }

    public void close() {
        executorService.shutdown();
    }

    private <T> RpcResult<T> rpcResultSuccess(T value) {
        return RpcResultBuilder.success(value).build();
    }

    private Callable<RpcResult<Void>> addDev(AddNodeInput input) {
        return () -> {
            String nodeId = input.getNid();
            String ip = input.getIp().getValue();
            Integer port = input.getPort().getValue();
            Long deviceId = input.getDid().longValue();
            String runtimeFile = input.getRuntimeFilePath();
            String configFile = input.getConfigFilePath();
            deviceManager.addDevice(nodeId, deviceId, ip, port, configFile, runtimeFile);
            LOG.info("Add device = {}:{}-{}-{}-{}-{} RPC success.", nodeId, deviceId, ip, port, configFile, runtimeFile);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> removeDev(RemoveNodeInput input) {
        return () -> {
            deviceManager.removeDevice(input.getNid());
            LOG.info("Remove device = {} RPC success.", input.getNid());
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> setConfig(SetPipelineConfigInput input) {
        return () -> {
            String nodeId = input.getNid();
            Optional<Device> optional = deviceManager.findDevice(nodeId);
            optional.orElseThrow(()->new IllegalArgumentException("Device isn't exist.")).setPipelineConfig();
            LOG.info("Set device = {} pipeline config RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<GetPipelineConfigOutput>> getConfig(GetPipelineConfigInput input) {
        return () -> {
            String nodeId = input.getNid();
            Optional<Device> optional = deviceManager.findDevice(nodeId);
            GetForwardingPipelineConfigResponse response =
                    optional.orElseThrow(()->new IllegalArgumentException("Device isn't exist.")).getPipelineConfig();
            String runtimeInfo = TextFormat.printToString(response.getConfigs(0).getP4Info());
            byte[] pipelineConfig = response.getConfigs(0).getP4DeviceConfig().toByteArray();
            GetPipelineConfigOutputBuilder outputBuilder = new GetPipelineConfigOutputBuilder();
            outputBuilder.setPipelineConfig(pipelineConfig);
            outputBuilder.setRuntimeInfo(runtimeInfo);
            LOG.info("Get device = {} pipeline config RPC success.", nodeId);
            return rpcResultSuccess(outputBuilder.build());
        };
    }

    private Callable<RpcResult<Void>> openChannel(OpenStreamChannelInput input) {
        return () -> {
            String nodeId = input.getNid();
            Optional<Device> optional = deviceManager.findDevice(nodeId);
            optional.orElseThrow(() -> new IllegalArgumentException("Device isn't exist.")).openStreamChannel();
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<QueryNodesOutput>> queryDev() {
        return () -> {
            List<String> keys = new ArrayList<>();
            deviceManager.queryDevices().forEach((key, dev) -> keys.add(key));
            QueryNodesOutputBuilder outputBuilder = new QueryNodesOutputBuilder();
            outputBuilder.setNode(keys);
            return rpcResultSuccess(outputBuilder.build());
        };
    }

    private Callable<RpcResult<GetNodeStateOutput>> getState(GetNodeStateInput input) {
        return () -> {
            String nodeId = input.getNid();
            Optional<Device> optional = deviceManager.findDevice(nodeId);
            GetNodeStateOutputBuilder builder = new GetNodeStateOutputBuilder();
            if (optional.isPresent()) {
                Device device = optional.get();
                builder.setChannelState(ChannelState.valueOf(device.getChannelState().toString()));
                builder.setConfigState(device.getConfigState());
            }
            return rpcResultSuccess(builder.build());
        };
    }

    @Override
    public Future<RpcResult<Void>> addNode(AddNodeInput input) {
        return executorService.submit(addDev(input));
    }

    @Override
    public Future<RpcResult<Void>> removeNode(RemoveNodeInput input) {
        return executorService.submit(removeDev(input));
    }

    @Override
    public Future<RpcResult<Void>> setPipelineConfig(SetPipelineConfigInput input) {
        return executorService.submit(setConfig(input));
    }

    @Override
    public Future<RpcResult<GetPipelineConfigOutput>> getPipelineConfig(GetPipelineConfigInput input) {
        return executorService.submit(getConfig(input));
    }

    @Override
    public Future<RpcResult<Void>> openStreamChannel(OpenStreamChannelInput input) {
        return executorService.submit(openChannel(input));
    }

    @Override
    public Future<RpcResult<QueryNodesOutput>> queryNodes() {
        return executorService.submit(queryDev());
    }

    @Override
    public Future<RpcResult<GetNodeStateOutput>> getNodeState(GetNodeStateInput input) {
        return executorService.submit(getState(input));
    }

    private static class NodeServiceThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "NodeServiceThread");
        }
    }
}
