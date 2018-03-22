/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.device;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4info.proto.Table;

import java.io.*;
import java.util.Optional;

public class PipelineConfig {
    private ByteString deviceConfig;
    private P4Info runtimeInfo;

    public PipelineConfig(String configFilePath, String runtimeFilePath) throws IOException {
        this.deviceConfig = parseDeviceConfigFile(configFilePath);
        this.runtimeInfo = parseRuntimeInfoFile(runtimeFilePath);
    }

    private ByteString parseDeviceConfigFile(String configFilePath) throws IOException {
        if (configFilePath == null) {
            throw new IllegalArgumentException("Device config file path is null.");
        }

        InputStream input = new FileInputStream(new File(configFilePath));
        return ByteString.readFrom(input);
    }

    private P4Info parseRuntimeInfoFile(String runtimeFilePath) throws IOException {
        if (runtimeFilePath == null) {
            throw new IllegalArgumentException("Runtime info file path is null.");
        }

        Reader reader = null;
        P4Info.Builder info = P4Info.newBuilder();
        try {
            reader = new FileReader(runtimeFilePath);
            TextFormat.merge(reader, info);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return info.build();
    }

    public ByteString getDeviceConfig() {
        return deviceConfig;
    }

    public P4Info getRuntimeInfo() {
        return runtimeInfo;
    }

    public int getTableId(String tableName) {
        Optional<Table> optional = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid table name = " + tableName))
                .getPreamble().getId();
    }

    public String getTableName(int tableId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> optional = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid table id = " + tableId))
                .getPreamble().getName();
    }

    public int getMatchFieldId(String tableName, String matchFieldName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer =
                tableContainer.orElseThrow(()-> new IllegalArgumentException("Invalid table name = " + tableName))
                        .getMatchFieldsList()
                        .stream()
                        .filter(matchField -> matchField.getName().equals(matchFieldName))
                        .findFirst();

        return matchFieldContainer.orElseThrow(()-> new IllegalArgumentException("Invalid match field name = "
                + matchFieldName)).getId();
    }

    public String getMatchFieldName(int tableId, int matchFieldId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getId() == tableId)
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer =
                tableContainer.orElseThrow(()-> new IllegalArgumentException("Invalid table id = " + tableId))
                        .getMatchFieldsList()
                        .stream()
                        .filter(matchField -> matchField.getId() == (matchFieldId))
                        .findFirst();

        return matchFieldContainer.orElseThrow(()-> new IllegalArgumentException("Invalid match field id = "
                + matchFieldId)).getName();
    }

    public int getMatchFieldWidth(String tableName, String matchFieldName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = runtimeInfo.getTablesList()
                .stream()
                .filter(table -> table.getPreamble().getName().equals(tableName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer =
                tableContainer.orElseThrow(()-> new IllegalArgumentException("Invalid table name = " + tableName))
                        .getMatchFieldsList()
                        .stream()
                        .filter(matchField -> matchField.getName().equals(matchFieldName))
                        .findFirst();

        return (matchFieldContainer.orElseThrow(()-> new IllegalArgumentException("Invalid match field name = "
                + matchFieldName)).getBitwidth() + 7 ) / 8;
    }

    public int getActionId(String actionName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> optional = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid action name = " + actionName))
                .getPreamble().getId();
    }

    private String getActionName(int actionId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> optional = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid action id = " + actionId))
                .getPreamble().getName();
    }

    public int getParamId(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                actionContainer.orElseThrow(()-> new IllegalArgumentException("Invalid action name = " + actionName))
                        .getParamsList()
                        .stream()
                        .filter(param -> param.getName().equals(paramName))
                        .findFirst();

        return paramContainer.orElseThrow(()-> new IllegalArgumentException("Invalid param name = " + paramName))
                .getId();
    }

    public String getParamName(int actionId, int paramId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getId() == actionId)
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                actionContainer.orElseThrow(()-> new IllegalArgumentException("Invalid action id = " + actionId))
                        .getParamsList()
                        .stream()
                        .filter(param -> param.getId() == paramId)
                        .findFirst();

        return paramContainer.orElseThrow(()-> new IllegalArgumentException("Invalid param id = " + paramId))
                .getName();
    }

    public int getParamWidth(String actionName, String paramName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = runtimeInfo.getActionsList()
                .stream()
                .filter(action -> action.getPreamble().getName().equals(actionName))
                .findFirst();

        Optional<org.opendaylight.p4plugin.p4info.proto.Action.Param> paramContainer =
                actionContainer.orElseThrow(()-> new IllegalArgumentException("Invalid action name = " + actionName))
                        .getParamsList()
                        .stream()
                        .filter(param -> param.getName().equals(paramName))
                        .findFirst();

        return (paramContainer.orElseThrow(()-> new IllegalArgumentException("Invalid param name = " + paramName))
                .getBitwidth() + 7 ) / 8;
    }

    public int getActionProfileId(String actionProfileName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.ActionProfile> optional = runtimeInfo.getActionProfilesList()
                .stream()
                .filter(actionProfile -> actionProfile.getPreamble().getName().equals(actionProfileName))
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid action profile name = "
                + actionProfileName)).getPreamble().getId();
    }

    public String getActionProfileName(Integer actionProfileId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.ActionProfile> optional = runtimeInfo.getActionProfilesList()
                .stream()
                .filter(actionProfile -> actionProfile.getPreamble().getId() == actionProfileId)
                .findFirst();
        return optional.orElseThrow(()-> new IllegalArgumentException("Invalid action profile id = "
                + actionProfileId)).getPreamble().getName();
    }
}
