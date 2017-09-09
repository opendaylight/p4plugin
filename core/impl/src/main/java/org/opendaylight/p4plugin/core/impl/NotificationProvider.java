/*
 * Copyright © 2016 ZTE,Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationProvider {
    private static NotificationProvider instance = new NotificationProvider();
    private NotificationPublishService notificationService;
    private static final Logger LOG = LoggerFactory.getLogger(NotificationProvider.class);

    private NotificationProvider() {}

    public static NotificationProvider getInstance() {
        return instance;
    }

    public void setNotificationService(NotificationPublishService notificationService) {
        this.notificationService = notificationService;
    }

    public <T extends Notification> void notify(T notification) {
        if (null != notificationService) {
            LOG.info("Notification publish!");
            notificationService.offerNotification(notification);
        }
    }
}

