/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.utils;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationPublisher {
    private static NotificationPublisher singleton = new NotificationPublisher();
    private NotificationPublishService notificationService;
    private static final Logger LOG = LoggerFactory.getLogger(NotificationPublisher.class);
    private NotificationPublisher() {}

    public static NotificationPublisher getInstance() {
        return singleton;
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
