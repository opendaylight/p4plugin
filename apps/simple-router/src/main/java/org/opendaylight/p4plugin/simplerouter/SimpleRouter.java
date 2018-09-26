/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.simplerouter;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.action.ActionParam;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.action.ActionParamBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.Field;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.FieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.LpmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.table.entry.action.type.DirectActionBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
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
    private Status status = Status.INITIALIZE;

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

    /**
    {
        "input": {
            "nid": "node0",
            "table-name": "ipv4_lpm",
            "action-name": "ipv4_forward",
            "action-param": [
                {
                    "param-name": "port",
                    "param-value": "2"
                },
                {
                    "param-name": "dstAddr",
                    "param-value": "00:04:00:00:00:01"
                }
            ],
            
            "field": [
                {
                    "field-name": "hdr.ipv4.dstAddr",
                    "lpm-value": "10.0.1.0",
                    "prefix-len": "24"
                }
            ]
        }
    }
    
    {
        "input": {
            "nid": "node0",
            "table-name": "ipv4_lpm",
            "action-name": "ipv4_forward",
            "action-param": [
                {
                    "param-name": "port",
                    "param-value": "1"
                },
                {
                    "param-name": "dstAddr",
                    "param-value": "00:04:00:00:00:00"
                }
            ],
            
            "field": [
                {
                    "field-name": "hdr.ipv4.dstAddr",
                    "lpm-value": "10.0.0.0",
                    "prefix-len": "24"
                }
            ]
        }
    }
    */
    public void addTableEntry() {
        if (status != Status.SET_PIPELINE_CONFIG) {
            return;
        }

        AddTableEntryInput entryInput1 = buildH1H2Entry();
        AddTableEntryInput entryInput2 = buildH2H1Entry();
        boolean result;
        try {
            ListenableFuture<RpcResult<AddTableEntryOutput>> output1 = runtimeService.addTableEntry(entryInput1);
            ListenableFuture<RpcResult<AddTableEntryOutput>> output2 = runtimeService.addTableEntry(entryInput2);
            boolean result1 = output1.get().isSuccessful();
            boolean result2 = output2.get().isSuccessful();
            LOG.info("Simple router add h1 -> h2 entry {}.", result1 ? "success" : "failed");
            LOG.info("Simple router add h2 -> h1 entry {}.", result2 ? "success" : "failed");
            result = result1 && result2;
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Simple router add h1 <-> h2 entry exception, message = {}.", e.getMessage());
        }

        status = result ? Status.SET_TABLE_ENTRY : Status.ERROR;
    }

    /* h1 -> h2 entry */
    private AddTableEntryInput buildH1H2Entry() {
        AddTableEntryInputBuilder inputBuilder = new AddTableEntryInputBuilder();
        inputBuilder.setNid(nodeId);
        inputBuilder.setTableName("ipv4_lpm");

        /* build match field */
        FieldBuilder fieldBuilder = new FieldBuilder();
        fieldBuilder.setFieldName("hdr.ipv4.dstAddr");

        LpmBuilder lpmBuilder = new LpmBuilder();
        lpmBuilder.setLpmValue(new TypedValue("10.0.1.0"));
        lpmBuilder.setPrefixLen((long)24);
        fieldBuilder.setMatchType(lpmBuilder.build());

        List<Field> fieldList = new ArrayList<>();
        fieldList.add(fieldBuilder.build());

        /* build action */
        ActionParamBuilder actionParamBuilder1 = new ActionParamBuilder();
        actionParamBuilder1.setParamName("port");
        actionParamBuilder1.setParamValue(new TypedValue("2"));

        ActionParamBuilder actionParamBuilder2 = new ActionParamBuilder();
        actionParamBuilder2.setParamName("dstAddr");
        actionParamBuilder2.setParamValue(new TypedValue("00:04:00:00:00:01"));

        List<ActionParam> actionParamList = new ArrayList<>();
        actionParamList.add(actionParamBuilder1.build());
        actionParamList.add(actionParamBuilder2.build());

        DirectActionBuilder directActionBuilder = new DirectActionBuilder();
        directActionBuilder.setActionName("ipv4_forward");
        directActionBuilder.setActionParam(actionParamList);

        inputBuilder.setField(fieldList);
        inputBuilder.setActionType(directActionBuilder.build());
        return inputBuilder.build();
    }

    /* h2 -> h1 entry */
    private AddTableEntryInput buildH2H1Entry() {
        AddTableEntryInputBuilder inputBuilder = new AddTableEntryInputBuilder();
        inputBuilder.setNid(nodeId);
        inputBuilder.setTableName("ipv4_lpm");

        /* build match field */
        FieldBuilder fieldBuilder = new FieldBuilder();
        fieldBuilder.setFieldName("hdr.ipv4.dstAddr");

        LpmBuilder lpmBuilder = new LpmBuilder();
        lpmBuilder.setLpmValue(new TypedValue("10.0.0.0"));
        lpmBuilder.setPrefixLen((long)24);
        fieldBuilder.setMatchType(lpmBuilder.build());

        List<Field> fieldList = new ArrayList<>();
        fieldList.add(fieldBuilder.build());

        /* build action */
        ActionParamBuilder actionParamBuilder1 = new ActionParamBuilder();
        actionParamBuilder1.setParamName("port");
        actionParamBuilder1.setParamValue(new TypedValue("1"));

        ActionParamBuilder actionParamBuilder2 = new ActionParamBuilder();
        actionParamBuilder2.setParamName("dstAddr");
        actionParamBuilder2.setParamValue(new TypedValue("00:04:00:00:00:00"));

        List<ActionParam> actionParamList = new ArrayList<>();
        actionParamList.add(actionParamBuilder1.build());
        actionParamList.add(actionParamBuilder2.build());

        DirectActionBuilder directActionBuilder = new DirectActionBuilder();
        directActionBuilder.setActionName("ipv4_forward");
        directActionBuilder.setActionParam(actionParamList);

        inputBuilder.setField(fieldList);
        inputBuilder.setActionType(directActionBuilder.build());
        return inputBuilder.build();
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
