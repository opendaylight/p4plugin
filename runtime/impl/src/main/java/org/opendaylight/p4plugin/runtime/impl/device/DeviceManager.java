/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.device;

import com.google.protobuf.ByteString;
import org.opendaylight.p4plugin.runtime.impl.utils.Utils;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
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
    private volatile ConcurrentHashMap<String, P4Device> devices = new ConcurrentHashMap<>();
    private DeviceManager() {}
    public static DeviceManager getInstance() {
        return singleton;
    }

    public boolean isNodeExist(String nodeId) {
        return devices.keySet().contains(nodeId);
    }

    public boolean isTargetExist(String ip, Integer port, Long deviceId) {
        Optional<String> keyContainer = devices.keySet()
                .stream()
                .filter(k -> devices.get(k).getDeviceId().equals(deviceId)
                          && devices.get(k).getIp().equals(ip)
                          && devices.get(k).getPort().equals(port))
                .findFirst();
        return keyContainer.isPresent();
    }

    public boolean isDeviceExist(String nodeId, String ip, Integer port, Long deviceId) {
        return isNodeExist(nodeId) && isTargetExist(ip, port, deviceId);
    }

    public Optional<P4Device> findDevice(String nodeId) {
        return Optional.ofNullable(devices.get(nodeId));
    }

    public synchronized void addDevice(String nodeId, Long deviceId, String ip, Integer port,
                          String runtimeFile, String configFile) throws IOException {
        if (isDeviceExist(nodeId, ip, port, deviceId)) {
            throw new IllegalArgumentException("Device is existed.");
        }

        P4Info p4Info = Utils.parseRuntimeInfo(runtimeFile);
        ByteString config = Utils.parseDeviceConfigInfo(configFile);
        P4Device.Builder builder = P4Device.newBuilder()
                .setNodeId(nodeId)
                .setDeviceId(deviceId)
                .setRuntimeInfo(p4Info)
                .setDeviceConfig(config)
                .setIp(ip)
                .setPort(port);
        devices.put(nodeId, builder.build());
    }

    public synchronized void removeDevice(String nodeId) {
        Optional<P4Device> optional = findDevice(nodeId);
        optional.ifPresent((device)->{
            device.shutdown();
            devices.remove(nodeId);
            LOG.info("Device = [{}] removed.", device.getNodeId());
        });
    }

    public Optional<P4Device> findConfiguredDevice(String nodeId) {
        Optional<P4Device> optional = findDevice(nodeId);

        if ((optional.isPresent()) && (optional.get().isConfigured())) {
            return optional;
        } else {
            LOG.info("Cannot find a configured device, node id = {}.", nodeId);
            return Optional.empty();
        }
    }

    public List<String> queryNodes() {
        List<String> result = new ArrayList<>();
        devices.keySet().forEach(node->{
            P4Device device = devices.get(node);
            result.add(device.toString());
        });
        return result;
    }
}
