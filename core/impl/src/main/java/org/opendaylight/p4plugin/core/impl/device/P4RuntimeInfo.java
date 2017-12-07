/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.device;

import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4info.proto.Table;

import java.util.Optional;

public class P4RuntimeInfo {
    private P4Info info;

    public P4RuntimeInfo(P4Info info) {
        this.info = info;
    }

    public P4Info getP4Info() {
        return info;
    }

    public int getTableId(String tableName) {
        Optional<Table> container = info.getTablesList().stream()
                        .filter(table -> table.getPreamble().getName().equals(tableName))
                        .findFirst();
        int result = 0;
        if (container.isPresent()) {
            result = container.get().getPreamble().getId();
        }
        return result;
    }

    public String getTableName(int tableId) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> container = info.getTablesList()
                        .stream()
                        .filter(table -> table.getPreamble().getId() == tableId)
                        .findFirst();
        String result = null;
        if (container.isPresent()) {
            result = container.get().getPreamble().getName();
        }
        return result;
    }

    public int getMatchFieldId(String tableName, String matchFieldName) {
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = info.getTablesList()
                        .stream()
                        .filter(table -> table.getPreamble().getName().equals(tableName))
                        .findFirst();
        int result = 0;
        if (tableContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer = tableContainer.get()
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
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = info.getTablesList()
                        .stream()
                        .filter(table -> table.getPreamble().getId() == tableId)
                        .findFirst();
        String result = null;
        if (tableContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer = tableContainer.get()
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
        Optional<org.opendaylight.p4plugin.p4info.proto.Table> tableContainer = info.getTablesList()
                        .stream()
                        .filter(table -> table.getPreamble().getName().equals(tableName))
                        .findFirst();
        int result = 0;
        if (tableContainer.isPresent()) {
            Optional<org.opendaylight.p4plugin.p4info.proto.MatchField> matchFieldContainer = tableContainer.get()
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
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = info.getActionsList()
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
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = info.getActionsList()
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
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = info.getActionsList()
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
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = info.getActionsList()
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
        Optional<org.opendaylight.p4plugin.p4info.proto.Action> actionContainer = info.getActionsList()
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

    public int getActionProfileId(String actionProfileName) {
        int result = 0;
        Optional<org.opendaylight.p4plugin.p4info.proto.ActionProfile> profileContainer = info.getActionProfilesList()
                .stream()
                .filter(actionProfile -> actionProfile.getPreamble().getName().equals(actionProfileName))
                .findFirst();
        if (profileContainer.isPresent()) {
            result = profileContainer.get().getPreamble().getId();
        }
        return result;
    }

    public String getActionProfileName(Integer actionProfileId) {
        String result = null;
        Optional<org.opendaylight.p4plugin.p4info.proto.ActionProfile> profileContainer = info.getActionProfilesList()
                .stream()
                .filter(actionProfile -> actionProfile.getPreamble().getId() == actionProfileId)
                .findFirst();
        if (profileContainer.isPresent()) {
            result = profileContainer.get().getPreamble().getName();
        }
        return result;
    }
}
