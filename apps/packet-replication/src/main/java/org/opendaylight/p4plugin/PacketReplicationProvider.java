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

public class PacketReplicationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(PacketReplicationProvider.class);
    private final DataBroker dataBroker;
    private final P4pluginDeviceService deviceService;
    private final P4pluginP4runtimeService runtimeService;
    private final String gRPCServerIp;
    private final Integer gRPCServerPort;
    private final Long deviceId;
    private final String nodeId;
    private final String configFile;
    private final String runtimeFile;
    private PacketReplicationRunner packetReplicationRunner;

    public PacketReplicationProvider(final DataBroker dataBroker,
                                     final P4pluginDeviceService deviceService,
                                     final P4pluginP4runtimeService runtimeService,
                                     final String gRPCServerIp,
                                     final Integer gRPCServerPort,
                                     final Long deviceId,
                                     final String nodeId,
                                     final String configFile,
                                     final String runtimeFile) {
        this.dataBroker = dataBroker;
        this.deviceService = deviceService;
        this.runtimeService = runtimeService;
        this.gRPCServerIp = gRPCServerIp;
        this.gRPCServerPort = gRPCServerPort;
        this.deviceId = deviceId;
        this.nodeId = nodeId;
        this.configFile = configFile;
        this.runtimeFile = runtimeFile;
    }

    public void init() {
        packetReplicationRunner = new PacketReplicationRunner(deviceService, runtimeService, gRPCServerIp,
                                        gRPCServerPort, deviceId, nodeId, configFile, runtimeFile);
        packetReplicationRunner.run();
        LOG.info("Packet replication provider init.");
    }

    public void close() {
        packetReplicationRunner.close();
        LOG.info("Packet replication provider closed.");
    }
}
