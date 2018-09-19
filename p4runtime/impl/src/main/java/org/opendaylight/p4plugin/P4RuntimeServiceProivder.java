/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import com.google.common.util.concurrent.ListenableFuture;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.TextFormat;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class P4RuntimeServiceProivder implements P4pluginP4runtimeService {
    private static final Logger LOG = LoggerFactory.getLogger(P4RuntimeServiceProivder.class);
    private final DataBroker dataBroker;
    private final NotificationPublishService notificationPublishService;
    private DeviceManager deviceManager;
    private ListeningExecutorService executorService;

    public P4RuntimeServiceProivder(final DataBroker dataBroker, final NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
    }

    public void init() {
        NotificationPublisher.getInstance().setNotificationService(notificationPublishService);
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
        deviceManager = DeviceManager.getInstance();
        LOG.info("P4 plugin runtime service provider initiated.");
    }

    public void close() {
        executorService.shutdown();
        LOG.info("P4 plugin runtime service provider closed.");
    }

    private <T> RpcResult<T> rpcResultSuccess(T value) {
        return RpcResultBuilder.success(value).build();
    }

    private Callable<RpcResult<AddDeviceOutput>> addDev(AddDeviceInput input) {
        return () -> {
            String nodeId = input.getNid();
            String ip = input.getIp().getValue();
            Integer port = input.getPort().getValue();
            Long deviceId = input.getDid().longValue();
            String runtimeFile = input.getRuntimeFile();
            String configFile = input.getPipelineFile();
            deviceManager.addDevice(nodeId, deviceId, ip, port, runtimeFile, configFile);
            LOG.info("Add device = [{}-{}-{}:{}-{}-{}] success.", nodeId, deviceId, ip, port, runtimeFile, configFile);
            AddDeviceOutputBuilder outputBuilder = new AddDeviceOutputBuilder();
            return rpcResultSuccess(outputBuilder.build());
        };
    }

    private Callable<RpcResult<RemoveDeviceOutput>> removeDev(RemoveDeviceInput input) {
        return () -> {
            deviceManager.removeDevice(input.getNid());
            LOG.info("Remove device success, nodeId = {}.", input.getNid());
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<AddTableEntryOutput>> addEntry(AddTableEntryInput input) {
        return () -> {
            String nodeId = input.getNid();
            deviceManager.findDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .addTableEntry(input);
            LOG.info("Add entry to device success, nodeId = {}", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<ModifyTableEntryOutput>> modifyEntry(ModifyTableEntryInput input) {
        return () -> {
            String nodeId = input.getNid();
            deviceManager.findDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .modifyTableEntry(input);
            LOG.info("Modify entry to device success, nodeId = {}.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<DeleteTableEntryOutput>> deleteEntry(DeleteTableEntryInput input) {
        return () -> {
            String nodeId = input.getNid();
            deviceManager.findDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .deleteTableEntry(input);
            LOG.info("Delete entry from device success, nodeId = {}.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<ReadTableEntryOutput>> readEntry(ReadTableEntryInput input) {
        return () -> {
            String nodeId = input.getNid();
            ReadTableEntryOutputBuilder outputBuilder = new ReadTableEntryOutputBuilder();
            List<String> result = deviceManager.findDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .readTableEntry(input.getTableName());
            outputBuilder.setEntry(result);
            LOG.info("Read entry from device success, nodeId = {}.", nodeId);
            return RpcResultBuilder.success(outputBuilder.build()).build();
        };
    }

    private Callable<RpcResult<TransmitPacketOutput>> tranPacket(TransmitPacketInput input) {
        return () -> {
            String nodeId = input.getNid();
            Optional<Device> optional = deviceManager.findDevice(nodeId);
            optional.orElseThrow(IllegalArgumentException::new).transmitPacket(input.getPayload());
            LOG.info("Transmit packet to device success, nodeId = {}.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<SetPipelineConfigOutput>> setConfig(SetPipelineConfigInput input) {
        return () -> {
            String nodeId = input.getNid();
            Optional<Device> optional = deviceManager.findDevice(nodeId);
            optional.orElseThrow(IllegalArgumentException::new).setPipelineConfig();
            LOG.info("Set device pipeline config success, nodeId = {}.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<GetPipelineConfigOutput>> getConfig(GetPipelineConfigInput input) {
        return () -> {
            String nodeId = input.getNid();
            Optional<Device> optional = deviceManager.findDevice(nodeId);
            P4Info p4info = optional.orElseThrow(IllegalArgumentException::new)
                    .getPipelineConfig()
                    .getConfigs(0)
                    .getP4Info();
            String result = TextFormat.printToString(p4info);
            GetPipelineConfigOutputBuilder outputBuilder = new GetPipelineConfigOutputBuilder();
            outputBuilder.setP4Info(result);
            LOG.info("Get device pipeline config success, nodeId = {}.", nodeId);
            return rpcResultSuccess(outputBuilder.build());
        };
    }

    @Override
    public ListenableFuture<RpcResult<AddDeviceOutput>> addDevice(AddDeviceInput input) {
        return executorService.submit(addDev(input));
    }

    @Override
    public ListenableFuture<RpcResult<RemoveDeviceOutput>> removeDevice(RemoveDeviceInput input) {
        return executorService.submit(removeDev(input));
    }

    @Override
    public ListenableFuture<RpcResult<AddTableEntryOutput>> addTableEntry(AddTableEntryInput input) {
        return executorService.submit(addEntry(input));
    }

    @Override
    public ListenableFuture<RpcResult<ModifyTableEntryOutput>> modifyTableEntry(ModifyTableEntryInput input) {
        return executorService.submit(modifyEntry(input));
    }

    @Override
    public ListenableFuture<RpcResult<DeleteTableEntryOutput>> deleteTableEntry(DeleteTableEntryInput input) {
        return executorService.submit(deleteEntry(input));
    }

    @Override
    public ListenableFuture<RpcResult<ReadTableEntryOutput>> readTableEntry(ReadTableEntryInput input) {
        return executorService.submit(readEntry(input));
    }

    @Override
    public ListenableFuture<RpcResult<TransmitPacketOutput>> transmitPacket(TransmitPacketInput input) {
        return executorService.submit(tranPacket(input));
    }

    @Override
    public ListenableFuture<RpcResult<SetPipelineConfigOutput>> setPipelineConfig(SetPipelineConfigInput input) {
        return executorService.submit(setConfig(input));
    }

    @Override
    public ListenableFuture<RpcResult<GetPipelineConfigOutput>> getPipelineConfig(GetPipelineConfigInput input) {
        return executorService.submit(getConfig(input));
    }
}
