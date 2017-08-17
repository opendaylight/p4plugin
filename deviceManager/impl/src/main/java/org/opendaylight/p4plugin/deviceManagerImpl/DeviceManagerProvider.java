/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.p4plugin.deviceManagerImpl;

/**
 * Created by hll on 8/16/17.
 */
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceManagerProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceManagerProvider.class);
    private final DataBroker dataBroker;
    public BundleContext bcontext;

    public DeviceManagerProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("Device Manager Provider Initiated");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("Device Manager Provider Closed");
    }

    public void setBcontext(BundleContext bcontext) {
        this.bcontext = bcontext;
    }
}