/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class SimpleRouter {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleRouter.class);
    private final String gRPCServerIp;
    private final Integer gRPCServerPort;
    private final Long deviceId;
    private final String nodeId;
    private final String configFile;
    private final String runtimeFile;
    private final P4pluginP4runtimeService runtimeService;
    private Status status;

    public SimpleRouter(String gRPCServerIp,
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
            LOG.info("Simple router open stream channel {}.", result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Simple router open stream channel exception, message = {}.", e.getMessage());
        }
        status = result ? Status.OPEN_STREAM_CHANNEL : Status.ERROR;
    }

    public void setPipelineConfig() {
        if (status != Status.OPEN_STREAM_CHANNEL) {
            return;
        }

        SetPipelineConfigInputBuilder inputBuilder = new SetPipelineConfigInputBuilder();
        inputBuilder.setNid(nodeId);
        boolean result;

        try {
            ListenableFuture<RpcResult<SetPipelineConfigOutput>> output = runtimeService.setPipelineConfig(inputBuilder.build());
            result = output.get().isSuccessful();
            LOG.info("Simple router set pipeline config {}.", result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Simple router set pipeline config exception, message = {}.", e.getMessage());
        }
        status = result ? Status.SET_PIPELINE_CONFIG : Status.ERROR;
    }

    public void setTableEntry() {
        if (status != Status.SET_PIPELINE_CONFIG) {
            return;
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String gRPCServerIp_;
        private Integer gRPCServerPort_;
        private Long deviceId_;
        private String nodeId_;
        private String configFile_;
        private String runtimeFile_;
        private P4pluginP4runtimeService runtimeService_;

        private Builder() {}

        public Builder setServerIp(String gRPCServerIp_) {
            this.gRPCServerIp_ = gRPCServerIp_;
            return this;
        }

        public Builder setServerPort(Integer gRPCServerPort_) {
            this.gRPCServerPort_ = gRPCServerPort_;
            return this;
        }

        public Builder setDeviceId(Long deviceId_) {
            this.deviceId_ = deviceId_;
            return this;
        }

        public Builder setNodeId(String nodeId_) {
            this.nodeId_ = nodeId_;
            return this;
        }

        public Builder setRuntimeFile(String runtimeFile_) {
            this.runtimeFile_ = runtimeFile_;
            return this;
        }

        public Builder setConfigFile(String configFile_) {
            this.configFile_ = configFile_;
            return this;
        }

        public Builder setRuntimeService(P4pluginP4runtimeService runtimeService_) {
            this.runtimeService_ = runtimeService_;
            return this;
        }

        public SimpleRouter build() {
            return new SimpleRouter(gRPCServerIp_, gRPCServerPort_, deviceId_,
                    nodeId_, configFile_, runtimeFile_, runtimeService_);
        }
    }

    enum Status {
        INITIALIZE,
        OPEN_STREAM_CHANNEL,
        SET_PIPELINE_CONFIG,
        SET_TABLE_ENTRY,
        ERROR,
    }
}
