/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.device;

import com.google.protobuf.ByteString;
import io.grpc.StatusRuntimeException;
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeStub;
import org.opendaylight.p4plugin.core.impl.utils.Utils;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.p4plugin.p4runtime.proto.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.ActionProfileGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.ActionProfileMember;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.TableEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.action.ActionParam;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.action.action.param.ParamValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.action.action.param.param.value.type.PARAMVALUETYPEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.action.action.param.param.value.type.PARAMVALUETYPESTRING;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.Field;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.EXACT;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.LPM;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.RANGE;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.TERNARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.exact.ExactValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.exact.exact.value.type.EXACTVALUETYPEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.exact.exact.value.type.EXACTVALUETYPESTRING;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.lpm.LpmValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.lpm.lpm.value.type.LPMVALUETYPEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.lpm.lpm.value.type.LPMVALUETYPESTRING;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.range.RangeValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.range.range.value.type.RANGEVALUETYPEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.range.range.value.type.RANGEVALUETYPESTRING;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.ternary.TernaryValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.ternary.ternary.value.type.TERNARYVALUEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.ternary.ternary.value.type.TERNARYVALUETYPESTRING;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.ActionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.action.type.ACTIONPROFILEGROUP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.action.type.ACTIONPROFILEMEMBER;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.action.type.DIRECTACTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * The relationship between P4Device, P4RuntimeStub, P4RuntimeChannel and Stream channel
 * is shown below.
 *    ---------------------------                      ---------------------------
 *  |           Device A        |                    |          Device B         |
 *  |                           |                    |                           |
 *  |  -----------------------  |                    |  -----------------------  |
 *  | |          Stub         | |                    | |          Stub         | |
 *  | |                       | |                    | |                       | |
 *  | |  -------------------  | |                    | |  -------------------  | |
 *  | | |  Stream Channel A | | |                    | | |  Stream Channel B | | |
 *  | | |                   | | |                    | | |                   | | |
 *  | |  -------------------  | |                    | |  -------------------  | |
 *  |  -----------------------  |                    |  -----------------------  |
 *   ---------------------------                      ---------------------------
 *               / \                                               / \
 *                |                                                 |
 *                 -------------------      ------------------------
 *                                    |    |
 *                                   \ /  \ /
 *   ----------------------------------------------------------------------------
 *  |                             gRPC channel                                   |
 *   ----------------------------------------------------------------------------
 *                     |                                     |
 *                     |     ---------------------------     |
 *                     |    |       Stream A Data       |    |
 *                     |     ---------------------------     |
 *                     |     ---------------------------     |
 *                     |    |       Stream B Data       |    |
 *                     |     ---------------------------     |
 *                     |     ---------------------------     |
 *                     |    |       Stream B Data       |    |
 *                     |     ---------------------------     |
 *                     |     ---------------------------     |
 *                     |    |       Stream A Data       |    |
 *                     |     ---------------------------     |
 *                     |                                     |
 *   -----------------------------------------------------------------------------
 *  |                                 Switch                                      |
 *  |   -----------------------------------------------------------------------   |
 *  |  |                            gRPC server                                |  |
 *  |   -----------------------------------------------------------------------   |
 *  |                / \                                       / \                |
 *  |                 |                                         |                 |
 *  |                 |                                         |                 |
 *  |                \ /                                       \ /                |
 *  |   ----------------------------               ----------------------------   |
 *  |  |                            |             |                            |  |
 *  |  |           Device A         |             |           Device B         |  |
 *  |  |                            |             |                            |  |
 *  |   ----------------------------               ----------------------------   |
 *   -----------------------------------------------------------------------------
 */
public class P4Device  {
    private static final Logger LOG = LoggerFactory.getLogger(P4Device.class);
    private P4RuntimeStub stub;
    private P4Info runtimeInfo;
    private ByteString deviceConfig;
    private String ip;
    private Integer port;
    private Long deviceId;
    private String nodeId;
    private State state = State.Unknown;
    private P4Device() {}

    public int getTableId(String tableName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> container =
                runtimeInfo.getTablesList().stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        int result = 0;
        if (container.isPresent()) {
            result = container.get().getPreamble().getId();
        }
        return result;
    }

    private String getTableName(int tableId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> container =
                runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();
        String result = null;
        if (container.isPresent()) {
            result = container.get().getPreamble().getName();
        }
        return result;
    }

    private int getMatchFieldId(String tableName, String matchFieldName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer =
                runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        int result = 0;
        if (tableContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer =
                    tableContainer.get()
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

    private String getMatchFieldName(int tableId, int matchFieldId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer =
                runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();
        String result = null;
        if (tableContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer =
                    tableContainer.get()
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

    private int getMatchFieldWidth(String tableName, String matchFieldName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer =
                runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        int result = 0;
        if (tableContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer =
                    tableContainer.get()
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

    private int getActionId(String actionName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            result = actionContainer.get().getPreamble().getId();
        }
        return result;
    }

    private String getActionName(int actionId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        String result = null;
        if (actionContainer.isPresent()) {
            result = actionContainer.get().getPreamble().getName();
        }
        return result;
    }

    private int getParamId(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                    actionContainer.get()
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

    private String getParamName(int actionId, int paramId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        String result = null;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                    actionContainer.get()
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

    private int getParamWidth(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer =
                runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        int result = 0;
        if (actionContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                    actionContainer.get()
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

    public int getActionProfileId(String actionProfileName) {
        int result = 0;
        Optional<org.opendaylight.p4plugin.p4info.proto.ActionProfile> actionProfileContainer =
                runtimeInfo.getActionProfilesList().stream()
                .filter(actionProfile -> actionProfile.getPreamble().getName().equals(actionProfileName))
                .findFirst();
        if (actionProfileContainer.isPresent()) {
            result = actionProfileContainer.get().getPreamble().getId();
        }
        return result;
    }

    private String getActionProfileName(Integer actionProfileId) {
        String result = null;
        Optional<org.opendaylight.p4plugin.p4info.proto.ActionProfile> actionProfileContainer =
                runtimeInfo.getActionProfilesList().stream()
                .filter(actionProfile -> actionProfile.getPreamble().getId() == actionProfileId)
                .findFirst();
        if (actionProfileContainer.isPresent()) {
            result = actionProfileContainer.get().getPreamble().getName();
        }
        return result;
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

    public State getDeviceState() {
        return state;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setDeviceState(State state) {
        this.state = state;
    }

    public boolean isConfigured() {
        return runtimeInfo != null
            && deviceConfig != null
            && state == State.Configured;
    }

    public boolean connectToDevice() {
        return stub.connect();
    }

    public String getDescription() {
        return nodeId + ":" + deviceId + ":" + ip + ":" + port + ":" +state;
    }

    public void shutdown() {
        stub.shutdown();
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig() {
        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();
        if (deviceConfig != null) {
            p4DeviceConfigBuilder.setDeviceData(deviceConfig);
        }
        if (runtimeInfo != null) {
            configBuilder.setP4Info(runtimeInfo);
        }
        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        configBuilder.setDeviceId(deviceId);
        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                .setAction(SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT)
                .addConfigs(configBuilder)
                .build();
        SetForwardingPipelineConfigResponse response;
        try {
            /* response is empty now */
            response = stub.setPipelineConfig(request);
            setDeviceState(State.Configured);
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
            response = stub.getPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Get pipeline config RPC failed: status = {}, reason = {}.",
                    e.getStatus(), e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public WriteResponse write(WriteRequest request) {
        WriteResponse response;
        try {
            response = stub.write(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Write RPC failed: status = {}, reason = {}.", e.getStatus(), e.getMessage());
        }
        return null;
    }

    public Iterator<ReadResponse> read(ReadRequest request) {
        Iterator<ReadResponse> responses;
        try {
            responses = stub.read(request);
            return responses;
        } catch (StatusRuntimeException e) {
            LOG.info("Read RPC failed: status = {}, reason = {}.", e.getStatus(), e.getMessage());
        }
        return null;
    }

    public void sendMasterArbitration() {
        stub.sendMasterArbitration();
    }

    public void transmitPacket(byte[] payload) {
        stub.transmitPacket(payload);
    }

    private TableAction buildTableAction(ActionType actionType) {
        AbstractActionParser parser;
        if (actionType instanceof DIRECTACTION) {
            parser = new DirectActionParser((DIRECTACTION)actionType);
        } else if (actionType instanceof ACTIONPROFILEMEMBER) {
            parser = new ActionProfileMemberParser((ACTIONPROFILEMEMBER) actionType);
        } else if (actionType instanceof ACTIONPROFILEGROUP) {
            parser = new ActionProfileGroupParser((ACTIONPROFILEGROUP) actionType);
        } else {
            throw new IllegalArgumentException("Invalid action type = " + actionType);
        }
        return parser.parse();
    }

    private FieldMatch buildFieldMatch(Field field, String tableName) {
        AbstractMatchFieldParser parser;
        MatchType matchType = field.getMatchType();
        String fieldName = field.getFieldName();
        if (matchType instanceof EXACT) {
            parser = new ExactMatchParser((EXACT) matchType, tableName, fieldName);
        } else if (matchType instanceof LPM) {
            parser = new LpmMatchParser((LPM) matchType, tableName, fieldName);
        } else if (matchType instanceof TERNARY) {
            parser = new TernaryMatchParser((TERNARY) matchType, tableName, fieldName);
        } else if (matchType instanceof RANGE) {
            parser = new RangeMatchParser((RANGE) matchType, tableName, fieldName);
        } else {
            throw new IllegalArgumentException("Invalid match type = " + matchType);
        }
        return parser.parse();
    }

    /**
     * Input table entry serialize to protobuf message. Used for adding and modifying an entry.
     * When this method is called, the device must be configured.
     */
    public org.opendaylight.p4plugin.p4runtime.proto.TableEntry toMessage(TableEntry input) {
        String tableName = input.getTable();
        int tableId = getTableId(tableName);
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder tableEntryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        List<Field> fields = input.getField();
        fields.forEach(field -> {
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch fieldMatch = buildFieldMatch(field, tableName);
            tableEntryBuilder.addMatch(fieldMatch);
        });
        ActionType actionType = input.getActionType();
        org.opendaylight.p4plugin.p4runtime.proto.TableAction tableAction = buildTableAction(actionType);
        tableEntryBuilder.setPriority(input.getPriority());
        tableEntryBuilder.setControllerMetadata(input.getControllerMetadata().longValue());
        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.setAction(tableAction);
        return tableEntryBuilder.build();
    }

    /**
     * Used for deleting table entry, when delete a table entry, only need table name
     * and match fields actually. BTW, this the only way for search a table entry,
     * not support table entry id.
     */
    public org.opendaylight.p4plugin.p4runtime.proto.TableEntry toMessage(EntryKey input) {
        String tableName = input.getTable();
        int tableId = getTableId(tableName);
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder tableEntryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        List<Field> fields = input.getField();
        fields.forEach(field -> {
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch fieldMatch =
                    buildFieldMatch(field, tableName);
            tableEntryBuilder.addMatch(fieldMatch);
        });
        tableEntryBuilder.setTableId(tableId);
        return tableEntryBuilder.build();
    }

    /**
     * Input action profile member serialize to protobuf message, used for adding and
     * modifying a member. When this method is called, the device must be configured.
     */
    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember toMessage(
            ActionProfileMember member) {
        String actionName = member.getActionName();
        Long memberId = member.getMemberId();
        String actionProfile = member.getActionProfile();

        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.Builder memberBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.newBuilder();
        org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();

        actionBuilder.setActionId(getActionId(actionName));
        member.getActionParam().forEach(actionParam -> {
            org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder();
            String paramName = actionParam.getParamName();
            ParamValueType valueType = actionParam.getParamValueType();
            int paramId = getParamId(actionName, paramName);
            int paramWidth = getParamWidth(actionName, paramName);

            if (valueType instanceof PARAMVALUETYPESTRING) {
                String valueStr = ((PARAMVALUETYPESTRING) valueType).getParamStringValue();
                byte[] valueByteArr = Utils.strToByteArray(valueStr, paramWidth);
                paramBuilder.setValue(ByteString.copyFrom(valueByteArr));
            } else if (valueType instanceof PARAMVALUETYPEBINARY) {
                byte[] valueBytes = ((PARAMVALUETYPEBINARY) valueType).getParamBinaryValue();
                paramBuilder.setValue(ByteString.copyFrom(valueBytes, 0, paramWidth));
            } else {
                throw new IllegalArgumentException("Invalid value type.");
            }
            paramBuilder.setParamId(paramId);
            actionBuilder.addParams(paramBuilder);
        });

        memberBuilder.setAction(actionBuilder);
        memberBuilder.setActionProfileId(getActionProfileId(actionProfile));
        memberBuilder.setMemberId(memberId.intValue());
        return memberBuilder.build();
    }

    /**
     * Used for delete one member in action profile.table. When delete a member,
     * only need action profile name and member id.
     */
    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember toMessage(
            MemberKey key) {
        Long memberId = key.getMemberId();
        String actionProfile = key.getActionProfile();
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.Builder memberBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.newBuilder();
        memberBuilder.setActionProfileId(getActionProfileId(actionProfile));
        memberBuilder.setMemberId(memberId.intValue());
        return memberBuilder.build();
    }

    /**
     * Input action profile group serialize to protobuf message, used for add/modify.
     * When this method is called, the device must be configured.
     */
    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup toMessage (
            ActionProfileGroup group) {
        Long groupId = group.getGroupId();
        String actionProfile = group.getActionProfile();
        org.opendaylight.yang.gen.v1.urn
                .opendaylight.p4plugin.core.table.rev170808.ActionProfileGroup.GroupType
                type = group.getGroupType();
        Integer maxSize = group.getMaxSize();

        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Builder groupBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(getActionProfileId(actionProfile));
        groupBuilder.setGroupId(groupId.intValue());
        groupBuilder.setType(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup
                .Type.valueOf(type.toString()));
        groupBuilder.setMaxSize(maxSize);

        group.getGroupMember().forEach(groupMember -> {
            org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Member.Builder builder =
                    org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Member.newBuilder();
            builder.setWatch(groupMember.getWatch().intValue());
            builder.setWeight(groupMember.getWeight().intValue());
            builder.setMemberId(groupMember.getMemberId().intValue());
            groupBuilder.addMembers(builder);
        });
        return groupBuilder.build();
    }

    /**
     * Used for delete one group in action profile. When delete a group,
     * only need action profile name and group id.
     */
    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup toMessage(
            GroupKey key) {
        Long groupId = key.getGroupId();
        String actionProfile = key.getActionProfile();
        org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Builder groupBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.newBuilder();
        groupBuilder.setActionProfileId(getActionProfileId(actionProfile));
        groupBuilder.setGroupId(groupId.intValue());
        return groupBuilder.build();
    }

    /**
     * Table entry object to human-readable string, for read table entry.
     */
    public String toString(org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry) {
        org.opendaylight.p4plugin.p4runtime.proto.TableAction tableAction = entry.getAction();
        int tableId = entry.getTableId();
        String tableName = getTableName(tableId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(tableName).append(" ");

        List<org.opendaylight.p4plugin.p4runtime.proto.FieldMatch> fieldList = entry.getMatchList();
        fieldList.forEach(field -> {
            int fieldId = field.getFieldId();
            switch (field.getFieldMatchTypeCase()) {
                case EXACT: {
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Exact exact = field.getExact();
                    buffer.append(String.format("%s = ", getMatchFieldName(tableId, fieldId)));
                    buffer.append(Utils.byteArrayToStr(exact.getValue().toByteArray()));
                    buffer.append(":exact");
                    break;
                }

                case LPM: {
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.LPM lpm = field.getLpm();
                    buffer.append(String.format("%s = ", getMatchFieldName(tableId, fieldId)));
                    buffer.append(Utils.byteArrayToStr(lpm.getValue().toByteArray()));
                    buffer.append("/");
                    buffer.append(String.valueOf(lpm.getPrefixLen()));
                    buffer.append(":lpm");
                    break;
                }

                case TERNARY: {
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Ternary ternary = field.getTernary();
                    buffer.append(String.format("%s = ", getMatchFieldName(tableId, fieldId)));
                    buffer.append(Utils.byteArrayToStr(ternary.getValue().toByteArray()));
                    buffer.append("/");
                    buffer.append(String.valueOf(ternary.getMask()));//TODO
                    break;
                }
                //TODO
                case RANGE:
                    break;
                default:
                    break;
            }
        });

        switch (tableAction.getTypeCase()) {
            case ACTION: {
                int actionId = tableAction.getAction().getActionId();
                List<org.opendaylight.p4plugin.p4runtime.proto.Action.Param> paramList =
                        tableAction.getAction().getParamsList();
                buffer.append(" ").append(getActionName(actionId)).append("(");
                paramList.forEach(param -> {
                    int paramId = param.getParamId();
                    buffer.append(String.format("%s", getParamName(actionId, paramId)));
                    buffer.append(" = ");
                    buffer.append(String.format("%s", Utils.byteArrayToStr(param.getValue().toByteArray())));
                });
                buffer.append(")");
                break;
            }

            case ACTION_PROFILE_MEMBER_ID: {
                int memberId = entry.getAction().getActionProfileMemberId();
                buffer.append(" member id = ").append(memberId);
                break;
            }

            case ACTION_PROFILE_GROUP_ID: {
                int groupId = entry.getAction().getActionProfileGroupId();
                buffer.append(" group id = ").append(groupId);
                break;
            }

            default:break;
        }
        return new String(buffer);
    }

    /**
     * Action profile member to human-readable string.
     */
    public String toString(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member) {
        int profileId = member.getActionProfileId();
        int memberId = member.getMemberId();
        org.opendaylight.p4plugin.p4runtime.proto.Action action = member.getAction();
        List<org.opendaylight.p4plugin.p4runtime.proto.Action.Param> paramList = action.getParamsList();
        int actionId = action.getActionId();
        String actionProfile = getActionProfileName(profileId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("%s - %d", actionProfile, memberId));
        buffer.append(" ").append(getActionName(actionId)).append("(");
        paramList.forEach(param -> {
            int paramId = param.getParamId();
            buffer.append(String.format("%s", getParamName(actionId, paramId)));
            buffer.append(" = ");
            buffer.append(String.format("%s", Utils.byteArrayToStr(param.getValue().toByteArray())));
        });
        buffer.append(")");
        return new String(buffer);
    }

    /**
     * Action profile group to human-readable string.
     */
    public String toString(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup group) {
        int profileId = group.getActionProfileId();
        int groupId = group.getGroupId();
        String actionProfile = getActionProfileName(profileId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("%s - %d : ", actionProfile, groupId));
        group.getMembersList().forEach(member -> buffer.append(member.getMemberId()).append(" "));
        return new String(buffer);
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
            P4Device device = new P4Device();
            device.deviceConfig = deviceConfig_;
            device.runtimeInfo = runtimeInfo_;
            device.deviceId = deviceId_;
            device.nodeId = nodeId_;
            device.ip = ip_;
            device.port = port_;
            device.stub = new P4RuntimeStub(nodeId_, deviceId_, ip_, port_);
            return device;
        }
    }

    public enum State {
        Unknown,
        Connected,
        Configured,
    }

    /**
     * Abstract action parser, we think there are three types actions for
     * direct action, action profile member and action profile group currently.
     * Each action parser must extends the abstract action parser.
     * @param <T> Action type.
     */
    private abstract class AbstractActionParser<T> {
        protected T action;
        public AbstractActionParser(T action) {
            this.action = action;
        }
        protected abstract TableAction parse();
    }

    private class DirectActionParser extends AbstractActionParser<DIRECTACTION> {
        public DirectActionParser(DIRECTACTION action) {
            super(action);
        }

        @Override
        public TableAction parse() {
            TableAction.Builder tableActionBuilder = TableAction.newBuilder();
            Action.Builder actionBuilder = Action.newBuilder();
            List<ActionParam> params = action.getActionParam();
            String actionName = action.getActionName();
            actionBuilder.setActionId(getActionId(actionName));
            for (ActionParam p : params) {
                Action.Param.Builder paramBuilder = Action.Param.newBuilder();
                String paramName = p.getParamName();
                int paramId = getParamId(actionName, paramName);
                int paramWidth = getParamWidth(actionName, paramName);
                paramBuilder.setParamId(paramId);
                ParamValueType valueType = p.getParamValueType();

                if (valueType instanceof PARAMVALUETYPESTRING) {
                    String valueStr = ((PARAMVALUETYPESTRING) valueType).getParamStringValue();
                    byte[] valueByteArr = Utils.strToByteArray(valueStr, paramWidth);
                    paramBuilder.setValue(ByteString.copyFrom(valueByteArr));
                } else if (valueType instanceof PARAMVALUETYPEBINARY) {
                    byte[] valueBytes = ((PARAMVALUETYPEBINARY) valueType).getParamBinaryValue();
                    paramBuilder.setValue(ByteString.copyFrom(valueBytes, 0, paramWidth));
                } else {
                    throw new IllegalArgumentException("Invalid value type.");
                }

                actionBuilder.addParams(paramBuilder);
            }
            return tableActionBuilder.setAction(actionBuilder).build();
        }
    }

    private class ActionProfileMemberParser extends AbstractActionParser<ACTIONPROFILEMEMBER> {
        public ActionProfileMemberParser(ACTIONPROFILEMEMBER action) {
            super(action);
        }

        @Override
        public TableAction parse() {
            TableAction.Builder builder = TableAction.newBuilder();
            builder.setActionProfileMemberId(action.getMemberId().intValue());
            return builder.build();
        }
    }

    private class ActionProfileGroupParser extends AbstractActionParser<ACTIONPROFILEGROUP> {
        public ActionProfileGroupParser(ACTIONPROFILEGROUP action) {
            super(action);
        }

        @Override
        public TableAction parse() {
            TableAction.Builder builder = TableAction.newBuilder();
            builder.setActionProfileGroupId(action.getGroupId().intValue());
            return builder.build();
        }
    }

    /**
     * Abstract match field parser, according to P4_16, there isn't valid match type.
     * Range match is limited in uin64, a little different from protobuf;
     * @param <T> Field match type.
     */
    private abstract class AbstractMatchFieldParser<T> {
        protected T matchType;
        protected Integer matchFieldId;
        protected Integer matchFieldWidth;

        private AbstractMatchFieldParser(T matchType, String tableName, String fieldName) {
            this.matchType = matchType;
            this.matchFieldId = getMatchFieldId(tableName, fieldName);
            this.matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        }

        public abstract FieldMatch parse();
    }

    private class ExactMatchParser extends AbstractMatchFieldParser<EXACT> {
        private ExactMatchParser(EXACT exact, String tableName, String fieldName) {
            super(exact, tableName, fieldName);
        }

        @Override
        public FieldMatch parse() {
            FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
            FieldMatch.Exact.Builder exactBuilder = FieldMatch.Exact.newBuilder();
            EXACT exact = matchType;
            ExactValueType valueType = exact.getExactValueType();
            if (valueType instanceof EXACTVALUETYPESTRING) {
                String valueStr = ((EXACTVALUETYPESTRING) valueType).getExactStringValue();
                exactBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(valueStr, matchFieldWidth)));
            } else if (valueType instanceof EXACTVALUETYPEBINARY) {
                byte[] valueBytes = ((EXACTVALUETYPEBINARY) valueType).getExactBinaryValue();
                exactBuilder.setValue(ByteString.copyFrom(valueBytes, 0, matchFieldWidth));
            } else {
                throw new IllegalArgumentException("Invalid exact value type");
            }
            fieldMatchBuilder.setExact(exactBuilder);
            fieldMatchBuilder.setFieldId(matchFieldId);
            return fieldMatchBuilder.build();
        }
    }

    private class LpmMatchParser extends AbstractMatchFieldParser<LPM> {
        private LpmMatchParser(LPM lpm, String tableName, String fieldName) {
            super(lpm, tableName, fieldName);
        }

        @Override
        public FieldMatch parse() {
            FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
            FieldMatch.LPM.Builder lpmBuilder = FieldMatch.LPM.newBuilder();
            LPM lpm = matchType;
            Long prefixLen = lpm.getLpmPrefixLen();
            LpmValueType valueType = lpm.getLpmValueType();
            if (valueType instanceof LPMVALUETYPESTRING) {
                String valueStr = ((LPMVALUETYPESTRING) valueType).getLpmStringValue();
                lpmBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(valueStr, matchFieldWidth)));
            } else if (valueType instanceof LPMVALUETYPEBINARY) {
                byte[] valueBytes = ((LPMVALUETYPEBINARY) valueType).getLpBinaryValue();
                lpmBuilder.setValue(ByteString.copyFrom(valueBytes, 0, matchFieldWidth));
            } else {
                throw new IllegalArgumentException("Invalid lpm value type");
            }
            lpmBuilder.setPrefixLen(prefixLen.intValue());
            fieldMatchBuilder.setLpm(lpmBuilder);
            fieldMatchBuilder.setFieldId(matchFieldId);
            return fieldMatchBuilder.build();
        }
    }

    private class TernaryMatchParser extends AbstractMatchFieldParser<TERNARY>  {
        private TernaryMatchParser(TERNARY ternary, String tableName, String fieldName) {
            super(ternary, tableName, fieldName);
        }

        @Override
        public FieldMatch parse() {
            FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
            FieldMatch.Ternary.Builder ternaryBuilder = FieldMatch.Ternary.newBuilder();
            TERNARY ternary = matchType;
            String mask = ternary.getTernaryMask();
            TernaryValueType valueType = ternary.getTernaryValueType();

            if (valueType instanceof TERNARYVALUETYPESTRING) {
                String valueStr = ((TERNARYVALUETYPESTRING) valueType).getTernaryStringValue();
                ternaryBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(valueStr, matchFieldWidth)));
            } else if (valueType instanceof TERNARYVALUEBINARY) {
                byte[] valueBytes = ((TERNARYVALUEBINARY) valueType).getTernaryBinaryValue();
                ternaryBuilder.setValue(ByteString.copyFrom(valueBytes, 0, matchFieldWidth));
            } else {
                throw new IllegalArgumentException("Invalid ternary value type");
            }

            if (mask.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                           + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                           + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
                ternaryBuilder.setMask(ByteString.copyFrom(Utils.strToByteArray(mask, 4)));
            } else if (mask.matches("([1-9]|[1-2][0-9]|3[0-2])")) {
                StringBuffer buffer = new StringBuffer(32);
                for (int i = 0; i < 32; i++) {
                    if (i < Integer.parseInt(mask)) {
                        buffer.append('1');
                    } else {
                        buffer.append('0');
                    }
                }

                String[] resultStr = new String[4];
                byte[] resultByte = new byte[4];
                for (int i = 0; i < resultStr.length; i++) {
                    resultStr[i] = buffer.substring(i * 8, i * 8 + 8);
                    for (int m = resultStr[i].length() - 1, n = 0; m >= 0; m--, n++) {
                        resultByte[i] += Byte.parseByte(resultStr[i].charAt(i) + "") * Math.pow(2, n);
                    }
                }
                ternaryBuilder.setMask(ByteString.copyFrom(resultByte));
            }
            fieldMatchBuilder.setTernary(ternaryBuilder);
            fieldMatchBuilder.setFieldId(matchFieldId);
            return fieldMatchBuilder.build();
        }
    }

    private class RangeMatchParser extends AbstractMatchFieldParser<RANGE> {
        private RangeMatchParser(RANGE range, String tableName, String fieldName) {
            super(range, tableName, fieldName);
        }

        @Override
        public FieldMatch parse() {
            FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
            FieldMatch.Range.Builder rangeBuilder = FieldMatch.Range.newBuilder();
            RANGE range = matchType;
            RangeValueType valueType = range.getRangeValueType();
            if (valueType instanceof RANGEVALUETYPESTRING) {
                String highStr = ((RANGEVALUETYPESTRING) valueType).getHighValueString();
                String lowStr = ((RANGEVALUETYPESTRING) valueType).getLowValueString();
                byte[] highBytes = BigInteger.valueOf(Integer.parseInt(highStr)).toByteArray();
                byte[] lowBytes = BigInteger.valueOf(Integer.parseInt(lowStr)).toByteArray();
                rangeBuilder.setHigh(ByteString.copyFrom(highBytes, 0, matchFieldWidth));
                rangeBuilder.setLow(ByteString.copyFrom(lowBytes, 0, matchFieldWidth));
            } else if (valueType instanceof RANGEVALUETYPEBINARY) {
                byte[] highBytes = ((RANGEVALUETYPEBINARY) valueType).getHighBinaryValue();
                byte[] lowBytes = ((RANGEVALUETYPEBINARY) valueType).getLowValueBinary();
                rangeBuilder.setHigh(ByteString.copyFrom(highBytes, 0, matchFieldWidth));
                rangeBuilder.setLow(ByteString.copyFrom(lowBytes, 0, matchFieldWidth));
            } else {
                throw new IllegalArgumentException("Invalid range value type");
            }
            fieldMatchBuilder.setFieldId(matchFieldId);
            fieldMatchBuilder.setRange(rangeBuilder);
            return fieldMatchBuilder.build();
        }
    }
}
