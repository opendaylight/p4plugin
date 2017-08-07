/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.p4plugin.channelimpl;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.*;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelImpl.class.getName());

    private final ManagedChannel channel;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private final P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private P4Info runTimeInfo;
    private ByteString logicInfo;
    private StreamObserver<StreamMessageRequest> requestObserver;
    public ChannelImpl(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    ChannelImpl(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
        asyncStub = P4RuntimeGrpc.newStub(channel);
    }

    public void shutdown() throws InterruptedException {
        //channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void parseRunTimeInfo(URL file) {
        try {
            runTimeInfo = ChannelUtils.parseRunTimeInfo(file);
        } catch (IOException e) {
            LOG.info("Parse runtime info failed, file = {}.", file);
        }
    }

    public void parseDeviceConfigInfo(URL file) {
        try {
            logicInfo = ChannelUtils.parseDeviceConfigInfo(file);
        } catch (IOException e) {
            LOG.info("Parse device config failed, file = {}.", file);
        }
    }

    /**
     * Get table id by table name from p4 runtime info;
     */
    public int getTableId(String tableName) {
        List<Table> list = runTimeInfo.getTablesList();
        for(Table table : list) {
            Preamble preamble = table.getPreamble();
            if(preamble.getName().equals(tableName)) {
                return preamble.getId();
            }
        }
        return  -1;
    }

    /**
     * Get match field id by table name and match field name from p4 runtime info;
     */
    public int getMatchFieldId(String tableName, String matchFieldName) {
        List<Table> list = runTimeInfo.getTablesList();
        Table table = null;

        for(Table t : list) {
            if(t.getPreamble().getName().equals(tableName)) { table = t; break; }
        }

        List<MatchField> mfList = table.getMatchFieldsList();
        for(MatchField mf : mfList) {
            if (mf.getName().equals(matchFieldName)) { return mf.getId(); }
        }
        return  -1;
    }

    /**
     * Get correct match field width, round to the nearest integer.
     */
    public int getMatchFieldWidth(String tableName, String matchFieldName) {
        List<Table> list = runTimeInfo.getTablesList();
        Table table = null;

        for(Table t : list) {
            if(t.getPreamble().getName().equals(tableName)) { table = t; break; }
        }

        List<MatchField> mfList = table.getMatchFieldsList();
        for(MatchField mf : mfList) {
            if (mf.getName().equals(matchFieldName)) {
                return (mf.getBitwidth() + 7) / 8;
            }
        }
        return  -1;
    }

    /**
     * Get action id ny action name.
     */
    public int getActionId(String actionName) {
        List<org.opendaylight.p4plugin.p4info.proto.Action> actionList = runTimeInfo.getActionsList();
        for (org.opendaylight.p4plugin.p4info.proto.Action action : actionList) {
            Preamble preamble = action.getPreamble();
            if (preamble.getName().equals(actionName)) { return action.getPreamble().getId(); }
        }
        return -1;
    }

    /**
     * Get a concrete param id, a action may contain many params
     */
    public int getParamId(String actionName, String paramName) {
        List<org.opendaylight.p4plugin.p4info.proto.Action> actionList = runTimeInfo.getActionsList();
        for (org.opendaylight.p4plugin.p4info.proto.Action action : actionList) {
            Preamble preamble = action.getPreamble();
            if (!preamble.getName().equals(actionName)) { continue; }
            for (org.opendaylight.p4plugin.p4info.proto.Action.Param param : action.getParamsList()) {
                if (param.getName().equals(paramName)) { return param.getId(); }
            }
        }
        return -1;
    }

    /**
     * Get correct param width, round to the nearest integer.
     */
    public int getParamWidth(String actionName, String paramName) {
        List<org.opendaylight.p4plugin.p4info.proto.Action> actionList = runTimeInfo.getActionsList();
        for (org.opendaylight.p4plugin.p4info.proto.Action action : actionList) {
            Preamble preamble = action.getPreamble();
            if (!preamble.getName().equals(actionName)) { continue; }
            for (org.opendaylight.p4plugin.p4info.proto.Action.Param param : action.getParamsList()) {
                if (param.getName().equals(paramName)) {
                    return (param.getBitwidth() + 7) / 8;
                }
            }
        }
        return -1;
    }

    /**
     * Human-readable entry metadata serialize to protobuf message
     */
    public TableEntry toMessage(TableEntryMetaData data) {
        String tableName = data.getTableName();
        String fieldName = data.getFieldName();
        String actionName = data.getActionName();

        int tableId = getTableId(tableName);
        int matchFieldId = getMatchFieldId(tableName, fieldName);
        int actionId = getActionId(actionName);

        org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder = org.opendaylight.p4plugin
                                                                                 .p4runtime
                                                                                 .proto
                                                                                 .Action
                                                                                 .newBuilder();
        actionBuilder.setActionId(actionId);
        for (String k : data.getParams().keySet()) {
            org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder = org.opendaylight.p4plugin
                                                                                         .p4runtime
                                                                                         .proto
                                                                                         .Action
                                                                                         .Param
                                                                                         .newBuilder();
            paramBuilder.setParamId(getParamId(actionName, k));
            paramBuilder.setValue(ByteString.copyFrom(ChannelUtils.strToByteArray(data.getParams().get(k),
                                                                                  getParamWidth(actionName, k))));
            actionBuilder.addParams(paramBuilder.build());
        }

        TableAction.Builder tableActionBuilder = TableAction.newBuilder();
        tableActionBuilder.setAction(actionBuilder.build());
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        fieldMatchBuilder.setFieldId(matchFieldId);

        switch(data.getMatchType()) {
            case EXACT: {
                FieldMatch.Exact.Builder exactBuilder = FieldMatch.Exact.newBuilder();
                exactBuilder.setValue(ByteString.copyFrom(ChannelUtils.strToByteArray(data.getMatchValue(),
                                                          getMatchFieldWidth(tableName, fieldName))));
                fieldMatchBuilder.setExact(exactBuilder.build());
                break;
            }
            case TERNARY:
                break;
            case LPM: {
                FieldMatch.LPM.Builder lpmBuilder = FieldMatch.LPM.newBuilder();
                lpmBuilder.setValue(ByteString.copyFrom(ChannelUtils.strToByteArray(data.getMatchValue(),
                                                        getMatchFieldWidth(tableName, fieldName))));
                lpmBuilder.setPrefixLen(data.getPrefixLen());
                fieldMatchBuilder.setLpm(lpmBuilder.build());
                break;
            }
            case RANGE:
                break;
            case VALID:
                break;
            default:
                break;
        }

        TableEntry.Builder tableEntryBuilder = TableEntry.newBuilder();
        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.addMatch(fieldMatchBuilder.build());
        tableEntryBuilder.setAction(tableActionBuilder.build());

        return tableEntryBuilder.build();
    }

    public void setForwardingPipelineConfig(URL logicFile, URL runTimeFile, long deviceId) {
        parseRunTimeInfo(runTimeFile);
        parseDeviceConfigInfo(logicFile);

        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();
        configBuilder.setDeviceId(deviceId);
        configBuilder.setP4Info(runTimeInfo);
        p4DeviceConfigBuilder.setDeviceData(logicInfo);

        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                                                     .setAction(org.opendaylight.p4plugin.p4runtime.proto
                                                                .SetForwardingPipelineConfigRequest.Action
                                                                .VERIFY_AND_COMMIT)
                                                     .addConfigs(configBuilder.build())
                                                     .build();
        SetForwardingPipelineConfigResponse response;

        try {
            /* response is empty now */
            response = blockingStub.setForwardingPipelineConfig(request);
        } catch (StatusRuntimeException e) {
            LOG.info("setForwardingPipelineConfig RPC failed: {}", e.getStatus());
        }
    }

    public void addTableEntry(TableEntryMetaData data) {
        WriteRequest.Builder request = WriteRequest.newBuilder();
        request.setDeviceId(data.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.valueOf(data.getUpdateType().toString()));

        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(toMessage(data));
        updateBuilder.setEntity(entityBuilder.build());
        request.addUpdates(updateBuilder.build());

        WriteResponse response = null;
        try {
            response = blockingStub.write(request.build());
        } catch (StatusRuntimeException e) {
            LOG.info("addTableEntry RPC failed: {}", e.getStatus());
        }
    }

    /**
     * This channel is bidirectional stream for packet-in and packet-out, do not close.
     */
    public void initBidirectionalStreamChannel() {
        requestObserver = asyncStub.streamChannel(new StreamObserver<StreamMessageResponse>() {
            @Override
            public void onNext(StreamMessageResponse value) {
                LOG.info("Receive packet-in, packet = {}.", value.getPacket());
                LOG.info("Receive packet-in, payload = {} size = {}.",  value.getPacket().getPayload(),
                                                                        value.getPacket().getPayload().size());
                LOG.info("Receive packet-in, payload = {} size = {}.",  value.getPacket().getIngressLogicalPort(),
                                                                        value.getPacket().getIngressPhysicalPort());
            }

            @Override
            public void onError(Throwable t) {
                LOG.info("response on onError {}", Status.fromThrowable(t));
            }

            @Override
            public void onCompleted() {
                LOG.info("response on completed");
            }
        });

        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        MasterArbitrationUpdate.Builder updateBuilder = MasterArbitrationUpdate.newBuilder();
        updateBuilder.setDeviceId(0);
        requestBuilder.setArbitration(updateBuilder.build());
        requestObserver.onNext(requestBuilder.build());
    }

    public void packetOut() {
        try {
            StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
            PacketOut.Builder packetBuilder = PacketOut.newBuilder();
            byte[] payload = new byte[100];
            Arrays.fill(payload, (byte)0x5a);
            packetBuilder.setPayload(ByteString.copyFrom(payload));
            packetBuilder.setEgressPhysicalPort(1);
            requestObserver.onNext(requestBuilder.setPacket(packetBuilder.build()).build());
        } catch (RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
    }
}
