/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.p4plugin.core.impl.device.DeviceManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.packet.rev170808.P4TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.packet.rev170808.P4pluginCorePacketService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;

public class PacketServiceProvider implements P4pluginCorePacketService {
    private static final Logger LOG = LoggerFactory.getLogger(PacketServiceProvider.class);
    private final DeviceManager manager =  DeviceManager.getInstance();
    public Future<RpcResult<Void>> p4TransmitPacket(P4TransmitPacketInput input) {
        Preconditions.checkArgument(input != null, "Transmit packet input is null.");
        try {
            manager.findConfiguredDevice(input.getNodeId()).transmitPacket(input.getPayload());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Futures.immediateFuture(RpcResultBuilder.success((Void)null).build());
    }
}
