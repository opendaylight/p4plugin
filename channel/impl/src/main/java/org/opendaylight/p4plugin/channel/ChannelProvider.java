/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.channel;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelProvider.class);
    private final DataBroker dataBroker;

    GrpcClient client = null;

    public ChannelProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Method called when the blueprint container is created.
     */

    public void init() {
        LOG.info("ChannelProvider Session Initiated");
        client = new GrpcClient("localhost",50051);
        LOG.info("ChannelProvider " + client.greet("dingrui"));
    }


    /**
     * Method called when the blueprint container is destroyed.
     */

    public void close() {
        LOG.info("ChannelProvider Closed");
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            LOG.error("Close is Interrupted", e);
        }
    }
}