/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.appcommon;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public abstract class P4Switch {
    private static final Logger LOG = LoggerFactory.getLogger(P4Switch.class);
    private final String gRPCServerIp;
    private final Integer gRPCServerPort;
    private final Long deviceId;
    private final String nodeId;
    private final String configFile;
    private final String runtimeFile;
    private final P4pluginP4runtimeService runtimeService;
    private Status status = Status.INITIALIZE;

    protected P4Switch(String gRPCServerIp,
                    Integer gRPCServerPort,
                    Long deviceId,
                    String nodeId,
                    String configFile,
                    String runtimeFile,
                    P4pluginP4runtimeService runtimeService) {
        this.gRPCServerIp = gRPCServerIp;
        this.gRPCServerPort = gRPCServerPort;
        this.deviceId = deviceId;
        this.nodeId = nodeId;
        this.configFile = configFile;
        this.runtimeFile = runtimeFile;
        this.runtimeService = runtimeService;
    }

    public void openStreamChannel() {
        OpenStreamChannelInputBuilder inputBuilder = new OpenStreamChannelInputBuilder();
        inputBuilder.setNid(nodeId);
        boolean result;

        try {
            ListenableFuture<RpcResult<OpenStreamChannelOutput>> output = runtimeService.openStreamChannel(inputBuilder.build());
            result = output.get().isSuccessful();
            LOG.info("Open stream channel {}.", result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Open stream channel exception, message = {}.", e.getMessage());
        }
        status = result ? Status.OPEN_STREAM_CHANNEL : Status.ERROR;
    }

    public void setPipelineConfig() {
        if (status != Status.OPEN_STREAM_CHANNEL) return;
        SetPipelineConfigInputBuilder inputBuilder = new SetPipelineConfigInputBuilder();
        inputBuilder.setNid(nodeId);
        boolean result;

        try {
            ListenableFuture<RpcResult<SetPipelineConfigOutput>> output = runtimeService.setPipelineConfig(inputBuilder.build());
            result = output.get().isSuccessful();
            LOG.info("Set pipeline config {}.", result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Set pipeline config exception, message = {}.", e.getMessage());
        }
        status = result ? Status.SET_PIPELINE : Status.ERROR;
    }

    public void addTableEntry(AddTableEntryInput entry) {
        if (status != Status.SET_PIPELINE) return;
        try {
            ListenableFuture<RpcResult<AddTableEntryOutput>> output = runtimeService.addTableEntry(entry);
            boolean result = output.get().isSuccessful();
            LOG.info("Add entry to {} {}.", nodeId, result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            LOG.error("Add entry to {} exception, message = {}.", nodeId, e.getMessage());
        }
    }

    public void sendPacketOut(byte[] packet) {
        if (status != Status.SET_PIPELINE) return;
        TransmitPacketInputBuilder inputBuilder = new TransmitPacketInputBuilder();
        inputBuilder.setNid(nodeId);
        inputBuilder.setPayload(packet);

        try {
            ListenableFuture<RpcResult<TransmitPacketOutput>> output = runtimeService.transmitPacket(inputBuilder.build());
            boolean result = output.get().isSuccessful();
            LOG.info("Send packet out to {} {},.", nodeId, result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            LOG.error("Send packet out to {} exception, message = {}.", nodeId, e.getMessage());
        }
    }

    public Integer getServerPort() {
        return gRPCServerPort;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public P4pluginP4runtimeService getRuntimeService() {
        return runtimeService;
    }

    public String getConfigFile() {
        return configFile;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getRuntimeFile() {
        return runtimeFile;
    }

    public String getServerIp() {
        return gRPCServerIp;
    }

    private enum Status {
        INITIALIZE,
        OPEN_STREAM_CHANNEL,
        SET_PIPELINE,
        ERROR
    }
}
