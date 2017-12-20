/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.table.action;

import com.google.protobuf.ByteString;
import org.opendaylight.p4plugin.runtime.impl.device.P4Device;
import org.opendaylight.p4plugin.runtime.impl.utils.Utils;
import org.opendaylight.p4plugin.p4runtime.proto.Action;
import org.opendaylight.p4plugin.p4runtime.proto.TableAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.action.ActionParam;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.action.action.param.ParamValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.action.action.param.param.value.type.PARAMVALUETYPEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.action.action.param.param.value.type.PARAMVALUETYPESTRING;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.table.entry.action.type.DIRECTACTION;

import java.util.List;

public class DirectActionParser extends AbstractActionParser<DIRECTACTION> {
    public DirectActionParser(P4Device device, DIRECTACTION action) {
        super(device, action);
    }

    @Override
    public TableAction parse() {
        TableAction.Builder tableActionBuilder = TableAction.newBuilder();
        Action.Builder actionBuilder = Action.newBuilder();
        List<ActionParam> params = action.getActionParam();
        String actionName = action.getActionName();
        actionBuilder.setActionId(device.getActionId(actionName));
        for (ActionParam p : params) {
            Action.Param.Builder paramBuilder = Action.Param.newBuilder();
            String paramName = p.getParamName();
            int paramId = device.getParamId(actionName, paramName);
            int paramWidth = device.getParamWidth(actionName, paramName);
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
