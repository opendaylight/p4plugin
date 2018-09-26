/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.loopbacker;

import org.opendaylight.p4plugin.appcommon.P4Switch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Loopbacker extends P4Switch {
    private static final Logger LOG = LoggerFactory.getLogger(Loopbacker.class);

    public Loopbacker(String gRPCServerIp,
                      Integer gRPCServerPort,
                      Long deviceId,
                      String nodeId,
                      String configFile,
                      String runtimeFile,
                      P4pluginP4runtimeService runtimeService) {
        super(gRPCServerIp, gRPCServerPort, deviceId, nodeId, configFile, runtimeFile, runtimeService);
    }

    public static LoopbackerBuilder newBuilder() {
        return new LoopbackerBuilder();
    }
}
