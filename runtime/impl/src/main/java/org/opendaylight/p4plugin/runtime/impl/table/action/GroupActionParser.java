/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.table.action;

import org.opendaylight.p4plugin.p4runtime.proto.TableAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.table.rev170808.table.entry.action.type.ACTIONPROFILEGROUP;

public class GroupActionParser extends AbstractActionParser<ACTIONPROFILEGROUP> {
    public GroupActionParser(ACTIONPROFILEGROUP action) {
        super(action);
    }

    @Override
    public TableAction parse() {
        TableAction.Builder builder = TableAction.newBuilder();
        builder.setActionProfileGroupId(action.getGroupId().intValue());
        return builder.build();
    }
}
