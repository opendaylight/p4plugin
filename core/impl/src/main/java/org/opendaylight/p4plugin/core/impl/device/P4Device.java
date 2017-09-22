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
import org.opendaylight.p4plugin.core.impl.connection.P4RuntimeStub;
import org.opendaylight.p4plugin.core.impl.utils.Utils;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.action.ActionParam;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.match.fields.Field;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.match.fields.field.MatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.match.fields.field.match.type.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.ActionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.action.type.ACTIONPROFILEGROUP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.action.type.ACTIONPROFILEMEMBER;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.table.entry.action.type.DIRECTACTION;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
 *  |                                 Switches                                    |
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
public class P4Device {
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

    private int getTableId(String tableName) {
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

    private int getActionProfileId(String actionProfileName) {
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

    public org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigResponse setPipelineConfig() {
        org.opendaylight.p4plugin.p4runtime.proto.ForwardingPipelineConfig.Builder configBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.ForwardingPipelineConfig.newBuilder();
        org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig.Builder p4DeviceConfigBuilder =
                org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig.newBuilder();
        if (deviceConfig != null) {
            p4DeviceConfigBuilder.setDeviceData(deviceConfig);
        }
        if (runtimeInfo != null) {
            configBuilder.setP4Info(runtimeInfo);
        }
        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        configBuilder.setDeviceId(deviceId);
        org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest request =
                org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest.newBuilder()
                .setAction(org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest.Action
                        .VERIFY_AND_COMMIT)
                .addConfigs(configBuilder.build())
                .build();
        org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigResponse response;

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

    public org.opendaylight.p4plugin.p4runtime.proto.GetForwardingPipelineConfigResponse getPipelineConfig() {
        org.opendaylight.p4plugin.p4runtime.proto.GetForwardingPipelineConfigRequest request =
                org.opendaylight.p4plugin.p4runtime.proto.GetForwardingPipelineConfigRequest.newBuilder()
                .addDeviceIds(deviceId)
                .build();
        org.opendaylight.p4plugin.p4runtime.proto.GetForwardingPipelineConfigResponse response;

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

    public org.opendaylight.p4plugin.p4runtime.proto.WriteResponse write(
            org.opendaylight.p4plugin.p4runtime.proto.WriteRequest request) {
        org.opendaylight.p4plugin.p4runtime.proto.WriteResponse response;
        try {
            response = stub.write(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Write RPC failed: status = {}, reason = {}.", e.getStatus(), e.getMessage());
        }
        return null;
    }

    public Iterator<org.opendaylight.p4plugin.p4runtime.proto.ReadResponse> read(
            org.opendaylight.p4plugin.p4runtime.proto.ReadRequest request) {
        Iterator<org.opendaylight.p4plugin.p4runtime.proto.ReadResponse> responses;
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

    private org.opendaylight.p4plugin.p4runtime.proto.TableAction BuildTableAction(ActionType actionType) {
        ActionParser parser = null;
        if (actionType instanceof DIRECTACTION) {
            parser = new DirectActionParser((DIRECTACTION) actionType);
        } else if (actionType instanceof ACTIONPROFILEMEMBER) {
            parser = new ActionProfileMemberParser((ACTIONPROFILEMEMBER) actionType);
        } else if (actionType instanceof ACTIONPROFILEGROUP) {
            parser = new ActionProfileGroupParser((ACTIONPROFILEGROUP) actionType);
        } else {
            LOG.info("Invalid action type.");
        }
        return parser == null ? null : parser.parse();
    }

    private org.opendaylight.p4plugin.p4runtime.proto.FieldMatch BuildFieldMatch(
            Field field, String tableName) {
        MatchFieldsParser parser = null;
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
        } else if (matchType instanceof VALID) {
            parser = new ValidMatchParser((VALID) matchType, tableName, fieldName);
        } else {
            LOG.info("Invalid match type.");
        }
        return parser == null ? null : parser.parse();
    }

    /**
     * Input table entry serialize to protobuf message, used for add/modify.
     * When this method is called, the device must be configured.
     */
    public org.opendaylight.p4plugin.p4runtime.proto.TableEntry toTableEntryMessage(TableEntry input) {
        String tableName = input.getTable();
        int tableId = getTableId(tableName);
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder tableEntryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        List<Field> fields = input.getField();
        fields.forEach(field -> {
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch fieldMatch = BuildFieldMatch(field, tableName);
            tableEntryBuilder.addMatch(fieldMatch);
        });
        ActionType actionType = input.getActionType();
        org.opendaylight.p4plugin.p4runtime.proto.TableAction tableAction = BuildTableAction(actionType);
        tableEntryBuilder.setTableId(tableId);
        tableEntryBuilder.setAction(tableAction);
        return tableEntryBuilder.build();
    }

    /**
     * Used for delete table entry, when delete a table entry, only need table name
     * and match fields actually. BTW, this the only way for search a table entry,
     * not support table entry id.
     */
    public org.opendaylight.p4plugin.p4runtime.proto.TableEntry toTableEntryMessage(EntryKey input) {
        String tableName = input.getTable();
        int tableId = getTableId(tableName);
        org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder tableEntryBuilder =
                org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
        List<Field> fields = input.getField();
        fields.forEach(field -> {
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch fieldMatch =
                    BuildFieldMatch(field, tableName);
            tableEntryBuilder.addMatch(fieldMatch);
        });
        tableEntryBuilder.setTableId(tableId);
        return tableEntryBuilder.build();
    }

    /**
     * Input action profile member serialize to protobuf message, used for add/modify.
     * When this method is called, the device must be configured.
     */
    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember toActionProfileMemberMessage(
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
            String paramValue = actionParam.getParamValue();
            int paramId = getParamId(actionName, paramName);
            int paramWidth = getParamWidth(actionName, paramName);
            paramBuilder.setParamId(paramId);
            byte[] valueByteArr = Utils.strToByteArray(paramValue, paramWidth);
            ByteString valueByteStr = ByteString.copyFrom(valueByteArr);
            paramBuilder.setValue(valueByteStr);
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
    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember toActionProfileMemberMessage(
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
    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup toActionProfileGroupMessage(
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
    public org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup toActionProfileGroupMessage(
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
    public String toTableEntryString(org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry) {
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
                case VALID:
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
    public String toActionProfileMemberString(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member) {
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
    public String toActionProfileGroupString(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup group) {
        int profileId = group.getActionProfileId();
        int groupId = group.getGroupId();
        String actionProfile = getActionProfileName(profileId);
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("%s - %d : ", actionProfile, groupId));
        group.getMembersList().forEach(member -> buffer.append(member.getMemberId()).append(" "));
        return new String(buffer);
    }

    public TableManager newTableManager() {
        return new TableManager();
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

    private interface ActionParser {
        org.opendaylight.p4plugin.p4runtime.proto.TableAction parse();
    }

    private class DirectActionParser implements ActionParser {
        private DIRECTACTION action;
        private DirectActionParser(DIRECTACTION action) {
            this.action = action;
        }

        @Override
        public org.opendaylight.p4plugin.p4runtime.proto.TableAction parse() {
            org.opendaylight.p4plugin.p4runtime.proto.TableAction.Builder tableActionBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.TableAction.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();
            List<ActionParam> params = action.getActionParam();
            String actionName = action.getActionName();
            actionBuilder.setActionId(getActionId(actionName));
            for (ActionParam p : params) {
                org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder();
                String paramName = p.getParamName();
                String paramValue = p.getParamValue();
                int paramId = getParamId(actionName, paramName);
                int paramWidth = getParamWidth(actionName, paramName);
                paramBuilder.setParamId(paramId);
                byte[] valueByteArr = Utils.strToByteArray(paramValue, paramWidth);
                ByteString valueByteStr = ByteString.copyFrom(valueByteArr);
                paramBuilder.setValue(valueByteStr);
                actionBuilder.addParams(paramBuilder);
            }
            return tableActionBuilder.setAction(actionBuilder).build();
        }
    }

    private class ActionProfileMemberParser implements ActionParser {
        private ACTIONPROFILEMEMBER action;
        private ActionProfileMemberParser(ACTIONPROFILEMEMBER action) {
            this.action = action;
        }

        @Override
        public org.opendaylight.p4plugin.p4runtime.proto.TableAction parse() {
            org.opendaylight.p4plugin.p4runtime.proto.TableAction.Builder builder =
                    org.opendaylight.p4plugin.p4runtime.proto.TableAction.newBuilder();
            builder.setActionProfileMemberId(action.getMemberId().intValue());
            return builder.build();
        }
    }

    private class ActionProfileGroupParser implements ActionParser {
        private ACTIONPROFILEGROUP action;
        private ActionProfileGroupParser(ACTIONPROFILEGROUP action) {
            this.action = action;
        }

        @Override
        public org.opendaylight.p4plugin.p4runtime.proto.TableAction parse() {
            org.opendaylight.p4plugin.p4runtime.proto.TableAction.Builder builder =
                    org.opendaylight.p4plugin.p4runtime.proto.TableAction.newBuilder();
            builder.setActionProfileGroupId(action.getGroupId().intValue());
            return builder.build();
        }
    }

    private abstract class MatchFieldsParser {
        protected org.opendaylight.yang.gen.v1.urn.opendaylight
                .p4plugin.core.table.rev170808.match.fields.field.MatchType matchType;
        protected Integer matchFieldId;
        protected Integer matchFieldWidth;

        private MatchFieldsParser(
                org.opendaylight.yang.gen.v1.urn.opendaylight
                        .p4plugin.core.table.rev170808.match.fields.field.MatchType matchType,
                String tableName, String fieldName) {
            this.matchType = matchType;
            this.matchFieldId = getMatchFieldId(tableName, fieldName);
            this.matchFieldWidth = getMatchFieldWidth(tableName, fieldName);
        }

        public abstract org.opendaylight.p4plugin.p4runtime.proto.FieldMatch parse();
    }

    private class ExactMatchParser extends MatchFieldsParser {
        private ExactMatchParser(EXACT exact, String tableName, String fieldName) {
            super(exact, tableName, fieldName);
        }

        public org.opendaylight.p4plugin.p4runtime.proto.FieldMatch parse() {
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Builder fieldMatchBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Exact.Builder exactBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Exact.newBuilder();
            EXACT exact = (EXACT) matchType;
            String value = exact.getExactValue();
            exactBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(value, matchFieldWidth)));
            fieldMatchBuilder.setExact(exactBuilder);
            fieldMatchBuilder.setFieldId(matchFieldId);
            return fieldMatchBuilder.build();
        }
    }

    private class LpmMatchParser extends MatchFieldsParser {
        private LpmMatchParser(LPM lpm, String tableName, String fieldName) {
            super(lpm, tableName, fieldName);
        }

        public org.opendaylight.p4plugin.p4runtime.proto.FieldMatch parse() {
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Builder fieldMatchBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.LPM.Builder lpmBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.LPM.newBuilder();
            LPM lpm = (LPM) matchType;
            String value = lpm.getLpmValue();
            int prefixLen = lpm.getLpmPrefixLen();
            // Value must match ipv4 address
            if (value.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                            + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                            + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
                lpmBuilder.setValue(ByteString.copyFrom(Utils.strToByteArray(value, matchFieldWidth)));
                lpmBuilder.setPrefixLen(prefixLen);
            }
            fieldMatchBuilder.setLpm(lpmBuilder);
            fieldMatchBuilder.setFieldId(matchFieldId);
            return fieldMatchBuilder.build();
        }
    }

    private class TernaryMatchParser extends MatchFieldsParser {
        private TernaryMatchParser(TERNARY ternary, String tableName, String fieldName) {
            super(ternary, tableName, fieldName);
        }

        public org.opendaylight.p4plugin.p4runtime.proto.FieldMatch parse() {
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Builder fieldMatchBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Ternary.Builder ternaryBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Ternary.newBuilder();
            TERNARY ternary = (TERNARY) matchType;
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

    private class RangeMatchParser extends MatchFieldsParser {
        private RangeMatchParser(RANGE range, String tableName, String fieldName) {
            super(range, tableName, fieldName);
        }

        public org.opendaylight.p4plugin.p4runtime.proto.FieldMatch parse() {
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Builder fieldMatchBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Range.Builder rangeBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Range.newBuilder();
            RANGE range = (RANGE) matchType;
            Long high = range.getRangeHigh();
            Long low = range.getRangeLow();
            rangeBuilder.setHigh(ByteString.copyFrom(Utils.intToByteArray(high.intValue())));
            rangeBuilder.setLow(ByteString.copyFrom(Utils.intToByteArray(low.intValue())));
            fieldMatchBuilder.setFieldId(matchFieldId);
            fieldMatchBuilder.setRange(rangeBuilder);
            return fieldMatchBuilder.build();
        }
    }

    private class ValidMatchParser extends MatchFieldsParser {
        private ValidMatchParser(VALID valid, String tableName, String fieldName) {
            super(valid, tableName, fieldName);
        }

        public org.opendaylight.p4plugin.p4runtime.proto.FieldMatch parse() {
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Builder fieldMatchBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Valid.Builder validBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.Valid.newBuilder();
            VALID valid = (VALID) matchType;
            validBuilder.setValue(valid.isValidValue());
            fieldMatchBuilder.setFieldId(matchFieldId);
            fieldMatchBuilder.setValid(validBuilder);
            return fieldMatchBuilder.build();
        }
    }

    public class TableManager {
        public TableManager() {}

        public boolean addTableEntry(TableEntry entry) {
            return new AddTableEntryOperator(entry).operate();
        }

        public boolean modifyTableEntry(TableEntry entry) {
            return new ModifyTableEntryOperator(entry).operate();
        }

        public boolean deleteTableEntry(EntryKey entry) {
            return new DeleteTableEntryOperator(entry).operate();
        }

        public List<String> readTableEntry(String tableName) {
            org.opendaylight.p4plugin.p4runtime.proto.ReadRequest.Builder request =
                    org.opendaylight.p4plugin.p4runtime.proto.ReadRequest.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.Entity.Builder entityBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.Entity.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.TableEntry.Builder entryBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.TableEntry.newBuilder();
            entryBuilder.setTableId(tableName == null ? 0 : getTableId(tableName));
            entityBuilder.setTableEntry(entryBuilder);
            request.addEntities(entityBuilder);
            request.setDeviceId(getDeviceId());

            Iterator<org.opendaylight.p4plugin.p4runtime.proto.ReadResponse> responses =
                    read(request.build());
            List<String> result = new ArrayList<>();

            while (responses.hasNext()) {
                org.opendaylight.p4plugin.p4runtime.proto.ReadResponse response = responses.next();
                List<org.opendaylight.p4plugin.p4runtime.proto.Entity> entityList =
                        response.getEntitiesList();
                boolean isCompleted = response.getComplete();
                entityList.forEach(entity-> {
                    String str = toTableEntryString(entity.getTableEntry());
                    result.add(str);
                });
                if (isCompleted) break;
            }
            return result;
        }

        private abstract class TableEntryOperator {
            private org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry;
            private org.opendaylight.p4plugin.p4runtime.proto.Update.Type type;

            protected void setEntry(org.opendaylight.p4plugin.p4runtime.proto.TableEntry entry) {
                this.entry = entry;
            }

            protected void setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type type) {
                this.type = type;
            }

            protected boolean operate() {
                org.opendaylight.p4plugin.p4runtime.proto.WriteRequest.Builder requestBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.WriteRequest.newBuilder();
                org.opendaylight.p4plugin.p4runtime.proto.Update.Builder updateBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.Update.newBuilder();
                org.opendaylight.p4plugin.p4runtime.proto.Entity.Builder entityBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.Entity.newBuilder();
                entityBuilder.setTableEntry(entry);
                updateBuilder.setType(type);
                updateBuilder.setEntity(entityBuilder);
                requestBuilder.setDeviceId(getDeviceId());
                requestBuilder.addUpdates(updateBuilder);
                return write(requestBuilder.build()) != null;
            }
        }

        private class AddTableEntryOperator extends TableEntryOperator {
            private AddTableEntryOperator(TableEntry entry) {
                setEntry(toTableEntryMessage(entry));
                setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type.INSERT);
            }
        }

        private class ModifyTableEntryOperator extends TableEntryOperator {
            private ModifyTableEntryOperator(TableEntry entry ) {
                setEntry(toTableEntryMessage(entry));
                setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type.MODIFY);
            }
        }

        private class DeleteTableEntryOperator extends TableEntryOperator {
            private DeleteTableEntryOperator(EntryKey key ) {
                setEntry(toTableEntryMessage(key));
                setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type.DELETE);
            }
        }

        public boolean addActionProfileMember(ActionProfileMember actionProfileMember) {
            return new AddActionProfileMemberOperator(actionProfileMember).operate();
        }

        public boolean modifyActionProfileMember(ActionProfileMember actionProfileMember) {
            return new ModifyActionProfileMemberOperator(actionProfileMember).operate();
        }

        public boolean deleteActionProfileMember(MemberKey key) {
            return new DeleteActionProfileMemberOperator(key).operate();
        }

        public List<String> readActionProfileMember(String actionProfileName) {
            org.opendaylight.p4plugin.p4runtime.proto.ReadRequest.Builder requestBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.ReadRequest.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.Entity.Builder entityBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.Entity.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.Builder memberBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember.newBuilder();

            memberBuilder.setActionProfileId(actionProfileName == null ? 0 : getActionProfileId(actionProfileName));
            entityBuilder.setActionProfileMember(memberBuilder);
            requestBuilder.setDeviceId(getDeviceId());
            requestBuilder.addEntities(entityBuilder);

            Iterator<org.opendaylight.p4plugin.p4runtime.proto.ReadResponse> responses = read(requestBuilder.build());
            List<String> result = new ArrayList<>();

            while (responses.hasNext()) {
                org.opendaylight.p4plugin.p4runtime.proto.ReadResponse response = responses.next();
                List<org.opendaylight.p4plugin.p4runtime.proto.Entity> entityList = response.getEntitiesList();
                boolean isCompleted = response.getComplete();
                entityList.forEach(entity-> {
                    String str = toActionProfileMemberString(entity.getActionProfileMember());
                    result.add(str);
                });
                if (isCompleted) break;
            }
            return result;
        }

        private abstract class ActionProfileMemberOperator {
            private org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member;
            private org.opendaylight.p4plugin.p4runtime.proto.Update.Type type;

            protected void setMember(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileMember member) {
                this.member = member;
            }

            protected void setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type type) {
                this.type = type;
            }

            protected boolean operate() {
                org.opendaylight.p4plugin.p4runtime.proto.WriteRequest.Builder requestBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.WriteRequest.newBuilder();
                org.opendaylight.p4plugin.p4runtime.proto.Update.Builder updateBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.Update.newBuilder();
                org.opendaylight.p4plugin.p4runtime.proto.Entity.Builder entityBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.Entity.newBuilder();
                entityBuilder.setActionProfileMember(member);
                updateBuilder.setType(type);
                updateBuilder.setEntity(entityBuilder);
                requestBuilder.addUpdates(updateBuilder);
                requestBuilder.setDeviceId(getDeviceId());
                return write(requestBuilder.build()) != null;
            }
        }

        private class AddActionProfileMemberOperator extends ActionProfileMemberOperator {
            private AddActionProfileMemberOperator(ActionProfileMember member) {
                setMember(toActionProfileMemberMessage(member));
                setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type.INSERT);
            }
        }

        private class ModifyActionProfileMemberOperator extends ActionProfileMemberOperator {
            private ModifyActionProfileMemberOperator(ActionProfileMember member) {
                setMember(toActionProfileMemberMessage(member));
                setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type.MODIFY);
            }
        }

        private class DeleteActionProfileMemberOperator extends ActionProfileMemberOperator {
            private DeleteActionProfileMemberOperator(MemberKey key) {
                setMember(toActionProfileMemberMessage(key));
                setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type.DELETE);
            }
        }

        public boolean addActionProfileGroup(ActionProfileGroup actionProfileGroup) {
            return new AddActionProfileGroupOperator(actionProfileGroup).operate();
        }

        public boolean modifyActionProfileGroup(ActionProfileGroup actionProfileGroup) {
            return new ModifyActionProfileGroupOperator(actionProfileGroup).operate();
        }

        public boolean deleteActionProfileGroup(GroupKey key) {
            return new DeleteActionProfileGroupOperator(key).operate();
        }

        public List<String> readActionProfileGroup(String actionProfileName) {
            org.opendaylight.p4plugin.p4runtime.proto.ReadRequest.Builder requestBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.ReadRequest.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.Entity.Builder entityBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.Entity.newBuilder();
            org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.Builder groupBuilder =
                    org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup.newBuilder();
            groupBuilder.setActionProfileId(actionProfileName == null ? 0 : getActionProfileId(actionProfileName));
            entityBuilder.setActionProfileGroup(groupBuilder);
            requestBuilder.setDeviceId(getDeviceId());
            requestBuilder.addEntities(entityBuilder);

            Iterator<org.opendaylight.p4plugin.p4runtime.proto.ReadResponse> responses = read(requestBuilder.build());
            List<String> result = new ArrayList<>();

            while (responses.hasNext()) {
                org.opendaylight.p4plugin.p4runtime.proto.ReadResponse response = responses.next();
                List<org.opendaylight.p4plugin.p4runtime.proto.Entity> entityList = response.getEntitiesList();
                boolean isCompleted = response.getComplete();
                entityList.forEach(entity-> {
                    String str = toActionProfileGroupString(entity.getActionProfileGroup());
                    result.add(str);
                });
                if (isCompleted) break;
            }
            return result;
        }

        private abstract class ActionProfileGroupOperator {
            private org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup group;
            private org.opendaylight.p4plugin.p4runtime.proto.Update.Type type;

            protected void setGroup(org.opendaylight.p4plugin.p4runtime.proto.ActionProfileGroup group) {
                this.group = group;
            }

            protected void setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type type) {
                this.type = type;
            }

            protected boolean operate() {
                org.opendaylight.p4plugin.p4runtime.proto.WriteRequest.Builder requestBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.WriteRequest.newBuilder();
                org.opendaylight.p4plugin.p4runtime.proto.Update.Builder updateBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.Update.newBuilder();
                org.opendaylight.p4plugin.p4runtime.proto.Entity.Builder entityBuilder =
                        org.opendaylight.p4plugin.p4runtime.proto.Entity.newBuilder();
                entityBuilder.setActionProfileGroup(group);
                updateBuilder.setEntity(entityBuilder);
                updateBuilder.setType(type);
                requestBuilder.addUpdates(updateBuilder);
                requestBuilder.setDeviceId(getDeviceId());
                return write(requestBuilder.build()) != null;
            }
        }

        private class AddActionProfileGroupOperator extends ActionProfileGroupOperator {
            private AddActionProfileGroupOperator(ActionProfileGroup group) {
                setGroup(toActionProfileGroupMessage(group));
                setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type.INSERT);
            }
        }

        private class ModifyActionProfileGroupOperator extends ActionProfileGroupOperator {
            private ModifyActionProfileGroupOperator(ActionProfileGroup group) {
                setGroup(toActionProfileGroupMessage(group));
                setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type.MODIFY);
            }
        }

        private class DeleteActionProfileGroupOperator extends ActionProfileGroupOperator {
            private DeleteActionProfileGroupOperator(GroupKey key) {
                setGroup(toActionProfileGroupMessage(key));
                setType(org.opendaylight.p4plugin.p4runtime.proto.Update.Type.DELETE);
            }
        }
    }
}
