/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.channel;
import com.google.protobuf.ByteString;
import org.opendaylight.p4plugin.p4info.proto.*;
import org.opendaylight.p4plugin.p4runtime.proto.FieldMatch;
import org.opendaylight.p4plugin.p4runtime.proto.TableAction;
import org.opendaylight.p4plugin.p4runtime.proto.TableEntry;
import java.util.LinkedHashMap;
import java.util.List;


public class WriteTableEntryData {
    private Integer deviceId;
    private UpdateType updateType;
    private MatchType matchType;
    private String tableName;
    private String fieldName;
    private String matchValue;
    private Integer prefixLen;
    private String actionName;
    private LinkedHashMap<String, String> params;

    private WriteTableEntryData() {}

    public Integer getDeviceId() {
        return deviceId;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public String getTableName() {
        return tableName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMatchValue() {
        return matchValue;
    }

    public String getActionName() {
        return actionName;
    }

    public LinkedHashMap<String, String>  getParams() {
        return params;
    }

    public Integer getPrefixLen() {
        return prefixLen;
    }

    public static int getTableId(String tableName, P4Info p4info) {
        List<Table> list = p4info.getTablesList();
        for(Table table : list) {
            Preamble preamble = table.getPreamble();
            if(preamble.getName().equals(tableName)) {
                return preamble.getId();
            }
        }

        return  -1;
    }

    public static int getMatchFieldId(String tableName, String matchFieldName, P4Info p4info) {
        List<Table> list = p4info.getTablesList();
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

    public static int getMatchFieldWidth(String tableName, String matchFieldName, P4Info p4info) {
        List<Table> list = p4info.getTablesList();
        Table table = null;

        for(Table t : list) {
            if(t.getPreamble().getName().equals(tableName)) { table = t; break; }
        }

        List<MatchField> mfList = table.getMatchFieldsList();
        for(MatchField mf : mfList) {
            if (mf.getName().equals(matchFieldName)) {
                return mf.getBitwidth() % 8 == 0 ?  mf.getBitwidth() / 8 : mf.getBitwidth() / 8 + 1;
            }
        }

        return  -1;
    }


    public static int getActionId(String actionName, P4Info p4info) {
        List<Action> actionList = p4info.getActionsList();
        for (org.opendaylight.p4plugin.p4info.proto.Action action : actionList) {
            Preamble preamble = action.getPreamble();
            if (preamble.getName().equals(actionName)) { return action.getPreamble().getId(); }
        }
        return -1;
    }

    public static int getParamId(String actionName, String paramName, P4Info p4info) {
        List<org.opendaylight.p4plugin.p4info.proto.Action> actionList = p4info.getActionsList();
        for (org.opendaylight.p4plugin.p4info.proto.Action action : actionList) {
            Preamble preamble = action.getPreamble();
            if (!preamble.getName().equals(actionName)) { continue; }
            for (org.opendaylight.p4plugin.p4info.proto.Action.Param param : action.getParamsList()) {
                if (param.getName().equals(paramName)) { return param.getId(); }
            }
        }
        return -1;
    }

    public static int getParamWidth(String actionName, String paramName, P4Info p4info) {
        List<org.opendaylight.p4plugin.p4info.proto.Action> actionList = p4info.getActionsList();
        for (org.opendaylight.p4plugin.p4info.proto.Action action : actionList) {
            Preamble preamble = action.getPreamble();
            if (!preamble.getName().equals(actionName)) { continue; }
            for (org.opendaylight.p4plugin.p4info.proto.Action.Param param : action.getParamsList()) {
                if (param.getName().equals(paramName)) {
                    return param.getBitwidth() + 7 / 8;
                }
            }
        }
        return -1;
    }

    public static byte[] strToByteArray(String str, int len) {
        String[] strArray = null;
        byte[] byteArray = null;

        /* regular ipv4 address match (1~255).(0~255).(0~255).(0~255) */
        if(str.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                     + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                     + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
            strArray = str.split("\\.");
            byteArray = new byte[strArray.length];
            assert(len == strArray.length);
            for (int i = 0; i < strArray.length; i++) {
                byteArray[i] = (byte)Integer.parseInt(strArray[i]);
            }
        }else if (str.matches("([0-9a-fA-F]{1,2}:){5}[0-9a-fA-F]{1,2}")){ /* mac address,aa:bb:cc:dd:ee:ff,1:2:3:4:5:6 */
            strArray = str.split(":");
            byteArray = new byte[strArray.length];
            assert(len == strArray.length);
            for (int i = 0; i < strArray.length; i++) {
                byteArray[i] = (byte)Integer.parseInt(strArray[i],16);
            }
        }else {
            int value = Integer.parseInt(str);
            byteArray = new byte[len];
            for(int i = 0; i < len; i++) {
                byteArray[i] = (byte)(value >> ((len - i - 1) * 8) & 0xFF);
            }
        }
        return byteArray;
    }

    public TableEntry toMessage(P4Info info) {
        int tableId = getTableId(tableName, info);
        int matchFieldId = getMatchFieldId(tableName, fieldName, info);
        int actionId = getActionId(actionName, info);

        org.opendaylight.p4plugin.p4runtime.proto.Action.Builder actionBuilder =
                                                    org.opendaylight.p4plugin.p4runtime.proto.Action.newBuilder();
        actionBuilder.setActionId(actionId);
        for (String k : params.keySet()) {
            org.opendaylight.p4plugin.p4runtime.proto.Action.Param.Builder paramBuilder =
                                                    org.opendaylight.p4plugin.p4runtime.proto.Action.Param.newBuilder();
            paramBuilder.setParamId(getParamId(actionName, k, info));
            paramBuilder.setValue(ByteString.copyFrom(strToByteArray(params.get(k), getParamWidth(actionName, k, info))));
            actionBuilder.addParams(paramBuilder.build());
        }

        TableAction.Builder tableActionBuilder = TableAction.newBuilder();
        tableActionBuilder.setAction(actionBuilder.build());
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        fieldMatchBuilder.setFieldId(matchFieldId);

        switch(matchType) {
            case EXACT: {
                FieldMatch.Exact.Builder exactBuilder = FieldMatch.Exact.newBuilder();
                exactBuilder.setValue(ByteString.copyFrom(strToByteArray(matchValue,
                                                                         getMatchFieldWidth(tableName, fieldName, info))));
                fieldMatchBuilder.setExact(exactBuilder.build());
                break;
            }
            case TERNARY:
                break;
            case LPM: {
                FieldMatch.LPM.Builder lpmBuilder = FieldMatch.LPM.newBuilder();
                lpmBuilder.setValue(ByteString.copyFrom(strToByteArray(matchValue,
                                                                       getMatchFieldWidth(tableName, fieldName, info))));
                lpmBuilder.setPrefixLen(prefixLen);
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

    enum MatchType {
        EXACT,
        TERNARY,
        LPM,
        RANGE,
        VALID
    }

    enum UpdateType {
        UNSPECIFIED,
        INSERT,
        MODIFY,
        DELETE
    }

    /**
     * address must be in decimal and separated by dot, like 10.0.0.10;
     * mac address must be in hexadecimal and separated by colon, like 01:02:03:0a:0b:0c;
     * prefiexlen must be in decimal;
     */
    public static final class Builder {
        private Integer deviceId_;
        private UpdateType updateType_;
        private MatchType matchType_;
        private String tableName_;
        private String fieldName_;
        private String matchValue_;
        private Integer prefixLen_;
        private String actionName_;
        private LinkedHashMap<String, String> params_ = new LinkedHashMap<>();

        public Builder() {}
        public Builder setDeviceId(Integer deviceId) {
            deviceId_ = deviceId;
            return this;
        }

        public Builder setUpdateType(UpdateType type) {
            updateType_ = type;
            return this;
        }

        public Builder setMatchType(MatchType type) {
            matchType_ = type;
            return this;
        }

        public Builder setTableName(String tableName) {
            tableName_ = tableName;
            return this;
        }

        public Builder setFieldName(String fieldName) {
            fieldName_ = fieldName;
            return this;
        }

        public Builder setMatchValue(String value) {
            matchValue_ = value;
            return this;
        }

        public Builder setPrefixLen(Integer prefixLen) {
            prefixLen_ = prefixLen;
            return this;
        }

        public Builder setActionName(String actionName) {
            actionName_ = actionName;
            return this;
        }

        public Builder addParams(String key, String value) {
            params_.put(key, value);
            return this;
        }

        public WriteTableEntryData build() {
            WriteTableEntryData entry = new WriteTableEntryData();
            entry.deviceId = deviceId_;
            entry.matchType = matchType_;
            entry.matchValue = matchValue_;
            entry.updateType = updateType_;
            entry.actionName = actionName_;
            entry.fieldName = fieldName_;
            entry.params = params_;
            entry.prefixLen = prefixLen_;
            entry.tableName = tableName_;
            return entry;
        }
    }
}
