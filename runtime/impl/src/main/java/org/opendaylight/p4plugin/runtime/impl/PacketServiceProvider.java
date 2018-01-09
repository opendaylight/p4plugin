/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl;

import org.opendaylight.p4plugin.runtime.impl.device.DeviceManager;
import org.opendaylight.p4plugin.runtime.impl.device.P4Device;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.packet.rev170808.P4TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.packet.rev170808.P4pluginPacketService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PacketServiceProvider implements P4pluginPacketService {
    private static final Logger LOG = LoggerFactory.getLogger(PacketServiceProvider.class);
    private DeviceManager manager;
    private ExecutorService executorService;

    public void init() {
        executorService = Executors.newFixedThreadPool(1);
        manager = DeviceManager.getInstance();
        LOG.info("P4plugin packet service provider initiated.");
    }

    public void close() {
        executorService.shutdown();
        LOG.info("P4plugin packet service provider closed.");
    }

    @Override
    public Future<RpcResult<Void>> p4TransmitPacket(P4TransmitPacketInput input) {
        return executorService.submit(()->{
            String nodeId = input.getNid();
            Optional<P4Device> optional = manager.findConfiguredDevice(nodeId);
            optional.orElseThrow(IllegalArgumentException::new).transmitPacket(input.getPayload());
            LOG.info("Transmit packet to device = {} RPC success.", nodeId);
            return RpcResultBuilder.success((Void)null).build();
        });
    }
}
