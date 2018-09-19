/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;


import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4runtime.P4RuntimeClient;
import org.opendaylight.p4plugin.p4runtime.proto.*;


import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.TypedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class Device {
    private static final Logger LOG = LoggerFactory.getLogger(Device.class);
    private String nodeId;
    private Long deviceId;
    private boolean isConfigured;
    private ByteString deviceConfig;
    private P4Info p4Info;
    private P4RuntimeClient p4RuntimeClient;

    private Device(String nodeId, Long deviceId, P4RuntimeClient p4RuntimeClient, P4Info p4Info, ByteString deviceConfig) {
        this.nodeId = nodeId;
        this.deviceId = deviceId;
        this.p4RuntimeClient = p4RuntimeClient;
        this.p4Info = p4Info;
        this.deviceConfig = deviceConfig;
    }

    private int getTableId(String tableName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> optional = p4Info.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException(String.format("Table name %s.", tableName)))
                .getPreamble()
                .getId();
    }

    private String getTableName(int tableId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> optional = p4Info.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException(String.format("Table id %d.", tableId)))
                .getPreamble()
                .getName();
    }

    private int getMatchFieldId(String tableName, String matchFieldName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = p4Info.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer = tableContainer
                        .orElseThrow(()-> new IllegalArgumentException(String.format("Table name %s.", tableName)))
                        .getMatchFieldsList()
                        .stream()
                        .filter(matchField -> matchField.getName().equals(matchFieldName))
                        .findFirst();

        return matchFieldContainer
                .orElseThrow(()-> new IllegalArgumentException(String.format("Match field name %s.", matchFieldName)))
                .getId();
    }

    private String getMatchFieldName(int tableId, int matchFieldId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = p4Info.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer = tableContainer
                .orElseThrow(()-> new IllegalArgumentException(String.format("Table id %d.", tableId)))
                .getMatchFieldsList()
                .stream()
                .filter(matchField -> matchField.getId() == (matchFieldId))
                .findFirst();

        return matchFieldContainer
                .orElseThrow(()-> new IllegalArgumentException(String.format("Match field id %d.", matchFieldId)))
                .getName();
    }

    private int getMatchFieldWidth(String tableName, String matchFieldName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = p4Info.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer = tableContainer
                        .orElseThrow(()-> new IllegalArgumentException(String.format("Table name %s.", tableName)))
                        .getMatchFieldsList()
                        .stream()
                        .filter(matchField -> matchField.getName().equals(matchFieldName))
                        .findFirst();

        return (matchFieldContainer
                .orElseThrow(()-> new IllegalArgumentException(String.format("Match field name %s.",matchFieldName)))
                .getBitwidth() + 7 ) / 8;
    }

    private int getActionId(String actionName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> optional = p4Info.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException(String.format("Action name %s.", actionName)))
                .getPreamble()
                .getId();
    }

    private String getActionName(int actionId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> optional = p4Info.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException(String.format("Action id %d.", actionId)))
                .getPreamble()
                .getName();
    }

    private int getParamId(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = p4Info.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer = actionContainer
                .orElseThrow(()-> new IllegalArgumentException(String.format("Action name %s.", actionName)))
                .getParamsList()
                .stream()
                .filter(param -> param.getName().equals(paramName))
                .findFirst();

        return paramContainer.orElseThrow(()-> new IllegalArgumentException(String.format("Param name %s.", paramName)))
                .getId();
    }

    private String getParamName(int actionId, int paramId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = p4Info.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer = actionContainer
                .orElseThrow(()-> new IllegalArgumentException(String.format("Action id %d.", actionId)))
                .getParamsList()
                .stream()
                .filter(param -> param.getId() == paramId)
                .findFirst();

        return paramContainer.orElseThrow(()-> new IllegalArgumentException(String.format("Param id %d.", paramId)))
                .getName();
    }

    private int getParamWidth(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = p4Info.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer = actionContainer
                .orElseThrow(()-> new IllegalArgumentException(String.format("Action name %s.", actionName)))
                .getParamsList()
                .stream()
                .filter(param -> param.getName().equals(paramName))
                .findFirst();

        return (paramContainer.orElseThrow(()-> new IllegalArgumentException(String.format("Param name %s.", paramName)))
                .getBitwidth() + 7 ) / 8;
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig() {
        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();
        p4DeviceConfigBuilder.setDeviceData(deviceConfig);
        configBuilder.setP4Info(p4Info);

        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        configBuilder.setDeviceId(deviceId);
        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                .setAction(SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT)
                .addConfigs(configBuilder)
                .build();

        SetForwardingPipelineConfigResponse response;
        response = p4RuntimeClient.setPipelineConfig(request);
        isConfigured = true;
        return response;
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig() {
        checkInit();
        GetForwardingPipelineConfigResponse response;
        GetForwardingPipelineConfigRequest request = GetForwardingPipelineConfigRequest.newBuilder()
                .addDeviceIds(deviceId)
                .build();
        response = p4RuntimeClient.getPipelineConfig(request);
        return response;
    }

    public WriteResponse addTableEntry(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.TableEntry tableEntry) {
        checkInit();
        WriteResponse response;
        WriteRequest request = buildWriteRequest(Convert2ProtoEntry(tableEntry), Update.Type.INSERT);
        response = p4RuntimeClient.write(request);
        return response;
    }

    public WriteResponse modifyTableEntry(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.TableEntry tableEntry) {
        checkInit();
        WriteResponse response;
        WriteRequest request = buildWriteRequest(Convert2ProtoEntry(tableEntry), Update.Type.MODIFY);
        response = p4RuntimeClient.write(request);
        return response;
    }

    public WriteResponse deleteTableEntry(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.TableEntryKey tableEntryKey) {
        checkInit();
        WriteResponse response;
        WriteRequest request = buildWriteRequest(Convert2ProtoEntry(tableEntryKey), Update.Type.DELETE);
        response = p4RuntimeClient.write(request);
        return response;
    }

    public List<String> readTableEntry(String tableName) {
        checkInit();
        ReadRequest.Builder request = ReadRequest.newBuilder();
        Entity.Builder entityBuilder = Entity.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder entryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        entryBuilder.setTableId(getTableId(tableName));
        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        request.setDeviceId(deviceId);

        Iterator<ReadResponse> responses = p4RuntimeClient.read(request.build());
        List<java.lang.String> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                String str = Convert2JsonEntry(entity.getTableEntry());
                result.add(str);
            });
            if (isCompleted) break;
        }
        return result;
    }

    public void transmitPacket(byte[] payload) {
        checkInit();
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        PacketOut.Builder packetOutBuilder = PacketOut.newBuilder();
        packetOutBuilder.setPayload(ByteString.copyFrom(payload));
        requestBuilder.setPacket(packetOutBuilder);
        p4RuntimeClient.transmitPacket(requestBuilder.build());
    }

    private WriteRequest buildWriteRequest(org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry,
                                           org.opendaylight.p4plugin.p4runtime.proto.Update.Type type) {
        WriteRequest.Builder requestBuilder = WriteRequest.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.Update.Builder updateBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.Update.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.Entity.Builder entityBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.Entity.newBuilder();

        entityBuilder.setTableEntry(entry);
        updateBuilder.setType(type);
        updateBuilder.setEntity(entityBuilder);
        requestBuilder.setDeviceId(deviceId);
        requestBuilder.addUpdates(updateBuilder);
        return requestBuilder.build();
    }

    private org.opendaylight.p4plugin.p4runtime.proto.TableEntry Convert2ProtoEntry(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.TableEntry tableEntry) {
        String tableName = tableEntry.getTableName();
        int tableId = getTableId(tableName);
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder tableEntryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.Field> fields =
                tableEntry.getField();

        if (fields != null) {
            fields.forEach(field -> tableEntryBuilder.addMatch(buildFieldMatch(field, tableName)));
        }

        org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.table.entry.ActionType actionType =
                tableEntry.getActionType();
        org.opendaylight.p4plugin.p4runtime.proto.TableAction tableAction = buildTableAction(actionType);
        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.setAction(tableAction);
        return tableEntryBuilder.build();
    }

    private org.opendaylight.p4plugin.p4runtime.proto.TableEntry Convert2ProtoEntry(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.TableEntryKey tableEntryKey){
        String tableName = tableEntryKey.getTableName();
        int tableId = getTableId(tableName);
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder tableEntryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.Field> fields =
                tableEntryKey.getField();

        if (fields != null) {
            fields.forEach(field -> tableEntryBuilder.addMatch(buildFieldMatch(field, tableName)));
        }

        tableEntryBuilder.setTableId(tableId);
        return tableEntryBuilder.build();
    }

    public String Convert2JsonEntry(org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry) {
        String result;
        try {
            result = JsonFormat.printer().print(entry);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            throw e;
        }
        return result;
    }

    private org.opendaylight.p4plugin.p4runtime.proto.TableAction buildTableAction(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.table.entry.ActionType actionType) {

        if (actionType instanceof org.opendaylight.yang.gen.v1.urn
                .opendaylight.p4plugin.p4runtime.rev170808.table.entry.action.type.DirectAction) {
            return buildDirectTableAction((org.opendaylight.yang.gen.v1.urn
                    .opendaylight.p4plugin.p4runtime.rev170808.table.entry.action.type.DirectAction)actionType);
        } else {
            throw new IllegalArgumentException("Invalid action type");
        }
    }

    private org.opendaylight.p4plugin.p4runtime.proto.TableAction buildDirectTableAction(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.table.entry.action.type.DirectAction directAction) {
        org.opendaylight.p4plugin.p4runtime.proto.TableAction.Builder tableActionBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableAction.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.action.ActionParam> params =
                directAction.getActionParam();
        String actionName = directAction.getActionName();
        actionBuilder.setActionId(getActionId(actionName));

        if (params != null) {
            params.forEach(p -> {
                org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder();
                String paramName = p.getParamName();
                byte[] paramValue = parseTypeValue(p.getParamValue());

                int paramId = getParamId(actionName, paramName);
                int paramWidth = getParamWidth(actionName, paramName);

                if (paramValue != null) {
                    paramBuilder.setParamId(paramId);
                    paramBuilder.setValue(ByteString.copyFrom(paramValue, 0, paramWidth));
                }

                actionBuilder.addParams(paramBuilder);
            });
        }

        return tableActionBuilder.setAction(actionBuilder).build();
    }
    private org.opendaylight.p4plugin.p4runtime.proto.FieldMatch buildFieldMatch(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.Field fields,
            String tableName) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.field.MatchType matchType =
                fields.getMatchType();
        String fieldName = fields.getFieldName();

        if (matchType instanceof org.opendaylight.yang.gen.v1.urn
                .opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.Exact) {
            return buildExactMatchField((org.opendaylight.yang.gen.v1.urn
                    .opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.Exact)matchType,
                    tableName, fieldName);
        } else if (matchType instanceof org.opendaylight.yang.gen.v1.urn
                .opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.Lpm) {
            return buildLpmMatchField((org.opendaylight.yang.gen.v1.urn
                    .opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.Lpm)matchType,
                    tableName, fieldName);
        } else if (matchType instanceof org.opendaylight.yang.gen.v1.urn
                .opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.Ternary) {
            return buildTernaryMatchField((org.opendaylight.yang.gen.v1.urn
                    .opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.Ternary)matchType,
                    tableName, fieldName);
        } else {
            throw new IllegalArgumentException("Invalid match type %s");
        }
    }

    private org.opendaylight.p4plugin.p4runtime.proto.FieldMatch buildExactMatchField(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.Exact exact,
            String tableName, String fieldName) {
        org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Builder fieldMatchBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Exact.Builder exactBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Exact.newBuilder();

        Integer matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = getMatchFieldId(tableName, fieldName);
        byte[] exactValue = parseTypeValue(exact.getExactValue());
        exactBuilder.setValue(ByteString.copyFrom(exactValue, 0, matchFieldWidth));
        fieldMatchBuilder.setExact(exactBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private org.opendaylight.p4plugin.p4runtime.proto.FieldMatch buildLpmMatchField(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.Lpm lpm,
            String tableName, String fieldName) {
        org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Builder fieldMatchBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.LPM.Builder lpmBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.LPM.newBuilder();

        Integer matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = getMatchFieldId(tableName, fieldName);

        byte[] lpmValue = parseTypeValue(lpm.getLpmValue());
        Long prefixLen = lpm.getPrefixLen();
        lpmBuilder.setValue(ByteString.copyFrom(lpmValue, 0, matchFieldWidth));
        lpmBuilder.setPrefixLen(prefixLen.intValue());
        fieldMatchBuilder.setLpm(lpmBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private org.opendaylight.p4plugin.p4runtime.proto.FieldMatch buildTernaryMatchField(
            org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.Ternary ternary,
            String tableName, String fieldName) {
        org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Builder fieldMatchBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Ternary.Builder ternaryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Ternary.newBuilder();

        byte[] ternaryValue = parseTypeValue(ternary.getTernaryValue());
        byte[] mask = parseTypeValue(ternary.getMask());
        Integer matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        Integer matchFieldId = getMatchFieldId(tableName, fieldName);
        ternaryBuilder.setValue(ByteString.copyFrom(ternaryValue, 0, matchFieldWidth));
        ternaryBuilder.setMask(ByteString.copyFrom(mask, 0, matchFieldWidth));
        fieldMatchBuilder.setTernary(ternaryBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private Long deviceId_;
        private String nodeId_;
        private String ip_;
        private Integer port_;
        private String deviceConfigFile_;
        private String p4InfoFile_;

        public Builder setIp(String ip) {
            if (ip.matches("\\d+.\\d+.\\d+.\\d+")) {
                this.ip_ = ip;
                return this;
            }
            throw new IllegalArgumentException("Invalid ipv4 address.");
        }

        public Builder setPort(Integer port) {
            this.port_ = port;
            return this;
        }

        public Builder setNodeId(String nodeId) {
            this.nodeId_ = nodeId;
            return this;
        }

        public Builder setDeviceConfigFile(String deviceConfigFile) {
            this.deviceConfigFile_ = deviceConfigFile;
            return this;
        }

        public Builder setP4InfoFile(String p4InfoFile) {
            this.p4InfoFile_ = p4InfoFile;
            return this;
        }

        public Builder setDeviceId(Long deviceId) {
            this.deviceId_ = deviceId;
            return this;
        }

        private P4Info parseP4Info(String file) throws IOException {
            if (file != null) {
                Reader reader = null;
                P4Info.Builder info = P4Info.newBuilder();
                try {
                    reader = new FileReader(file);
                    TextFormat.merge(reader, info);
                    return info.build();
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            }
            return null;
        }

        private ByteString parseDeviceConfig(String file) throws IOException {
            if (file != null) {
                InputStream input = new FileInputStream(new File(file));
                return ByteString.readFrom(input);
            }
            return null;
        }

        public Device build() throws IOException {
            P4Info p4Info;
            ByteString deviceConfig;
            p4Info = parseP4Info(p4InfoFile_);
            deviceConfig = parseDeviceConfig(deviceConfigFile_);
            P4RuntimeClient p4RuntimeClient = new P4RuntimeClient(ip_, port_, deviceId_, nodeId_);
            return new Device(nodeId_, deviceId_, p4RuntimeClient, p4Info, deviceConfig);
        }
    }

    @Override
    public String toString() {
        return String.format("[%s]", nodeId);
    }

    public void openStreamChannel() {
        if (p4RuntimeClient != null) {
            p4RuntimeClient.openStreamChannel();
        }
    }

    public void closeStreamChannel() {
        if (p4RuntimeClient != null) {
            p4RuntimeClient.closeStreamChannel();
        }
    }

    public String getNodeId() {
        return  nodeId;
    }

    public String getIp() {
        if (p4RuntimeClient != null) {
            return p4RuntimeClient.getIp();
        } else {
            throw new RuntimeException("p4RuntimeClient = null.");
        }
    }

    public Integer getPort() {
        if (p4RuntimeClient != null) {
            return p4RuntimeClient.getPort();
        } else {
            throw new RuntimeException("p4RuntimeClient = null.");
        }
    }

    public Long getDeviceId() {
        return deviceId;
    }

    private void checkInit() {
        if (!isConfigured) {
            throw new RuntimeException("Device pipeline isn't initialized.");
        }
    }

    private byte[] parseTypeValue(TypedValue typedValue) {
        String str_val = typedValue.getString();
        Short uint8_val = typedValue.getUint8();
        Integer uint16_val = typedValue.getUint16();
        Long uint32_val = typedValue.getUint32();
        BigInteger uint64_val = typedValue.getUint64();
        byte[] binary_val = typedValue.getBinary();

        if (str_val != null) {
            return str2ByteArray(str_val);
        } else if (uint8_val != null) {
            return ByteBuffer.allocate(1).put(uint8_val.byteValue()).array();
        } else if (uint16_val != null) {
            return ByteBuffer.allocate(2).putShort(uint16_val.shortValue()).array();
        } else if (uint32_val != null) {
            return ByteBuffer.allocate(4).putInt(uint32_val.intValue()).array();
        } else if (uint64_val != null) {
            return uint64_val.toByteArray();
        } else if (binary_val != null) {
            return binary_val;
        } else {
            throw new IllegalArgumentException("Invalid value.");
        }
    }

    /**
     * regular ipv4 address: (1~255).(0~255).(0~255).(0~255);
     * mac address,e.g. aa:bb:cc:dd:ee:ff,1:2:3:4:5:6;
     * Integer, e.g. 10,300, decimal;
     */
    private byte[] str2ByteArray(String str) {
        String[] strArray = null;
        byte[] byteArray = null;

        if (str.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
            strArray = str.split("\\.");
            byteArray = new byte[strArray.length];
            for (int i = 0; i < strArray.length; i++) {
                byteArray[i] = (byte) Integer.parseInt(strArray[i]);
            }
        } else if (str.matches("([0-9a-fA-F]{1,2}:){5}[0-9a-fA-F]{1,2}")) {
            strArray = str.split(":");
            byteArray = new byte[strArray.length];
            for (int i = 0; i < strArray.length; i++) {
                byteArray[i] = (byte) Integer.parseInt(strArray[i], 16);
            }
        } else {
            int value = Integer.parseInt(str);
            byteArray = ByteBuffer.allocate(4).putInt(value).array();
        }
        return byteArray;
    }
}
