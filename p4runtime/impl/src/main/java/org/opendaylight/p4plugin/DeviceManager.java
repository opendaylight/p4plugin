/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private boolean isNodeIdExist(String nodeId) {
        return devices.keySet().contains(nodeId);
    }

    private boolean isServerAddressExist(String ip, Integer port, Long deviceId) {
        Optional<String> keyContainer = devices.keySet()
                .stream()
                .filter(k -> devices.get(k).getDeviceId().equals(deviceId)
                          && devices.get(k).getIp().equals(ip)
                          && devices.get(k).getPort().equals(port))
                .findFirst();
        return keyContainer.isPresent();
    }

    public boolean isDeviceExist(String nodeId, String ip, Integer port, Long deviceId) {
        return isNodeIdExist(nodeId) || isServerAddressExist(ip, port, deviceId);
    }

    public Optional<Device> findDevice(String nodeId) {
        return Optional.ofNullable(devices.get(nodeId));
    }

    public synchronized Status addDevice(String nodeId, Long deviceId, String ip, Integer port,
                                         String p4InfoFile, String configFile) throws IOException {
        if (isDeviceExist(nodeId, ip, port, deviceId)) {
            return Status.DEVICE_ALREADY_EXIST;
        }

        Status status = Status.DEVICE_ADD_SUCCESS;
        try {
            Device.Builder builder = Device.newBuilder()
                    .setNodeId(nodeId)
                    .setDeviceId(deviceId)
                    .setP4InfoFile(p4InfoFile)
                    .setDeviceConfigFile(configFile)
                    .setIp(ip)
                    .setPort(port);
            Device device = builder.build();
            devices.put(nodeId, device);
            LOG.info("Device add success, nodeId = {}.", device.getNodeId());
        } catch (IOException e) {
            e.printStackTrace();
            LOG.error("ZTE {} / {}", e.getMessage(),e.getCause());
            status = Status.PARSE_FILE_EXCEPTION;
        } catch (IllegalArgumentException e) {
            status =  Status.INVALID_ARGUMENTS;
        }
        return status;
    }

    public synchronized void removeDevice(String nodeId) {
        Optional<Device> optional = findDevice(nodeId);
        optional.ifPresent((device) -> {
            device.closeStreamChannel();
            devices.remove(nodeId);
            LOG.info("Device removed, nodeId = {}.", device.getNodeId());
        });
    }

    public List<String> queryNodes() {
        List<String> result = new ArrayList<>();
        devices.keySet().forEach(node -> {
            Device device = devices.get(node);
            result.add(device.toString());
        });
        return result;
    }

    public enum Status {
        DEVICE_ALREADY_EXIST(0),
        PARSE_FILE_EXCEPTION(1),
        INVALID_ARGUMENTS(2),
        DEVICE_ADD_SUCCESS(3);
        private int value;

        Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
