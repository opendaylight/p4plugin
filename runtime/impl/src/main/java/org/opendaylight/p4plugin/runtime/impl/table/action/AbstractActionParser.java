/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.table.action;

import org.opendaylight.p4plugin.runtime.impl.device.P4Device;
import org.opendaylight.p4plugin.p4runtime.proto.TableAction;

/**
 * Abstract action parser, we think there are three types actions for
 * direct action, action profile member and action profile group currently.
 * Each action parser must extends the abstract action parser.
 * @param <T> Action type.
 */
public abstract class AbstractActionParser<T> {
    protected T action;
    protected P4Device device;

    public  AbstractActionParser(T action) {
        this(null, action);
    }

    public AbstractActionParser(P4Device device, T action) {
        this.device = device;
        this.action = action;
    }

    public abstract TableAction parse();
}