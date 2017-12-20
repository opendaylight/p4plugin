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
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.key.field.match.type.TERNARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.key.field.match.type.ternary.TernaryValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.key.field.match.type.ternary.ternary.value.type.TERNARYVALUEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.key.field.match.type.ternary.ternary.value.type.TERNARYVALUETYPESTRING;

public class TernaryMatchParser extends  AbstractMatchFieldParser<TERNARY> {
    public TernaryMatchParser(P4Device device, TERNARY ternary, String tableName, String fieldName) {
        super(device, ternary, tableName, fieldName);
    }

    @Override
    public FieldMatch parse() {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.Ternary.Builder ternaryBuilder = FieldMatch.Ternary.newBuilder();
        TERNARY ternary = matchType;
        String mask = ternary.getTernaryMask();
        TernaryValueType valueType = ternary.getTernaryValueType();
        Integer matchFieldWidth = getMatchFieldWidth();
        Integer matchFieldId = getMatchFieldId();
        ternaryBuilder.setValue(ByteString.copyFrom(getValue(valueType), 0, matchFieldWidth));
        ternaryBuilder.setMask(ByteString.copyFrom(getMask(mask), 0, matchFieldWidth));
        fieldMatchBuilder.setTernary(ternaryBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private byte[] getValue(TernaryValueType valueType) {
        if (valueType instanceof TERNARYVALUETYPESTRING) {
            String valueStr = ((TERNARYVALUETYPESTRING) valueType).getTernaryStringValue();
            return Utils.strToByteArray(valueStr, getMatchFieldWidth());
        } else if (valueType instanceof TERNARYVALUEBINARY) {
            return ((TERNARYVALUEBINARY) valueType).getTernaryBinaryValue();
        } else {
            throw new IllegalArgumentException("Invalid ternary value type");
        }
    }

    private byte[] getMask(String mask) {
        if (mask.matches("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]|25[0-5])\\."
                + "((\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])\\.){2}"
                + "(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])")) {
            return Utils.strToByteArray(mask, 4);
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
            return resultByte;
        } else {
            throw new IllegalArgumentException("Invalid ternary mask value");
        }
    }
}
