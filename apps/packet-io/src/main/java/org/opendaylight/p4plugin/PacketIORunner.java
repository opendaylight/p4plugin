/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import org.opendaylight.p4plugin.appcommon.ApplicationRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.P4pluginDeviceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.packet.metadata.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.packet.metadata.MetadataBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PacketIORunner extends ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(PacketIORunner.class);

    public PacketIORunner(final P4pluginDeviceService deviceService,
                          final P4pluginP4runtimeService runtimeService,
                          final String topoFilePath) {
        super(deviceService, runtimeService, topoFilePath);
    }

    @Override
    public void run() {
        if (loadTopo()) {
            List<Metadata> metadataList = new ArrayList<>();
            MetadataBuilder metadataBuilder1 = new MetadataBuilder();
            metadataBuilder1.setMetadataValue(new byte[]{0,0,0,0,0,0,0,0});
            metadataBuilder1.setMetadataName("cpu_preamble");
            MetadataBuilder metadataBuilder2 = new MetadataBuilder();
            metadataBuilder2.setMetadataValue(new byte[]{37});
            metadataBuilder2.setMetadataName("egress_port");
            metadataList.add(metadataBuilder1.build());
            metadataList.add(metadataBuilder2.build());

            Runnable runnable = () -> {
                for (int index = 0; index < 10; index++) {
                    p4SwitchMap.get("s1").sendPacketOut(metadataList, "Welcome to ZTE.".getBytes());
                    SleepUtils.delay(10);
                }
            };
            new Thread(runnable).start();
        }
    }

    private static class SleepUtils {
        private static void delay(long seconds) {
            try {
                TimeUnit.SECONDS.sleep(seconds);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
