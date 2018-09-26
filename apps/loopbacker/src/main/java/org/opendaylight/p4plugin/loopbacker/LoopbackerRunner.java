/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.loopbacker;

import org.opendaylight.p4plugin.appcommon.P4Switch;
import org.opendaylight.p4plugin.appcommon.P4SwitchRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.P4pluginDeviceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoopbackerRunner extends P4SwitchRunner {
    private static final Logger LOG = LoggerFactory.getLogger(LoopbackerRunner.class);

    public LoopbackerRunner(final P4pluginDeviceService deviceService,
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
    public P4Switch newSwitch(String gRPCServerIp,
                              Integer gRPCServerPort,
                              Long deviceId,
                              String nodeId,
                              String configFile,
                              String runtimeFile,
                              P4pluginP4runtimeService runtimeService) {
        return Loopbacker.newBuilder()
                .setServerIp(gRPCServerIp)
                .setServerPort(gRPCServerPort)
                .setDeviceId(deviceId)
                .setNodeId(nodeId)
                .setRuntimeFile(runtimeFile)
                .setConfigFile(configFile)
                .setRuntimeService(runtimeService)
                .build();
    }

    @Override
    public void run() {
        if (addDevice()) {
            p4Switch.openStreamChannel();
            p4Switch.setPipelineConfig();
            Runnable runnable = () -> {
                for (int index = 0; index < 10; index++) {
                    p4Switch.sendPacketOut("Welcome to ZTE.".getBytes());
                    SleepUtils.delay(10);
                }
            };
            new Thread(runnable).start();
        }
    }
}
