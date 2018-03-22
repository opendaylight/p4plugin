/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.device;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.ConnectivityState;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionId;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionIdGenerator;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionIdObserver;
import org.opendaylight.p4plugin.runtime.impl.stub.RuntimeStub;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.action.ActionParam;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.match.field.Field;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.match.field.field.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.match.field.field.match.type.EXACT;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.match.field.field.match.type.LPM;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.match.field.field.match.type.TERNARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.table.entry.ActionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.table.entry.action.type.ACTIONPROFILEGROUP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.table.entry.action.type.ACTIONPROFILEMEMBER;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.table.entry.action.type.DIRECTACTION;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.types.rev170808.ConfigState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class Device implements ElectionIdObserver {
    private static final Logger LOG = LoggerFactory.getLogger(Device.class);
    public final ServerAddress serverAddress;
    public final PipelineConfig pipelineConfig;
    public final Long deviceId;
    public final String nodeId;
    private RuntimeStub runtimeStub;
    private ConfigState configState;

    public Device(String nodeId, Long deviceId, ServerAddress serverAddress, PipelineConfig pipelineConfig) {
        this.deviceId = deviceId;
        this.nodeId = nodeId;
        this.serverAddress = serverAddress;
        this.pipelineConfig = pipelineConfig;
    }

    @Override
    public void update(ElectionId electionId) {
        if (runtimeStub != null) {
            runtimeStub.updateElectionId(electionId);
        }
    }

    public void init() {
        newRuntimeStub();
        addElectionIdObserver();
    }

    private void newRuntimeStub() {
        String ip = serverAddress.getIp();
        Integer port = serverAddress.getPort();
        runtimeStub = new RuntimeStub(ip, port, deviceId, nodeId);
    }

    private void addElectionIdObserver() {
        ElectionIdGenerator.getInstance().addObserver(this);
    }

    private void setConfigState(ConfigState configState) {
        this.configState = configState;
    }

    public ConfigState getConfigState() {
        return configState;
    }

    public ConnectivityState getChannelState() {
        return runtimeStub.getConnectState();
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig() {
        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();
        p4DeviceConfigBuilder.setDeviceData(pipelineConfig.getDeviceConfig());
        configBuilder.setP4Info(pipelineConfig.getRuntimeInfo());

        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        configBuilder.setDeviceId(deviceId);
        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                .setAction(SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT)
                .addConfigs(configBuilder)
                .build();
        SetForwardingPipelineConfigResponse response;
        response = runtimeStub.setPipelineConfig(request);
        setConfigState(ConfigState.CONFIGURED);
        return response;
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig() {
        GetForwardingPipelineConfigRequest request = GetForwardingPipelineConfigRequest.newBuilder()
                .addDeviceIds(deviceId)
                .build();
        GetForwardingPipelineConfigResponse response;
        response = runtimeStub.getPipelineConfig(request);
        return response;
    }

    public WriteResponse addTableEntry(org.opendaylight.yang.gen.v1.urn.opendaylight
                                               .p4plugin.table.rev170808.TableEntry entry) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(ConvertToProtoTableEntry(entry), Update.Type.INSERT);
        response = runtimeStub.write(request);
        return response;
    }

    public WriteResponse modifyTableEntry(org.opendaylight.yang.gen.v1.urn.opendaylight
                                                  .p4plugin.table.rev170808.TableEntry entry) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(ConvertToProtoTableEntry(entry), Update.Type.MODIFY);
        response = runtimeStub.write(request);
        return response;
    }

    public WriteResponse deleteTableEntry(org.opendaylight.yang.gen.v1.urn.opendaylight
                                                  .p4plugin.table.rev170808.TableEntryKey entryKey) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(ConvertToProtoTableEntry(entryKey), Update.Type.DELETE);
        response = runtimeStub.write(request);
        return response;
    }

    public List<String> readTableEntry(String tableName) {
        ReadRequest.Builder request = ReadRequest.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder entryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        entryBuilder.setTableId(pipelineConfig.getTableId(tableName));
        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        request.setDeviceId(deviceId);

        Iterator<ReadResponse> responses = runtimeStub.read(request.build());
        List<java.lang.String> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                String str = ConvertToJsonEntry(entity.getTableEntry());
                result.add(str);
            });
            if (isCompleted) break;
        }
        return result;
    }

    public void openStreamChannel() {
        runtimeStub.streamChannel();
    }

    public void transmitPacket(byte[] payload) {
        runtimeStub.transmitPacket(payload);
    }

    public void shutdown() {
        runtimeStub.shutdown();
    }

    private FieldMatch exactMatchParse(EXACT exact, String tableName, String fieldName) {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.Exact.Builder exactBuilder = FieldMatch.Exact.newBuilder();
        Integer matchFieldWidth = pipelineConfig.getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = pipelineConfig.getMatchFieldId(tableName, fieldName);
        exactBuilder.setValue(ByteString.copyFrom(exact.getExactValue(), 0, matchFieldWidth));
        fieldMatchBuilder.setExact(exactBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private FieldMatch lpmMatchParse(LPM lpm, String tableName, String fieldName) {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.LPM.Builder lpmBuilder = FieldMatch.LPM.newBuilder();
        Integer matchFieldWidth = pipelineConfig.getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = pipelineConfig.getMatchFieldId(tableName, fieldName);
        lpmBuilder.setValue(ByteString.copyFrom(lpm.getLpmValue(), 0, matchFieldWidth));
        lpmBuilder.setPrefixLen(lpm.getPrefixLen().intValue());
        fieldMatchBuilder.setLpm(lpmBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private FieldMatch ternaryMatchParse(TERNARY ternary, String tableName, String fieldName) {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.Ternary.Builder ternaryBuilder = FieldMatch.Ternary.newBuilder();
        Integer matchFieldWidth = pipelineConfig.getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = pipelineConfig.getMatchFieldId(tableName, fieldName);
        ternaryBuilder.setValue(ByteString.copyFrom(ternary.getTernaryValue(), 0, matchFieldWidth));
        ternaryBuilder.setMask(ByteString.copyFrom(ternary.getMask(), 0, matchFieldWidth));
        fieldMatchBuilder.setTernary(ternaryBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private TableAction directActionParse(DIRECTACTION action) {
        TableAction.Builder tableActionBuilder = TableAction.newBuilder();
        Action.Builder actionBuilder = Action.newBuilder();
        List<ActionParam> params = action.getActionParam();
        String actionName = action.getActionName();
        actionBuilder.setActionId(pipelineConfig.getActionId(actionName));

        params.forEach(p->{
            Action.Param.Builder paramBuilder = Action.Param.newBuilder();
            String paramName = p.getParamName();
            int paramId = pipelineConfig.getParamId(actionName, paramName);
            int paramWidth = pipelineConfig.getParamWidth(actionName, paramName);
            paramBuilder.setParamId(paramId);
            paramBuilder.setValue(ByteString.copyFrom(p.getParamValue(), 0, paramWidth));
            actionBuilder.addParams(paramBuilder);
        });

        return tableActionBuilder.setAction(actionBuilder).build();
    }

    private TableAction memberActionParse(ACTIONPROFILEMEMBER memberAction) {
        TableAction.Builder builder = TableAction.newBuilder();
        builder.setActionProfileMemberId(memberAction.getMemberId().intValue());
        return builder.build();
    }

    private TableAction groupActionParse(ACTIONPROFILEGROUP groupAction) {
        TableAction.Builder builder = TableAction.newBuilder();
        builder.setActionProfileGroupId(groupAction.getGroupId().intValue());
        return builder.build();
    }

    private FieldMatch buildFieldMatch(String tableName, Field fields) {
        MatchType matchType = fields.getMatchType();
        String fieldName = fields.getFieldName();
        FieldMatch fieldMatch;

        if (matchType instanceof EXACT) {
            fieldMatch = exactMatchParse((EXACT)matchType, tableName, fieldName);
        } else if (matchType instanceof LPM) {
            fieldMatch = lpmMatchParse((LPM)matchType, tableName, fieldName);
        } else if (matchType instanceof TERNARY) {
            fieldMatch =  ternaryMatchParse((TERNARY) matchType, tableName, fieldName);
        } else {
            throw new IllegalArgumentException("Invalid match type = " + matchType.toString());
        }

        return fieldMatch;
    }

    private TableAction buildTableAction(ActionType actionType) {
        TableAction tableAction;
        if (actionType instanceof DIRECTACTION) {
            tableAction =  directActionParse((DIRECTACTION)actionType);
        } else if (actionType instanceof ACTIONPROFILEMEMBER) {
            tableAction =  memberActionParse((ACTIONPROFILEMEMBER)actionType);
        } else if (actionType instanceof ACTIONPROFILEGROUP) {
            tableAction = groupActionParse(((ACTIONPROFILEGROUP) actionType));
        } else {
            throw new IllegalArgumentException("Invalid action type = " + actionType.toString());
        }
        return  tableAction;
    }

    private TableEntry ConvertToProtoTableEntry(org.opendaylight.yang.gen.v1.urn.opendaylight
                                                  .p4plugin.table.rev170808.TableEntry entry) {
        String tableName = entry.getTableName();
        int tableId = pipelineConfig.getTableId(tableName);
        TableEntry.Builder tableEntryBuilder = TableEntry.newBuilder();
        List<Field> fields = entry.getField();
        fields.forEach(field -> tableEntryBuilder.addMatch(buildFieldMatch(tableName, field)));
        ActionType actionType = entry.getActionType();
        TableAction tableAction = buildTableAction(actionType);
        tableEntryBuilder.setPriority(entry.getPriority());
        tableEntryBuilder.setControllerMetadata(entry.getControllerMetadata().longValue());
        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.setAction(tableAction);
        return tableEntryBuilder.build();
    }

    public TableEntry ConvertToProtoTableEntry(org.opendaylight.yang.gen.v1.urn.opendaylight
                                                       .p4plugin.table.rev170808.TableEntryKey entryKey) {
        String tableName = entryKey.getTableName();
        int tableId = pipelineConfig.getTableId(tableName);
        TableEntry.Builder tableEntryBuilder = TableEntry.newBuilder();
        List<Field> fields = entryKey.getField();
        fields.forEach(field -> tableEntryBuilder.addMatch(buildFieldMatch(tableName, field)));
        tableEntryBuilder.setTableId(tableId);
        return tableEntryBuilder.build();
    }

    public String ConvertToJsonEntry(TableEntry entry) {
        try {
            String result = JsonFormat.printer().print(entry);
            return result;
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e);
        }
    }

    private WriteRequest createWriteRequest(TableEntry entry, Update.Type type) {
        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        Update.Builder updateBuilder = Update.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(entry);
        updateBuilder.setType(type);
        updateBuilder.setEntity(entityBuilder);
        requestBuilder.setDeviceId(deviceId);
        requestBuilder.addUpdates(updateBuilder);
        return requestBuilder.build();
    }

    private WriteRequest createWriteRequest(ActionProfileGroup group, Update.Type type) {
        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        Update.Builder updateBuilder = Update.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileGroup(group);
        updateBuilder.setEntity(entityBuilder);
        updateBuilder.setType(type);
        requestBuilder.setDeviceId(deviceId);
        requestBuilder.addUpdates(updateBuilder);
        return requestBuilder.build();
    }

    private WriteRequest createWriteRequest(ActionProfileMember member, Update.Type type) {
        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        Update.Builder updateBuilder = Update.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setActionProfileMember(member);
        updateBuilder.setType(type);
        updateBuilder.setEntity(entityBuilder);
        requestBuilder.setDeviceId(deviceId);
        requestBuilder.addUpdates(updateBuilder);
        return requestBuilder.build();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Device) {
            Device dev = (Device)obj;
            boolean isNodeEqual = dev.nodeId.equals(nodeId);
            boolean isServerEqual = dev.serverAddress.getIp().equals(this.serverAddress.getIp())
                                 && dev.serverAddress.getPort().equals(this.serverAddress.getPort())
                                 && dev.deviceId.equals(this.deviceId);
            return isNodeEqual && isServerEqual;
        }
        return false;
    }
}
