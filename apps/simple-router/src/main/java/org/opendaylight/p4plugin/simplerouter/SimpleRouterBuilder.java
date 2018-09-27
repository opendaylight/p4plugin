/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.simplerouter;

import org.opendaylight.p4plugin.appcommon.P4Switch;
import org.opendaylight.p4plugin.appcommon.P4SwitchBuilder;

public class SimpleRouterBuilder extends P4SwitchBuilder {
    public SimpleRouterBuilder() {
        super();
    }

    @Override
    public P4Switch build() {
        return new SimpleRouter(gRPCServerIp, gRPCServerPort, deviceId, nodeId,
                                configFile, runtimeFile, runtimeService);
    }
}
