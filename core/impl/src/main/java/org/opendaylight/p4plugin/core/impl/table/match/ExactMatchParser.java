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
import org.opendaylight.p4plugin.core.impl.utils.Utils;
import org.opendaylight.p4plugin.p4runtime.proto.FieldMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.EXACT;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.exact.ExactValueType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.exact.exact.value.type.EXACTVALUETYPEBINARY;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.table.rev170808.key.field.match.type.exact.exact.value.type.EXACTVALUETYPESTRING;

public class ExactMatchParser extends AbstractMatchFieldParser<EXACT>{
    public ExactMatchParser(P4Device device, EXACT exact, String tableName, String fieldName) {
        super(device, exact, tableName, fieldName);
    }

    @Override
    public FieldMatch parse() {
        FieldMatch.Builder fieldMatchBuilder = FieldMatch.newBuilder();
        FieldMatch.Exact.Builder exactBuilder = FieldMatch.Exact.newBuilder();
        EXACT exact = matchType;
        ExactValueType valueType = exact.getExactValueType();
        Integer matchFieldWidth = getMatchFieldWidth();
        Integer matchFieldId = getMatchFieldId();
        exactBuilder.setValue(ByteString.copyFrom(getValue(valueType), 0, matchFieldWidth));
        fieldMatchBuilder.setExact(exactBuilder);
        fieldMatchBuilder.setFieldId(matchFieldId);
        return fieldMatchBuilder.build();
    }

    private byte[] getValue(ExactValueType valueType) {
        if (valueType instanceof EXACTVALUETYPESTRING) {
            String valueStr = ((EXACTVALUETYPESTRING) valueType).getExactStringValue();
            return Utils.strToByteArray(valueStr, getMatchFieldWidth());
        } else if (valueType instanceof EXACTVALUETYPEBINARY) {
            return ((EXACTVALUETYPEBINARY) valueType).getExactBinaryValue();
        } else {
            throw new IllegalArgumentException("Invalid exact value type");
        }
    }
}
