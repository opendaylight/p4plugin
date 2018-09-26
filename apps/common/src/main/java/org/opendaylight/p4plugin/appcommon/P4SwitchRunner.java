/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.appcommon;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

public abstract class P4SwitchRunner {
    private static final Logger LOG = LoggerFactory.getLogger(P4SwitchRunner.class);
    protected P4Switch p4Switch;
    private P4pluginDeviceService deviceService;
    private P4pluginP4runtimeService runtimeService;

    public P4SwitchRunner(final P4pluginDeviceService deviceService,
                          final P4pluginP4runtimeService runtimeService,
                          final String gRPCServerIp,
                          final Integer gRPCServerPort,
                          final Long deviceId,
                          final String nodeId,
                          final String configFile,
                          final String runtimeFile) {
        this.deviceService = deviceService;
        this.runtimeService = runtimeService;
        this.p4Switch = newSwitch(gRPCServerIp, gRPCServerPort, deviceId, nodeId, configFile, runtimeFile, runtimeService);
    }

    public abstract P4Switch newSwitch(String gRPCServerIp,
                                       Integer gRPCServerPort,
                                       Long deviceId,
                                       String nodeId,
                                       String configFile,
                                       String runtimeFile,
                                       P4pluginP4runtimeService runtimeService);
    public abstract void run();
    public void close() {
        removeDevice();
    }

    public boolean addDevice() {
        AddDeviceInputBuilder inputBuilder = new AddDeviceInputBuilder();
        inputBuilder.setNid(p4Switch.getNodeId());
        inputBuilder.setDid(new BigInteger(p4Switch.getDeviceId().toString()));
        inputBuilder.setIp(new Ipv4Address(p4Switch.getServerIp()));
        inputBuilder.setPort(new PortNumber(p4Switch.getServerPort()));
        inputBuilder.setPipelineFile(p4Switch.getConfigFile());
        inputBuilder.setRuntimeFile(p4Switch.getRuntimeFile());
        String nodeId = p4Switch.getNodeId();
        boolean result;

        try {
            ListenableFuture<RpcResult<AddDeviceOutput>> output = deviceService.addDevice(inputBuilder.build());
            result = output.get().isSuccessful();
            LOG.info("Add switch {} {}.", nodeId, result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Add switch {} exception, message = {}.", nodeId, e.getMessage());
        }
        return result;
    }

    public boolean removeDevice() {
        RemoveDeviceInputBuilder inputBuilder = new RemoveDeviceInputBuilder();
        String nodeId = p4Switch.getNodeId();
        inputBuilder.setNid(nodeId);
        boolean result;

        try {
            ListenableFuture<RpcResult<RemoveDeviceOutput>> output = deviceService.removeDevice(inputBuilder.build());
            result = output.get().isSuccessful();
            LOG.info("Remove switch {} {}.", nodeId, result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Remove switch {} exception, message = {}.", nodeId, e.getMessage());
        }
        return result;
    }
}
