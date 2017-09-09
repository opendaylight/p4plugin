/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.p4plugin.core.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ResourceManager {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);
    static ConcurrentHashMap<String, GrpcChannel> channels = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, P4Device> devices = new ConcurrentHashMap<>();
    private ResourceManager() {}

    public static GrpcChannel findChannel(String ip, Integer port) {
        String key = String.format("%s:%d", ip, port);
        GrpcChannel channel = null;
        Optional<String> keyContainer = channels.keySet()
                .stream()
                .filter(k->k.equals(key))
                .findFirst();

        if (keyContainer.isPresent()) {
            channel = channels.get(key);
        }

        return channel;
    }

    public static GrpcChannel getChannel(String ip, Integer port) {
        GrpcChannel channel = findChannel(ip, port);
        if (channel == null) {
            channel = new GrpcChannel(ip, port);
        }

        if (channel.getChannelState()) {
            channels.put(String.format("%s:%d", ip, port), channel);
        } else {
            channel.shutdown();
            channel = null;
        }

        return channel;
    }

    public static void removeChannel(String ip, Integer port) {
        String key = String.format("%s:%d", ip, port);
        channels.keySet()
                .stream()
                .filter(k -> k.equals(key))
                .collect(Collectors.toList())
                .forEach(k->{
                    channels.get(k).shutdown();
                    channels.remove(k);
                });
    }

    public static P4Device findDevice(String ip, Integer port, Long deviceId) {
        String key = String.format("%s:%d:%d", ip, port, deviceId);
        P4Device device = null;
        Optional<String> keyContainer = devices.keySet().stream().filter(k->k.equals(key)).findFirst();

        if (keyContainer.isPresent()) {
            device = devices.get(key);
        }

        return device;
    }

    public static P4Device getDevice(String ip, Integer port, Long deviceId,
                                     String runtimeInfo, String deviceConfig) throws IOException {
        P4Device device = findDevice(ip, port, deviceId);

        if (device == null) {
            device = newDevice(ip, port, deviceId, runtimeInfo, deviceConfig);
            if (device != null) {
                device.setDeviceState(P4Device.State.Connected);
                devices.put(String.format("%s:%d:%d", ip, port, deviceId), device);
                device.sendMasterArbitration();
            }
        } else {
            if (runtimeInfo != null) {
                device.setRuntimeInfo(runtimeInfo);
                device.setDeviceState(P4Device.State.Connected);
            }

            if (deviceConfig != null) {
                device.setDeviceConfig(deviceConfig);
                device.setDeviceState(P4Device.State.Connected);
            }
        }

        return device;
    }

    public static P4Device newDevice(String ip, Integer port, Long deviceId,
                                     String runtimeInfo, String deviceConfig) throws IOException {
        GrpcChannel channel = getChannel(ip, port);
        P4Device device = null;
        if (channel != null) {
            P4Device.Builder builder = P4Device.newBuilder()
                    .setChannel(channel)
                    .setDeviceId(deviceId)
                    .setRuntimeInfo(runtimeInfo)
                    .setDeviceConfig(deviceConfig);
            device = builder.build();
        }
        return device;
    }

    public static void removeDevices(String ip, Integer port) {
        String deviceKey = String.format("%s:%d:.*", ip, port);
        devices.keySet()
                .stream()
                .filter(k->k.matches(deviceKey))
                .collect(Collectors.toList())
                .forEach(devices::remove);
    }
}
