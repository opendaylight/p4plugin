/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.channelImpl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelProvider.class);
    private final DataBroker dataBroker;
    public BundleContext bcontext;

    public ChannelProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }
    
    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        new ChannelImpl("localhost", 50051).shutdown();//grpc bug
        LOG.info("Channel Provider Initiated");
    }
    
    
    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("Channel Provider Closed");
    }

    public void setBcontext(BundleContext bcontext) {
        this.bcontext = bcontext;
    }
}