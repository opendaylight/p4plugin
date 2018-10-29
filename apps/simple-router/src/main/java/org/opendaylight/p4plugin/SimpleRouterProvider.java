/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.P4pluginDeviceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRouterProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleRouterProvider.class);
    private final DataBroker dataBroker;
    private final P4pluginDeviceService deviceService;
    private final P4pluginP4runtimeService runtimeService;
    private final String topoFilePath;
    private SimpleRouterRunner simpleRouterRunner;

    public SimpleRouterProvider(final DataBroker dataBroker,
                                final P4pluginDeviceService deviceService,
                                final P4pluginP4runtimeService runtimeService,
                                final String topoFilePath) {
        this.dataBroker = dataBroker;
        this.deviceService = deviceService;
        this.runtimeService = runtimeService;
        this.topoFilePath = topoFilePath;
    }

    public void init() {
        simpleRouterRunner = new SimpleRouterRunner(deviceService, runtimeService, topoFilePath);
        simpleRouterRunner.run();
        LOG.info("Simple router provider init.");
    }

    public void close() {
        simpleRouterRunner.removeTopo();
        LOG.info("Simple router provider closed.");
    }
}
