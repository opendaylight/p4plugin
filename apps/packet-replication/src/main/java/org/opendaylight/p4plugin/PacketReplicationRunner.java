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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketReplicationRunner extends ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(PacketReplicationRunner.class);

    public PacketReplicationRunner(final P4pluginDeviceService deviceService,
                                   final P4pluginP4runtimeService runtimeService,
                                   final String topoPathFile) {
        super(deviceService, runtimeService, topoPathFile);
    }

    @Override
    public void run() {
        loadTopo();
    }
}
