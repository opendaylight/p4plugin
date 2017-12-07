/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.table.match;

import com.google.protobuf.ByteString;
import org.opendaylight.p4plugin.core.impl.device.P4Device;
import org.opendaylight.p4plugin.p4runtime.proto.FieldMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.RANGE;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.range.RangeValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.range.range.value.type.RANGEVALUETYPEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.range.range.value.type.RANGEVALUETYPESTRING;

import java.math.BigInteger;

public class RangeMatchParser extends AbstractMatchFieldParser<RANGE> {
    public RangeMatchParser(P4Device device, RANGE range, String tableName, String fieldName) {
        super(device, range, tableName, fieldName);
    }

    @Override
    public FieldMatch parse() {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.Range.Builder rangeBuilder = FieldMatch.Range.newBuilder();
        RANGE range = matchType;
        RangeValueType valueType = range.getRangeValueType();
        Integer matchFieldWidth = getMatchFieldWidth();
        Integer matchFieldId = getMatchFieldId();
        rangeBuilder.setHigh(ByteString.copyFrom(getHighValue(valueType), 0, matchFieldWidth));
        rangeBuilder.setLow(ByteString.copyFrom(getLowValue(valueType), 0, matchFieldWidth));
        fieldMatchBuilder.setFieldId(matchFieldId);
        fieldMatchBuilder.setRange(rangeBuilder);
        return fieldMatchBuilder.build();
    }

    private byte[] getHighValue(RangeValueType valueType) {
        if (valueType instanceof RANGEVALUETYPESTRING) {
            String highStr = ((RANGEVALUETYPESTRING) valueType).getHighValueString();
            return BigInteger.valueOf(Integer.parseInt(highStr)).toByteArray();
        } else if (valueType instanceof RANGEVALUETYPEBINARY) {
            return ((RANGEVALUETYPEBINARY) valueType).getHighBinaryValue();
        } else {
            throw new IllegalArgumentException("Invalid range high value");
        }
    }

    private byte[] getLowValue(RangeValueType valueType) {
        if (valueType instanceof RANGEVALUETYPESTRING) {
            String lowStr = ((RANGEVALUETYPESTRING) valueType).getLowValueString();
            return BigInteger.valueOf(Integer.parseInt(lowStr)).toByteArray();
        } else if (valueType instanceof RANGEVALUETYPEBINARY) {
            return ((RANGEVALUETYPEBINARY) valueType).getLowValueBinary();
        } else {
            throw new IllegalArgumentException("Invalid range low value");
        }
    }
}
