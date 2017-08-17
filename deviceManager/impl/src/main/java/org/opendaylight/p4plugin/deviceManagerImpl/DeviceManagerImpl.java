/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.p4plugin.deviceManagerImpl;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.p4plugin.channelImpl.ChannelImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.manager.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class DeviceManagerImpl implements DeviceManagerService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceManagerImpl.class.getName());
    ConcurrentHashMap<String, ChannelImpl> clients;

    public DeviceManagerImpl() {
        clients = new ConcurrentHashMap<>();
    }

    /**
     * Create a new client according to host:port:deviceId, then send a master arbitration message;
     * @param host ip addres;s
     * @param port tcp port;
     * @param deviceId device id which can be set when starting bmv2 using --device-id, default is 0;
     * @return A new client, when gRPC server is down, the client.getState() function will return false;
     */
    public ChannelImpl newClient(String host, Integer port, BigInteger deviceId) {
        ChannelImpl client = new ChannelImpl(host, port);
        //send master arbitration, just as a handshake
        client.sendMasterArbitration(deviceId.longValue());
        return client;
    }

    /**
     * Find a client is in clients
     * @param host
     * @param port
     * @param deviceId
     * @return
     */
    public ChannelImpl findClient(String host, Integer port, BigInteger deviceId) {
        String key = String.format("%s:%d:%d", host, port, deviceId);
        ChannelImpl client = null;
        for(String k : clients.keySet()) {
            if(k.equals(key)) {
                client = clients.get(k);
                break;
            }
        }
        return client;
    }

    @Override
    public Future<RpcResult<ConnectToTargetOutput>> connectToTarget(ConnectToTargetInput input) {
        String host = input.getIp().getIpv4Address().getValue();
        Integer port = input.getPort().getValue();
        BigInteger deviceId = input.getDeviceId();
        String key = String.format("%s:%d:%d", host, port, deviceId);
        ChannelImpl client = newClient(host, port, deviceId);
        if(client.getState()) {
            clients.put(key, client);
        }
        ConnectToTargetOutputBuilder builder = new ConnectToTargetOutputBuilder();
        builder.setResult(client.getState());
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<TransmitPacketOutput>> transmitPacket(TransmitPacketInput input) {
        Integer port = input.getPort();
        Integer reason = input.getReason();
        byte[] zeros = input.getZeros();
        byte[] payload = input.getPayload();
        Arrays.fill(zeros, (byte)0);
        byte[] fullPacket = new byte[2 + 2 + zeros.length + payload.length];
        System.arraycopy(fullPacket, 0, zeros, 0, zeros.length);
        fullPacket[zeros.length] = (byte)(reason >> 8);
        fullPacket[zeros.length + 1] = (byte)(reason & 0xff);
        fullPacket[zeros.length + 2] = (byte)(port >> 8);
        fullPacket[zeros.length + 3] = (byte)(port & 0xff);
        System.arraycopy(fullPacket, zeros.length + 4, payload, 0, payload.length);
        TransmitPacketOutputBuilder builder = new TransmitPacketOutputBuilder();
        builder.setResult(true);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfig(SetPipelineConfigInput input) {
        String host = input.getIp().getIpv4Address().getValue();
        Integer port = input.getPort().getValue();
        BigInteger deviceId = input.getDeviceId();
        ChannelImpl client = findClient(host, port, deviceId);
        if(client == null) {
            client = newClient(host, port, deviceId);
            String key = String.format("%s:%d:%d", host, port, deviceId);
            if(client.getState()) {
                clients.put(key, client);
            }
        }

        String logicFile = input.getLogicFile();
        String runtimeFile = input.getRuntimeFile();
        boolean result = false;
        try {
            result = client.setPipelineConfig(logicFile, runtimeFile, deviceId.longValue());
        } catch (IOException e) {
            LOG.info("Set pipeline config failed, reason = {}.", e.getMessage());
            e.printStackTrace();
        }

        SetPipelineConfigOutputBuilder builder = new SetPipelineConfigOutputBuilder();
        builder.setResult(result);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    public Future<RpcResult<QueryTargetStateOutput>> queryTargetState(QueryTargetStateInput input) {
        QueryTargetStateOutputBuilder builder = new QueryTargetStateOutputBuilder();
        //builder.setResults(new ArrayList<>());
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

}
