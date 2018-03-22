/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl;

import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.p4plugin.runtime.impl.device.Device;
import org.opendaylight.p4plugin.runtime.impl.device.DeviceManager;
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
import java.util.concurrent.ThreadFactory;

public class PacketServiceImpl implements P4pluginPacketService {
    private static final Logger LOG = LoggerFactory.getLogger(PacketServiceImpl.class);
    private DeviceManager deviceManager;
    private ExecutorService executorService;

    public void init() {
        executorService = Executors.newCachedThreadPool(new PacketServiceThreadFactory());
        deviceManager = DeviceManager.getInstance();
    }

    public void close() {
        executorService.shutdown();
    }

    @Override
    public Future<RpcResult<Void>> p4TransmitPacket(P4TransmitPacketInput input) {
        return executorService.submit(()->{
            String nodeId = input.getNid();
            Optional<Device> optional = deviceManager.findDevice(nodeId);
            optional.orElseThrow(IllegalArgumentException::new).transmitPacket(input.getPayload());
            LOG.info("Transmit packet to device = {} RPC success.", nodeId);
            return RpcResultBuilder.success((Void)null).build();
        });
    }

    private static class PacketServiceThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "PacketServiceThread");
        }
    }
}
