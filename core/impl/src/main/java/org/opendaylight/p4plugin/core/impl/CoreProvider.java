/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoreProvider {
    private static final Logger LOG = LoggerFactory.getLogger(CoreProvider.class);
    private final DataBroker dataBroker;
    private final NotificationPublishService notificationService;
    public BundleContext bcontext;

    public CoreProvider(final DataBroker dataBroker, 
                        final NotificationPublishService notificationService) {
        this.dataBroker = dataBroker;
        this.notificationService = notificationService;
    }
    
    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        new GrpcChannel("localhost", 50051).shutdown();//grpc bug
        NotificationProvider.getInstance().setNotificationService(notificationService); 
        LOG.info("P4plugin core provider initiated");
    }
    
    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("P4plugin core provider closed.");
    }

    public void setBcontext(BundleContext bcontext) {
        this.bcontext = bcontext;
    }
}