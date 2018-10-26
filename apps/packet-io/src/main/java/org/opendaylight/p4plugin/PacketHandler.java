/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.packet.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class PacketHandler implements P4pluginP4runtimeListener {
    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    @Override
    public void onPacketReceived(PacketReceived notification) {
        byte[] packetByte = notification.getPayload();
        String nodeId = notification.getNid();
        List<Metadata> metadataList =  notification.getMetadata();
        StringBuilder stringBuilder = new StringBuilder();

        metadataList.forEach(
            metadata -> {
                String name = metadata.getMetadataName();
                String value = Arrays.toString(metadata.getMetadataValue());
                stringBuilder.append("[ metadata name = " + name + "/value = " + value + "]");
            }
        );

        LOG.info("Receive packet from {}, {}, payload = {}.", nodeId,
                new String(stringBuilder),
                Arrays.toString(packetByte));
    }
}
