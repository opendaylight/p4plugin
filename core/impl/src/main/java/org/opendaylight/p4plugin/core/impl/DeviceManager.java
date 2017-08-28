/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.DeviceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.SetPipelineConfigInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.SetPipelineConfigOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.SetPipelineConfigOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class DeviceManager implements DeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceManager.class);
    static ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();

    public DeviceManager() {}
    
    /**
     * Create a new client according to host:port:deviceId, then send a master arbitration message;
     * @param host ip addres;
     * @param port tcp port;
     * @param deviceId device id which can be set when starting bmv2 using --device-id, default is 0;
     * @return A new client, when gRPC server is down, the client.getState() function will return false;
     */
    public static Channel newChannel(String host, Integer port, BigInteger deviceId,
                                     String deviceConfig, String runtimeInfo) {
        Channel channel = new Channel(host, port);
        try {
            if (deviceConfig != null) {//for tofino test, we can only transport runtime info to tofino.
                channel.setDeviceConfig(Utils.parseDeviceConfigInfo(deviceConfig));
            }
            channel.setRuntimeInfo(Utils.parseRuntimeInfo(runtimeInfo));
            //send master arbitration, just as a handshake
            channel.sendMasterArbitration(deviceId.longValue());
            return channel;
        } catch (IOException e) {
            LOG.info("IOException, reason = {}.", e.getMessage());
        }
        return null;
    }
    
    public static void addNewChannelToMap(String key, Channel channel) {
        if(channel.getState()) {
            channels.put(key, channel);
        }
    }
    
    public static Channel findChannel(String host, Integer port, BigInteger deviceId) {
        String key = String.format("%s:%d:%d", host, port, deviceId);
        Channel channel = null;
        for(String k : channels.keySet()) {
            if(k.equals(key)) {
                channel = channels.get(k);
                break;
            }
        }
        return channel;
    }
    
    public boolean doSetPipelineConfig(SetPipelineConfigInput input) {
        String host = input.getIP().getIpv4Address().getValue();
        Integer port = input.getPort().getValue();
        BigInteger deviceId = input.getDeviceID();
        String deviceConfig = input.getDeviceConfig();
        String runtimeInfo = input.getRuntimeInfo();

        Channel channel = findChannel(host, port, deviceId);
        if(channel == null) {
            channel = newChannel(host, port, deviceId, deviceConfig, runtimeInfo);
            String key = String.format("%s:%d:%d", host, port, deviceId);
            if(channel.getState()) {
                channels.put(key, channel);
            }
        }

        SetForwardingPipelineConfigResponse response;
        try {
            response = channel.setPipelineConfig(deviceConfig, runtimeInfo, deviceId.longValue());
        } catch (IOException e) {
            LOG.info("Set pipeline config failed, reason = {}.", e.getMessage());
            response = null;
        }

        return response == null ? false : true;
    }

    @Override
    public Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfig(SetPipelineConfigInput input) {
        SetPipelineConfigOutputBuilder builder = new SetPipelineConfigOutputBuilder();
        builder.setResult(doSetPipelineConfig(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
