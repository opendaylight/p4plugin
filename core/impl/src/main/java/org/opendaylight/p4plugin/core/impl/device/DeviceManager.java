/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.device;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import org.opendaylight.p4plugin.core.impl.utils.Utils;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Device manager is used to save the device info and provides the add/remove,
 * find, query and other methods, only one instance.
 */
public class DeviceManager {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceManager.class);
    private static DeviceManager singleton = new DeviceManager();
    private ConcurrentHashMap<String, P4Device> devices = new ConcurrentHashMap<>(); //nodeId<->P4Device
    private DeviceManager() {}
    public static DeviceManager getInstance() {
        return singleton;
    }

    public boolean isNodeExist(String nodeId) {
        Optional<String> keyContainer = devices.keySet()
                .stream()
                .filter(k -> k.equals(nodeId))
                .findFirst();
        return keyContainer.isPresent();
    }

    public boolean isDeviceExist(String ip, Integer port, Long deviceId) {
        Optional<String> keyContainer = devices.keySet()
                .stream()
                .filter(k -> devices.get(k).getDeviceId().equals(deviceId)
                          && devices.get(k).getIp().equals(ip)
                          && devices.get(k).getPort().equals(port))
                .findFirst();
        return keyContainer.isPresent();
    }

    public boolean isDuplicateDevice(String nodeId, String ip, Integer port, Long deviceId) {
        return isNodeExist(nodeId) && isDeviceExist(ip, port, deviceId);
    }

    public P4Device findDevice(String nodeId) {
        P4Device device = null;
        Optional<String> keyContainer = devices.keySet()
                .stream()
                .filter(k->k.equals(nodeId))
                .findFirst();
        if (keyContainer.isPresent()) {
            device = devices.get(nodeId);
        }
        return device;
    }

    private P4Device newDevice(String nodeId, Long deviceId, String ip, Integer port,
                               String runtimeFile, String configFile) throws IOException {
        P4Info p4Info = Utils.parseRuntimeInfo(runtimeFile);
        ByteString config = Utils.parseDeviceConfigInfo(configFile);
        P4Device.Builder builder = P4Device.newBuilder()
                .setNodeId(nodeId)
                .setDeviceId(deviceId)
                .setRuntimeInfo(p4Info)
                .setDeviceConfig(config)
                .setIp(ip)
                .setPort(port);
        return builder.build();
    }

    public P4Device addDevice(String nodeId, Long deviceId, String ip, Integer port,
                              String runtimeFile, String configFile) throws IOException {
        Preconditions.checkArgument(runtimeFile != null, "Runtime file is null.");
        Preconditions.checkArgument(configFile != null, "Config file is null.");
        String description = String.format("%s:%d:%s:%d", nodeId, deviceId, ip, port);

        if (isDuplicateDevice(nodeId, ip, port, deviceId)) {
            LOG.info("Duplicate device = {}.", description);
            return findDevice(nodeId);
        }

        if (isNodeExist(nodeId) || isDeviceExist(ip, port, deviceId)) {
            LOG.info("Device = {} node or device is already existed.", description);
            return null;
        }

        P4Device device = newDevice(nodeId, deviceId, ip, port, runtimeFile, configFile);
        if (device.connectToDevice()) {
            device.setDeviceState(P4Device.State.Connected);
            devices.put(nodeId, device);
            LOG.info("Add device = {} success.", description);
            return device;
        }

        LOG.info("Connect to device = {} failed.", description);
        return null;
    }

    public void removeDevice(String nodeId) {
        P4Device device = findDevice(nodeId);
        if (device != null) {
            device.shutdown();
            devices.remove(nodeId);
            LOG.info("Device = {} removed.", device.getDescription());
        }
    }

    public P4Device findConfiguredDevice(String nodeId) {
        P4Device device = findDevice(nodeId);
        if (device != null && device.isConfigured()) {
            return device;
        }
        LOG.info("Cannot find a configured device, node id = {}", nodeId);
        return null;
    }

    public List<String> queryNodes() {
        List<String> result = new ArrayList<>();
        devices.keySet().forEach(node->{
            P4Device device = devices.get(node);
            result.add(device.getDescription());
        });
        return result;
    }
}
