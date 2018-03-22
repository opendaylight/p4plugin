/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DeviceManager {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceManager.class);
    private static DeviceManager singleton = new DeviceManager();
    private ConcurrentHashMap<String, Device> devices = new ConcurrentHashMap<>();
    private DeviceManager() {}
    public static DeviceManager getInstance() {
        return singleton;
    }

    public boolean isNodeIdExist(String nodeId) {
        return devices.keySet().contains(nodeId);
    }

    public boolean isTargetExist(Long deviceId, String ip, Integer port) {
        Optional<String> keys = devices.keySet()
                .stream()
                .filter(k -> devices.get(k).deviceId.equals(deviceId)
                          && devices.get(k).serverAddress.getIp().equals(ip)
                          && devices.get(k).serverAddress.getPort().equals(port))
                .findFirst();
        return keys.isPresent();
    }

    public boolean isDeviceExist(String nodeId, Long deviceId, String ip, Integer port) {
        return isNodeIdExist(nodeId) || isTargetExist(deviceId, ip, port);
    }

    public Optional<Device> findDevice(String nodeId) {
        return Optional.ofNullable(devices.get(nodeId));
    }

    public void addDevice(String nodeId, Long deviceId, String ip, Integer port,
                             String configFile, String runtimeFile) throws IOException {
        if (isDeviceExist(nodeId, deviceId, ip, port)) {
            LOG.info("Device = {}/{} is already existed.", nodeId, String.format("%d:%s:%s", deviceId, ip, port));
            throw new IllegalArgumentException("Device is already existed.");
        }

        Device device = new Device(nodeId, deviceId,
                                   new ServerAddress(ip, port),
                                   new PipelineConfig(configFile, runtimeFile));
        devices.put(nodeId, device);
    }

    public void removeDevice(String nodeId) {
        Optional<Device> optional = findDevice(nodeId);
        optional.ifPresent((device) -> {
            device.shutdown();
            devices.remove(nodeId);
            LOG.info("Device = {} removed.", device.nodeId);
        });
    }

    public Map<String, Device> queryDevices() {
        return devices;
    }
}
