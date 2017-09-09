/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.MatchField;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4info.proto.Table;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.TableEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.table.entry.input.MatchFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.table.entry.input.Params;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.table.entry.input.match.fields.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.table.entry.input.match.fields.match.type.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT;

public class P4Device {
    private static final Logger LOG = LoggerFactory.getLogger(P4Device.class);
    private GrpcChannel channel;
    private P4Info runtimeInfo;
    private ByteString deviceConfig;
    private Long deviceId;
    private State state = State.Unknown;
    private P4Device() {}

    public int getTableId(String tableName) {
        Optional<Table> container = runtimeInfo.getTablesList().stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        int result = 0;
        if (container.isPresent()) {
            result = container.get().getPreamble().getId();
        }
        return result;
    }

    public String  getTableName(int tableId) {
        Optional<Table> container = runtimeInfo.getTablesList()
                .stream()
                .filter(table->table.getPreamble().getId() == tableId)
                .findFirst();
        String result = null;
        if (container.isPresent()) {
            result = container.get().getPreamble().getName();
        }
        return result;
    }

    public int getMatchFieldId(String tableName, String matchFieldName) {
        Optional<Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        int result = 0;
        if (tableContainer.isPresent()) {
            Optional<MatchField> matchFieldContainer = tableContainer.get()
                    .getMatchFieldsList()
                    .stream()
                    .filter(matchField -> matchField.getName().equals(matchFieldName))
                    .findFirst();
            if (matchFieldContainer.isPresent()) {
                result = matchFieldContainer.get().getId();
            }
        }
        return result;
    }

    public String getMatchFieldName(int tableId, int matchFieldId) {
        Optional<Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table->table.getPreamble().getId() == tableId)
                .findFirst();
        String result = null;
        if (tableContainer.isPresent()) {
            Optional<MatchField> matchFieldContainer = tableContainer.get()
                    .getMatchFieldsList()
                    .stream()
                    .filter(matchField -> matchField.getId() == (matchFieldId))
                    .findFirst();
            if (matchFieldContainer.isPresent()) {
                result = matchFieldContainer.get().getName();
            }
        }
        return result;
    }

    public int getMatchFieldWidth(String tableName, String matchFieldName) {
        if (runtimeInfo == null) {
            throw new NullPointerException("P4Device runtime info is null.");
        }
        Optional<Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        int result = 0;
        if (tableContainer.isPresent()) {
            Optional<MatchField> matchFieldContainer = tableContainer.get()
                    .getMatchFieldsList()
                    .stream()
                    .filter(matchField -> matchField.getName().equals(matchFieldName))
                    .findFirst();
            if (matchFieldContainer.isPresent()) {
                result = (matchFieldContainer.get().getBitwidth() + 7) / 8;
            }
        }
        return result;
    }

    public int getActionId(String actionName) {
        if (runtimeInfo == null) {
            throw new NullPointerException("P4Device runtime info is null.");
        }
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            result = actionContainer.get().getPreamble().getId();
        }
        return result;
    }

    public String getActionName(int actionId) {
        if (runtimeInfo == null) throw new NullPointerException("P4Device runtime info is null.");
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        String result = null;
        if (actionContainer.isPresent()) {
            result = actionContainer.get().getPreamble().getName();
        }
        return result;
    }

    public int getParamId(String actionName, String paramName) {
        if (runtimeInfo == null) throw new NullPointerException("P4Device runtime info is null.");
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer = actionContainer.get()
                    .getParamsList()
                    .stream()
                    .filter(param -> param.getName().equals(paramName))
                    .findFirst();
            if (paramContainer.isPresent()) {
                result = paramContainer.get().getId();
            }
        }
        return result;
    }

    public String getParamName(int actionId, int paramId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        String result = null;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer = actionContainer.get()
                    .getParamsList()
                    .stream()
                    .filter(param -> param.getId() == paramId)
                    .findFirst();
            if (paramContainer.isPresent()) {
                result = paramContainer.get().getName();
            }
        }
        return result;
    }

    public int getParamWidth(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer = actionContainer.get()
                    .getParamsList()
                    .stream()
                    .filter(param -> param.getName().equals(paramName))
                    .findFirst();
            if (paramContainer.isPresent()) {
                result = (paramContainer.get().getBitwidth() + 7) / 8;
            }
        }
        return result;
    }

    private org.opendaylight.p4plugin.p4runtime.proto.Action.Builder newActionBuilder() {
        return org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();
    }

    private org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder newParamBuilder() {
        return org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder();
    }

    /**
     * Input table entry serialize to protobuf message.
     */
    public TableEntry toMessage(TableEntryInput input) {
        if (runtimeInfo == null) {
            throw new NullPointerException("Runtime info is null.");
        }

        String tableName = input.getTable();
        List<MatchFields> fields = input.getMatchFields();
        String actionName = input.getAction();
        List<Params> params = input.getParams();

        int tableId = getTableId(tableName);
        int actionId = getActionId(actionName);

        org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder = newActionBuilder();
        actionBuilder.setActionId(actionId);

        /* Must support no param. */
        if (params != null) {
            for (Params k : params) {
                org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder = newParamBuilder();
                int paramId = getParamId(actionName, k.getName());
                paramBuilder.setParamId(paramId);
                int paramWidth = getParamWidth(actionName, k.getName());
                byte[] valueArr = Utils.strToByteArray(k.getValue(), paramWidth);
                ByteString valueByteStr = ByteString.copyFrom(valueArr);
                paramBuilder.setValue(valueByteStr);
                actionBuilder.addParams(paramBuilder.build());
            }
        }

        TableAction.Builder tableActionBuilder = TableAction.newBuilder();
        tableActionBuilder.setAction(actionBuilder.build());
        TableEntry.Builder tableEntryBuilder = TableEntry.newBuilder();
        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.setAction(tableActionBuilder.build());

        /* Must support no match field, for example set default action. */
        if (fields != null) {
            for (MatchFields f : fields) {
                FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
                int matchFieldId = getMatchFieldId(tableName, f.getField());
                int matchFieldWidth = getMatchFieldWidth(tableName, f.getField());
                fieldMatchBuilder.setFieldId(matchFieldId);
                MatchType matchType = f.getMatchType();

                if ((matchType instanceof EXACT)) {
                    FieldMatch.Exact.Builder exactBuilder = FieldMatch.Exact.newBuilder();
                    EXACT exact = (EXACT)matchType;
                    String value = exact.getExactValue();
                    exactBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(value, matchFieldWidth)));
                    fieldMatchBuilder.setExact(exactBuilder);
                } else if (matchType instanceof LPM) {
                    FieldMatch.LPM.Builder lpmBuilder = FieldMatch.LPM.newBuilder();
                    LPM lpm = (LPM)matchType;
                    String value = lpm.getLpmValue();
                    int prefixLen = lpm.getLpmPrefixLen();
                    // Value must match ipv4 address
                    if (value.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                                    + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                                    + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
                        lpmBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(value, matchFieldWidth)));
                        lpmBuilder.setPrefixLen(prefixLen);
                        fieldMatchBuilder.setLpm(lpmBuilder);
                    }
                } else if (matchType instanceof TERNARY) {
                    FieldMatch.Ternary.Builder ternaryBuilder = FieldMatch.Ternary.newBuilder();
                    TERNARY ternary = (TERNARY)matchType;
                    String mask = ternary.getTernaryMask();
                    String value = ternary.getTernaryValue();

                    if (value.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                                    + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                                    + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
                        ternaryBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(value, matchFieldWidth)));
                    }

                    if (mask.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                                   + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                                   + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
                        ternaryBuilder.setMask(ByteString.copyFrom(Utils.strToByteArray(mask, 4)));
                    } else if (mask.matches("([1-9]|[1-2][0-9]|3[0-2])")) {
                        StringBuffer buffer =new StringBuffer(32);
                        for(int i = 0; i < 32; i++) {
                            if(i < Integer.parseInt(mask)) {
                                buffer.append('1');
                            }
                            else {
                                buffer.append('0');
                            }
                        }

                        String[] resultStr = new String[4];
                        byte[] resultByte = new byte[4];
                        for(int i = 0; i < resultStr.length; i++) {
                            resultStr[i] = buffer.substring(i * 8, i * 8 + 8);
                            for (int m = resultStr[i].length() - 1, n = 0; m >= 0; m--, n++) {
                                resultByte[i] += Byte.parseByte(resultStr[i].charAt(i)+"") * Math.pow(2, n);
                            }
                        }
                        ternaryBuilder.setMask(ByteString.copyFrom(resultByte));
                        fieldMatchBuilder.setTernary(ternaryBuilder);
                    }
                } else if (matchType instanceof RANGE) {
                    //TODO
                } else if (matchType instanceof VALID) {
                    FieldMatch.Valid.Builder validBuilder = FieldMatch.Valid.newBuilder();
                    boolean value = ((VALID)matchType).isValid();
                    validBuilder.setValue(value);
                    fieldMatchBuilder.setValid(validBuilder);
                } else {
                    throw new IllegalArgumentException("Match type illegal.");
                }
                tableEntryBuilder.addMatch(fieldMatchBuilder.build());
            }
        }
        return tableEntryBuilder.build();
    }

    /**
     * Table entry object to human-readable string.
     */
    public String tableEntryToString(TableEntry entry) {
        if (runtimeInfo == null) {
            throw new NullPointerException("Runtime info is null.");
        }
        org.opendaylight.p4plugin.p4runtime.proto.Action action = entry.getAction().getAction();
        int tableId = entry.getTableId();
        int actionId = action.getActionId();
        List<org.opendaylight.p4plugin.p4runtime.proto.Action.Param> paramList = action.getParamsList();
        String tableName = getTableName(tableId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(tableName + " ");

        List<FieldMatch> fieldList = entry.getMatchList();
        fieldList.forEach(field -> {
            int fieldId = field.getFieldId();
            switch (field.getFieldMatchTypeCase()) {
                case EXACT: {
                    FieldMatch.Exact exact = field.getExact();
                    buffer.append(String.format("%s = ", getMatchFieldName(tableId, fieldId)));
                    buffer.append(Utils.byteArrayToStr(exact.getValue().toByteArray()));
                    buffer.append(":exact");
                    break;
                }

                case LPM: {
                    FieldMatch.LPM lpm = field.getLpm();
                    buffer.append(String.format("%s = ", getMatchFieldName(tableId, fieldId)));
                    buffer.append(Utils.byteArrayToStr(lpm.getValue().toByteArray()));
                    buffer.append("/");
                    buffer.append(String.valueOf(lpm.getPrefixLen()));
                    buffer.append(":lpm");
                    break;
                }

                case TERNARY: {
                    FieldMatch.Ternary ternary = field.getTernary();
                    buffer.append(String.format("%s = ", getMatchFieldName(tableId, fieldId)));
                    buffer.append(Utils.byteArrayToStr(ternary.getValue().toByteArray()));
                    buffer.append("/");
                    buffer.append(String.valueOf(ternary.getMask()));//TODO
                    break;
                }
                //TODO
                case RANGE:
                    break;
                case VALID:
                    break;
                default:
                    break;
            }
        });

        buffer.append(" " + getActionName(actionId) + "(");
        paramList.forEach(param -> {
            int paramId = param.getParamId();
            buffer.append(String.format("%s", getParamName(actionId, paramId)));
            buffer.append(" = ");
            buffer.append(String.format("%s", Utils.byteArrayToStr(param.getValue().toByteArray())));
        });
        buffer.append(")");
        return new String(buffer);
    }

    public void setDeviceState(State state) {
        this.state = state;
    }

    public State getDeviceState() {
        return state;
    }

    public P4Info getRuntimeInfo() {
        return runtimeInfo;
    }

    public void setRuntimeInfo(String file) throws IOException {
        runtimeInfo  = Utils.parseRuntimeInfo(file);
    }

    public void setRuntimeInfo(P4Info runtimeInfo) {
        this.runtimeInfo = runtimeInfo;
    }

    public ByteString getDeviceConfig() {
        return this.deviceConfig;
    }

    public void setDeviceConfig(String file) throws IOException {
        deviceConfig = Utils.parseDeviceConfigInfo(file);
    }

    public GrpcChannel getGrpcChannel() {
        return this.channel;
    }

    public void sendMasterArbitration() {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        MasterArbitrationUpdate.Builder masterArbitrationBuilder = MasterArbitrationUpdate.newBuilder();
        masterArbitrationBuilder.setDeviceId(deviceId);
        requestBuilder.setArbitration(masterArbitrationBuilder);
        channel.getRequestStreamObserver().onNext(requestBuilder.build());
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig() {
        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();
        configBuilder.setDeviceId(deviceId);

        if(this.runtimeInfo != null) {
            configBuilder.setP4Info(this.runtimeInfo);
        }

        if(this.deviceConfig != null) {
            p4DeviceConfigBuilder.setDeviceData(this.deviceConfig);
        }

        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                                                        .setAction(VERIFY_AND_COMMIT)
                                                        .addConfigs(configBuilder.build())
                                                        .build();
        SetForwardingPipelineConfigResponse response;

        try {
            /* response is empty now */
            response = channel.getBlockingStub().setForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Set pipeline config RPC failed: {}", e.getStatus());
            e.printStackTrace();
        }
        return null;
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig() {
        GetForwardingPipelineConfigRequest request = GetForwardingPipelineConfigRequest.newBuilder()
                                                        .addDeviceIds(deviceId)
                                                        .build();
        GetForwardingPipelineConfigResponse response;
        try {
            /* response is empty now */
            response = channel.getBlockingStub().getForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Get pipeline config RPC failed: {}", e.getStatus());
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  write RPC, unary call;
     */
    public WriteResponse write(WriteRequest request) {
        @Nullable WriteResponse response;
        try {
            response = channel.getBlockingStub().write(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Write RPC failed: {}", e.getStatus());
        }
        return null;
    }

    /**
     *  read RPC, server stream;
     */
    public Iterator<ReadResponse> read(ReadRequest request) {
        @Nullable Iterator<ReadResponse> responses;
        try {
            responses = channel.getBlockingStub().read(request);
            return responses;
        } catch (StatusRuntimeException e) {
            LOG.info("Read RPC failed: {}", e.getStatus());
        }
        return null;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private GrpcChannel channel_;
        private P4Info runtimeInfo_;
        private ByteString deviceConfig_;
        private Long deviceId_;

        public Builder setChannel(GrpcChannel channel) {
            this.channel_ = channel;
            return this;
        }

        public Builder setRuntimeInfo(String file) throws IOException {
            runtimeInfo_ = Utils.parseRuntimeInfo(file);
            return this;
        }

        public Builder setDeviceConfig(String file) throws IOException {
            deviceConfig_ = Utils.parseDeviceConfigInfo(file);
            return this;
        }

        public Builder setDeviceId(Long deviceId) {
            this.deviceId_ = deviceId;
            return this;
        }

        public P4Device build() {
            P4Device target = new P4Device();
            target.deviceConfig = deviceConfig_;
            target.runtimeInfo = runtimeInfo_;
            target.channel = channel_;
            target.deviceId = deviceId_;
            return target;
        }
    }

    public enum State {
        Unknown,
        Connected,
        Configured,
    }
}

