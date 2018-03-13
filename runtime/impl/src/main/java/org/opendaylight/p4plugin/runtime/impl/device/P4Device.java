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
import org.opendaylight.p4plugin.p4info.proto.Table;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.p4plugin.p4runtime.proto.Action;
import org.opendaylight.p4plugin.runtime.impl.stub.RuntimeStub;
import org.opendaylight.p4plugin.runtime.impl.utils.Utils;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.ActionProfileGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.ActionProfileMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.TableEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.action.ActionParam;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.match.field.Field;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.match.field.field.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.match.field.field.match.type.EXACT;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.match.field.field.match.type.LPM;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.match.field.field.match.type.RANGE;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.match.field.field.match.type.TERNARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.table.entry.ActionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.table.entry.action.type.ACTIONPROFILEGROUP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.table.entry.action.type.ACTIONPROFILEMEMBER;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.table.entry.action.type.DIRECTACTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;

public class P4Device  {
    private static final Logger LOG = LoggerFactory.getLogger(P4Device.class);
    private RuntimeStub runtimeStub;
    private P4Info runtimeInfo;
    private ByteString deviceConfig;
    private String ip;
    private Integer port;
    private Long deviceId;
    private String nodeId;
    private boolean isConfigured;

    private P4Device(String ip, Integer port, Long deviceId, String nodeId,
                     P4Info runtimeInfo, ByteString deviceConfig) {
        this.ip = ip;
        this.port = port;
        this.deviceId = deviceId;
        this.nodeId = nodeId;
        this.runtimeInfo = runtimeInfo;
        this.deviceConfig = deviceConfig;
    }

    public boolean getConnectState() {
        return runtimeStub.getConnectState();
    }

    public boolean isConfigured() {
        return runtimeInfo != null && deviceConfig != null && isConfigured;
    }

    public String getNodeId() {
        return nodeId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    private int getTableId(String tableName) {
        Optional<Table> optional = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid table name"))
                .getPreamble().getId();
    }

    private String getTableName(int tableId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> optional = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid table id"))
                .getPreamble().getName();
    }

    private int getMatchFieldId(String tableName, String matchFieldName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer =
                tableContainer.orElseThrow(()-> new IllegalArgumentException("Invalid table name"))
                        .getMatchFieldsList()
                        .stream()
                        .filter(matchField -> matchField.getName().equals(matchFieldName))
                        .findFirst();

        return matchFieldContainer.orElseThrow(()-> new IllegalArgumentException("Invalid match field name"))
                .getId();
    }

    private String getMatchFieldName(int tableId, int matchFieldId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer =
                tableContainer.orElseThrow(()-> new IllegalArgumentException("Invalid table id"))
                        .getMatchFieldsList()
                        .stream()
                        .filter(matchField -> matchField.getId() == (matchFieldId))
                        .findFirst();

        return matchFieldContainer.orElseThrow(()-> new IllegalArgumentException("Invalid match field id"))
                .getName();
    }

    private int getMatchFieldWidth(String tableName, String matchFieldName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer =
                tableContainer.orElseThrow(()-> new IllegalArgumentException("Invalid table name"))
                        .getMatchFieldsList()
                        .stream()
                        .filter(matchField -> matchField.getName().equals(matchFieldName))
                        .findFirst();

        return (matchFieldContainer.orElseThrow(()-> new IllegalArgumentException("Invalid match field name"))
                .getBitwidth() + 7 ) / 8;
    }

    private int getActionId(String actionName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> optional = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid action name"))
                .getPreamble().getId();
    }

    private String getActionName(int actionId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> optional = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid action id"))
                .getPreamble().getName();
    }

    private int getParamId(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                actionContainer.orElseThrow(()-> new IllegalArgumentException("Invalid action name"))
                        .getParamsList()
                        .stream()
                        .filter(param -> param.getName().equals(paramName))
                        .findFirst();

        return paramContainer.orElseThrow(()-> new IllegalArgumentException("Invalid param name"))
                .getId();
    }

    private String getParamName(int actionId, int paramId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                actionContainer.orElseThrow(()-> new IllegalArgumentException("Invalid action id"))
                        .getParamsList()
                        .stream()
                        .filter(param -> param.getId() == paramId)
                        .findFirst();

        return paramContainer.orElseThrow(()-> new IllegalArgumentException("Invalid param id"))
                .getName();
    }

    private int getParamWidth(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                actionContainer.orElseThrow(()-> new IllegalArgumentException("Invalid action name"))
                        .getParamsList()
                        .stream()
                        .filter(param -> param.getName().equals(paramName))
                        .findFirst();

        return (paramContainer.orElseThrow(()-> new IllegalArgumentException("Invalid param name"))
                .getBitwidth() + 7 ) / 8;
    }

    private int getActionProfileId(String actionProfileName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.ActionProfile> optional = runtimeInfo.getActionProfilesList()
                .stream()
                .filter(actionProfile -> actionProfile.getPreamble().getName().equals(actionProfileName))
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid action profile name"))
                .getPreamble().getId();
    }

    private String getActionProfileName(Integer actionProfileId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.ActionProfile> optional = runtimeInfo.getActionProfilesList()
                .stream()
                .filter(actionProfile -> actionProfile.getPreamble().getId() == actionProfileId)
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid action profile id"))
                .getPreamble().getName();
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig() {
        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();
        p4DeviceConfigBuilder.setDeviceData(deviceConfig);
        configBuilder.setP4Info(runtimeInfo);

        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        configBuilder.setDeviceId(deviceId);
        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                .setAction(SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT)
                .addConfigs(configBuilder)
                .build();

        SetForwardingPipelineConfigResponse response;
        response = runtimeStub.setPipelineConfig(request);
        isConfigured = true;
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

    private WriteRequest createWriteRequest(org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry,
                                            Update.Type type) {
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

    private WriteRequest createWriteRequest(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup group,
                                            Update.Type type) {
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

    private WriteRequest createWriteRequest(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member,
                                            Update.Type type) {
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

    public WriteResponse addTableEntry(TableEntry inputEntry) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(toProtoEntry(inputEntry), Update.Type.INSERT);
        response = write(request);
        return response;
    }

    public WriteResponse modifyTableEntry(TableEntry inputEntry) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(toProtoEntry(inputEntry), Update.Type.MODIFY);
        response = write(request);
        return response;
    }

    public WriteResponse deleteTableEntry(TableEntryKey inputEntryKey) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(toProtoEntry(inputEntryKey), Update.Type.DELETE);
        response = write(request);
        return response;
    }

    public List<String> readTableEntry(String tableName) {
        ReadRequest.Builder request = ReadRequest.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder entryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        entryBuilder.setTableId(getTableId(tableName));
        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        request.setDeviceId(deviceId);

        Iterator<ReadResponse> responses = read(request.build());
        List<java.lang.String> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                String str = toStringEntry(entity.getTableEntry());
                result.add(str);
            });
            if (isCompleted) break;
        }
        return result;
    }

    public WriteResponse addActionProfileMember(ActionProfileMember inputMember) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(toProtoMember(inputMember), Update.Type.INSERT);
        response = write(request);
        return response;
    }

    public WriteResponse modifyActionProfileMember(ActionProfileMember inputMember) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(toProtoMember(inputMember), Update.Type.MODIFY);
        response = write(request);
        return response;
    }

    public WriteResponse deleteActionProfileMember(ActionProfileMemberKey inputMemberKey) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(toProtoMember(inputMemberKey), Update.Type.DELETE);
        response = write(request);
        return response;
    }

    public List<String> readActionProfileMember(String actionProfileName) {
        ReadRequest.Builder requestBuilder = ReadRequest.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.Builder memberBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.newBuilder();
        memberBuilder.setActionProfileId(getActionProfileId(actionProfileName));
        entityBuilder.setActionProfileMember(memberBuilder);
        requestBuilder.setDeviceId(deviceId);
        requestBuilder.addEntities(entityBuilder);

        Iterator<ReadResponse> responses = read(requestBuilder.build());
        List<String> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                String str = toStringMember(entity.getActionProfileMember());
                result.add(str);
            });
            if (isCompleted) break;
        }
        return result;
    }

    public WriteResponse addActionProfileGroup(ActionProfileGroup inputGroup) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(toProtoGroup(inputGroup), Update.Type.INSERT);
        response = write(request);
        return response;
    }

    public WriteResponse modifyActionProfileGroup(ActionProfileGroup inputGroup) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(toProtoGroup(inputGroup), Update.Type.MODIFY);
        response = write(request);
        return response;
    }

    public WriteResponse deleteActionProfileGroup(ActionProfileGroupKey inputGroupKey) {
        WriteResponse response;
        WriteRequest request = createWriteRequest(toProtoGroup(inputGroupKey), Update.Type.DELETE);
        response = write(request);
        return response;
    }

    public List<String> readActionProfileGroup(String actionProfileName) {
        ReadRequest.Builder requestBuilder = ReadRequest.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Builder groupBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(getActionProfileId(actionProfileName));
        entityBuilder.setActionProfileGroup(groupBuilder);
        requestBuilder.setDeviceId(deviceId);
        requestBuilder.addEntities(entityBuilder);

        Iterator<ReadResponse> responses = read(requestBuilder.build());
        List<String> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                String str = toStringGroup(entity.getActionProfileGroup());
                result.add(str);
            });
            if (isCompleted) break;
        }
        return result;
    }

    public WriteResponse write(WriteRequest request) {
        WriteResponse response = runtimeStub.write(request);
        return response;
    }

    public Iterator<ReadResponse> read(ReadRequest request) {
        Iterator<ReadResponse> responses;
        responses = runtimeStub.read(request);
        return responses;
    }

    public void transmitPacket(byte[] payload) {
        runtimeStub.transmitPacket(payload);
    }

    public void connectToDevice() {
        if (runtimeStub != null) {
            runtimeStub.shutdown();
        }
        runtimeStub = new RuntimeStub(ip, port, deviceId, nodeId);
        runtimeStub.notifyWhenStateChanged(ConnectivityState.READY, ()->isConfigured = false);
        runtimeStub.streamChannel();

    }

    public void shutdown() {
        runtimeStub.shutdown();
    }

    private TableAction directActionParse(DIRECTACTION action) {
        TableAction.Builder tableActionBuilder = TableAction.newBuilder();
        Action.Builder actionBuilder = Action.newBuilder();
        List<ActionParam> params = action.getActionParam();
        String actionName = action.getActionName();
        actionBuilder.setActionId(getActionId(actionName));

        params.forEach(p->{
            Action.Param.Builder paramBuilder = Action.Param.newBuilder();
            String paramName = p.getParamName();
            int paramId = getParamId(actionName, paramName);
            int paramWidth = getParamWidth(actionName, paramName);
            paramBuilder.setParamId(paramId);
            String valueStr = p.getParamValue();
            byte[] valueBytes = Utils.strToByteArray(valueStr, paramWidth);
            paramBuilder.setValue(ByteString.copyFrom(valueBytes));
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

    private TableAction buildTableAction(ActionType actionType) {
        if (actionType instanceof DIRECTACTION) {
            return directActionParse((DIRECTACTION)actionType);
        } else if (actionType instanceof ACTIONPROFILEMEMBER) {
            return memberActionParse((ACTIONPROFILEMEMBER)actionType);
        } else if (actionType instanceof ACTIONPROFILEGROUP) {
            return groupActionParse(((ACTIONPROFILEGROUP) actionType));
        } else {
            throw new IllegalArgumentException("Invalid action type");
        }
    }

    private FieldMatch exactMatchParse(EXACT exact, String tableName, String fieldName) {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.Exact.Builder exactBuilder = FieldMatch.Exact.newBuilder();
        Integer matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = getMatchFieldId(tableName, fieldName);
        String valueStr = exact.getExactValue().getValue();
        byte[] valeBytes = Utils.strToByteArray(valueStr, matchFieldWidth);
        exactBuilder.setValue(ByteString.copyFrom(valeBytes, 0, matchFieldWidth));
        fieldMatchBuilder.setExact(exactBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private FieldMatch lpmMatchParse(LPM lpm, String tableName, String fieldName) {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.LPM.Builder lpmBuilder = FieldMatch.LPM.newBuilder();
        Integer matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = getMatchFieldId(tableName, fieldName);
        String valueStr = lpm.getLpmValue().getValue();
        byte[] valeBytes = Utils.strToByteArray(valueStr, matchFieldWidth);
        lpmBuilder.setValue(ByteString.copyFrom(valeBytes, 0, matchFieldWidth));
        lpmBuilder.setPrefixLen(lpm.getPrefixLen().intValue());
        fieldMatchBuilder.setLpm(lpmBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private byte[] getMask(Short mask) {
        StringBuffer buffer = new StringBuffer(128);
        for (int i = 0; i < 128; i++) {
            if (i < mask) {
                buffer.append('1');
            } else {
                buffer.append('0');
            }
        }

        byte[] result = new byte[16];
        for (int i = 0; i < 16; i++) {
            result[i] = (byte)Integer.parseInt(buffer.substring(i * 8, i * 8 + 8),2);
        }
        return result;
    }

    private FieldMatch ternaryMatchParse(TERNARY ternary, String tableName, String fieldName) {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.Ternary.Builder ternaryBuilder = FieldMatch.Ternary.newBuilder();
        String valueStr = new String(ternary.getTernaryValue().getValue());
        Short mask = ternary.getMask();
        Integer matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = getMatchFieldId(tableName, fieldName);
        byte[] valueBytes = Utils.strToByteArray(valueStr, matchFieldWidth);
        byte[] maskBytes = getMask(mask);
        ternaryBuilder.setValue(ByteString.copyFrom(valueBytes, 0, matchFieldWidth));
        ternaryBuilder.setMask(ByteString.copyFrom(maskBytes, 0, matchFieldWidth));
        fieldMatchBuilder.setTernary(ternaryBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private FieldMatch rangeMatchParse(RANGE range, String tableName, String fieldName) {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.Range.Builder rangeBuilder = FieldMatch.Range.newBuilder();
        BigInteger high = range.getRangeValueHigh();
        BigInteger low = range.getRangeValueLow();
        Integer matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = getMatchFieldId(tableName, fieldName);
        rangeBuilder.setHigh(ByteString.copyFrom(high.toByteArray(), 0, matchFieldWidth));
        rangeBuilder.setLow(ByteString.copyFrom(low.toByteArray(), 0, matchFieldWidth));
        fieldMatchBuilder.setFieldId(matchFieldId);
        fieldMatchBuilder.setRange(rangeBuilder);
        return fieldMatchBuilder.build();
    }

    private FieldMatch buildFieldMatch(Field fields, String tableName) {
        MatchType matchType = fields.getMatchType();
        String fieldName = fields.getFieldName();

        if (matchType instanceof EXACT) {
            return exactMatchParse((EXACT)matchType, tableName, fieldName);
        } else if (matchType instanceof LPM) {
            return lpmMatchParse((LPM)matchType, tableName, fieldName);
        } else if (matchType instanceof TERNARY) {
            return ternaryMatchParse((TERNARY)matchType, tableName, fieldName);
        } else if (matchType instanceof RANGE) {
            return rangeMatchParse((RANGE) matchType, tableName, fieldName);
        } else {
            throw new IllegalArgumentException("Invalid match type");
        }
    }

    public org.opendaylight.p4plugin.p4runtime.proto.TableEntry toProtoEntry(TableEntry entry) {
        String tableName = entry.getTableName();
        int tableId = getTableId(tableName);
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder tableEntryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        List<Field> fields = entry.getField();
        fields.forEach(field -> tableEntryBuilder.addMatch(buildFieldMatch(field, tableName)));
        ActionType actionType = entry.getActionType();
        org.opendaylight.p4plugin.p4runtime.proto.TableAction tableAction = buildTableAction(actionType);
        tableEntryBuilder.setPriority(entry.getPriority());
        tableEntryBuilder.setControllerMetadata(entry.getControllerMetadata().longValue());
        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.setAction(tableAction);
        return tableEntryBuilder.build();
    }

    public org.opendaylight.p4plugin.p4runtime.proto.TableEntry toProtoEntry(TableEntryKey entryKey) {
        String tableName = entryKey.getTableName();
        int tableId = getTableId(tableName);
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder tableEntryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        List<Field> fields = entryKey.getField();
        fields.forEach(field -> tableEntryBuilder.addMatch(buildFieldMatch(field, tableName)));
        tableEntryBuilder.setTableId(tableId);
        return tableEntryBuilder.build();
    }

    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember toProtoMember(ActionProfileMember member) {
        String actionName = member.getActionName();
        Long memberId = member.getMemberId();
        String actionProfile = member.getActionProfileName();
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.Builder memberBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();
        actionBuilder.setActionId(getActionId(actionName));

        member.getActionParam().forEach(actionParam -> {
            org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder();
            String paramName = actionParam.getParamName();
            int paramId = getParamId(actionName, paramName);
            int paramWidth = getParamWidth(actionName, paramName);
            String valueStr = actionParam.getParamValue();
            byte[] valueBytes = Utils.strToByteArray(valueStr, paramWidth);
            paramBuilder.setValue(ByteString.copyFrom(valueBytes));
            paramBuilder.setParamId(paramId);
            actionBuilder.addParams(paramBuilder);
        });

        memberBuilder.setAction(actionBuilder);
        memberBuilder.setActionProfileId(getActionProfileId(actionProfile));
        memberBuilder.setMemberId(memberId.intValue());
        return memberBuilder.build();
    }

    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember toProtoMember(ActionProfileMemberKey memberKey) {
        Long memberId = memberKey.getMemberId();
        String actionProfile = memberKey.getActionProfileName();
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.Builder memberBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.newBuilder();
        memberBuilder.setActionProfileId(getActionProfileId(actionProfile));
        memberBuilder.setMemberId(memberId.intValue());
        return memberBuilder.build();
    }

    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup toProtoGroup(ActionProfileGroup group) {
        Long groupId = group.getGroupId();
        String actionProfile = group.getActionProfileName();
        Integer maxSize = group.getMaxSize();

        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Builder groupBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(getActionProfileId(actionProfile));
        groupBuilder.setGroupId(groupId.intValue());
        groupBuilder.setMaxSize(maxSize);

        group.getGroupMember().forEach(groupMember -> {
            org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Member.Builder builder =
                    org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Member.newBuilder();
            builder.setWatch(groupMember.getWatch().intValue());
            builder.setWeight(groupMember.getWeight());
            builder.setMemberId(groupMember.getMemberId().intValue());
            groupBuilder.addMembers(builder);
        });

        return groupBuilder.build();
    }

    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup toProtoGroup(ActionProfileGroupKey groupKey) {
        Long groupId = groupKey.getGroupId();
        String actionProfile = groupKey.getActionProfileName();
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Builder groupBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(getActionProfileId(actionProfile));
        groupBuilder.setGroupId(groupId.intValue());
        return groupBuilder.build();
    }

    public String toStringEntry(org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry) {
        try {
            String result = JsonFormat.printer().print(entry);
            return result;
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e);
        }
    }

    public String toStringMember(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member) {
        try {
            String result = JsonFormat.printer().print(member);
            return result;
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e);
        }
    }

    public String toStringGroup(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup group) {
        try {
            String result = JsonFormat.printer().print(group);
            return result;
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private P4Info runtimeInfo_;
        private ByteString deviceConfig_;
        private Long deviceId_;
        private String nodeId_;
        private String ip_;
        private Integer port_;

        public Builder setIp(String ip) {
            this.ip_ = ip;
            return this;
        }

        public Builder setPort(Integer port) {
            this.port_ = port;
            return this;
        }

        public Builder setRuntimeInfo(P4Info p4Info) {
            this.runtimeInfo_ = p4Info;
            return this;
        }

        public Builder setDeviceConfig(ByteString config) {
            this.deviceConfig_ = config;
            return this;
        }

        public Builder setDeviceId(Long deviceId) {
            this.deviceId_ = deviceId;
            return this;
        }

        public Builder setNodeId(String nodeId) {
            this.nodeId_ = nodeId;
            return this;
        }

        public P4Device build() {
            P4Device device = new P4Device(ip_,port_,deviceId_, nodeId_, runtimeInfo_,deviceConfig_);
            return device;
        }
    }

    @Override
    public String toString() {
        return String.format("%s/%d-%s:%d/%s/%s", nodeId, deviceId, ip, port,
                getConnectState(), isConfigured);
    }
}
