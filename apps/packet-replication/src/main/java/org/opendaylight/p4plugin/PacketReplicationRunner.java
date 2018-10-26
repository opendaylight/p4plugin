/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import org.opendaylight.p4plugin.appcommon.P4SwitchRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.P4pluginDeviceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketReplicationRunner extends P4SwitchRunner {
    private static final Logger LOG = LoggerFactory.getLogger(PacketReplicationRunner.class);

    public PacketReplicationRunner(final P4pluginDeviceService deviceService,
                                   final P4pluginP4runtimeService runtimeService,
                                   final String gRPCServerIp,
                                   final Integer gRPCServerPort,
                                   final Long deviceId,
                                   final String nodeId,
                                   final String configFile,
                                   final String runtimeFile) {
        super(deviceService, runtimeService, gRPCServerIp, gRPCServerPort, deviceId, nodeId, configFile, runtimeFile);
    }

    @Override
    public void run() {
        if (addDevice()) {
            p4Switch.openStreamChannel();
            p4Switch.setPipelineConfig();
        }
    }
}
