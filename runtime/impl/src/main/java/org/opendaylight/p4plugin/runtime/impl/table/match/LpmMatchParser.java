/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.table.match;

import com.google.protobuf.ByteString;
import org.opendaylight.p4plugin.runtime.impl.device.P4Device;
import org.opendaylight.p4plugin.runtime.impl.utils.Utils;
import org.opendaylight.p4plugin.p4runtime.proto.FieldMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.key.field.match.type.LPM;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.key.field.match.type.lpm.LpmValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.key.field.match.type.lpm.lpm.value.type.LPMVALUETYPEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.key.field.match.type.lpm.lpm.value.type.LPMVALUETYPESTRING;

public class LpmMatchParser extends AbstractMatchFieldParser<LPM> {
    public LpmMatchParser(P4Device device, LPM lpm, String tableName, String fieldName) {
        super(device, lpm, tableName, fieldName);
    }

    @Override
    public FieldMatch parse() {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.LPM.Builder lpmBuilder = FieldMatch.LPM.newBuilder();
        LPM lpm = matchType;
        Long prefixLen = lpm.getLpmPrefixLen();
        LpmValueType valueType = lpm.getLpmValueType();
        Integer matchFieldWidth = getMatchFieldWidth();
        Integer matchFieldId = getMatchFieldId();
        lpmBuilder.setValue(ByteString.copyFrom(getValue(valueType), 0, matchFieldWidth));
        lpmBuilder.setPrefixLen(prefixLen.intValue());
        fieldMatchBuilder.setLpm(lpmBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private byte[] getValue(LpmValueType valueType) {
        if (valueType instanceof LPMVALUETYPESTRING) {
            String valueStr = ((LPMVALUETYPESTRING) valueType).getLpmStringValue();
            return Utils.strToByteArray(valueStr, getMatchFieldWidth());
        } else if (valueType instanceof LPMVALUETYPEBINARY) {
            return ((LPMVALUETYPEBINARY) valueType).getLpBinaryValue();
        } else {
            throw new IllegalArgumentException("Invalid lpm value type");
        }
    }
}
