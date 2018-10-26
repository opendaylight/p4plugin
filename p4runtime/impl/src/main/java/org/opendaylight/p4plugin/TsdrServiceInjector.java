/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.p4plugin.gnmi.DataStorage;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TsdrServiceInjector {
    private static final Logger LOG = LoggerFactory.getLogger(TsdrServiceInjector.class);
    private final DataBroker dataBroker;
    private final TsdrCollectorSpiService tsdrCollectorSpiService;

    public TsdrServiceInjector(final DataBroker dataBroker,
                               final TsdrCollectorSpiService tsdrCollectorSpiService) {
        this.dataBroker = dataBroker;
        this.tsdrCollectorSpiService = tsdrCollectorSpiService;
    }

    public void init() {
        DataStorage.getInstance().setTsdrCollectorSpiService(tsdrCollectorSpiService);
        LOG.info("P4 plugin TSDR service injector initiated.");
    }

    public void close() {
        LOG.info("P4 plugin TSDR service injector closed.");
    }
}
