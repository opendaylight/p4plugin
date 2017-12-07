/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.table.match;

import org.opendaylight.p4plugin.core.impl.device.P4Device;
import org.opendaylight.p4plugin.p4runtime.proto.FieldMatch;

/**
 * Abstract match field parser, according to P4_16, there isn't valid match type.
 * Range match is limited in uin64, a little different from protobuf;
 * @param <T> Field match type.
 */
public abstract class AbstractMatchFieldParser<T> {
    protected P4Device device;
    protected T matchType;
    protected String tableName;
    protected String fieldName;

    public AbstractMatchFieldParser(P4Device device, T matchType, String tableName, String fieldName) {
        this.device = device;
        this.matchType = matchType;
        this.tableName = tableName;
        this.fieldName = fieldName;
    }

    protected Integer getMatchFieldId() {
        return device.getMatchFieldId(tableName, fieldName);
    }

    protected Integer getMatchFieldWidth() {
        return device.getMatchFieldWidth(tableName, fieldName);
    }

    public abstract FieldMatch parse();
}