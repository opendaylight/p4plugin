/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

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

public class SimpleRouterRunner {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleRouterRunner.class);
    private SimpleRouter simpleRouter;
    private P4pluginDeviceService deviceService;
    private P4pluginP4runtimeService runtimeService;

    public SimpleRouterRunner(final P4pluginDeviceService deviceService,
                              final P4pluginP4runtimeService runtimeService,
                              final String gRPCServerIp,
                              final Integer gRPCServerPort,
                              final Long deviceId,
                              final String nodeId,
                              final String configFile,
                              final String runtimeFile) {
        this.deviceService = deviceService;
        this.runtimeService = runtimeService;
        this.simpleRouter = SimpleRouter.newBuilder().setServerIp(gRPCServerIp)
                .setServerPort(gRPCServerPort)
                .setDeviceId(deviceId)
                .setNodeId(nodeId)
                .setRuntimeFile(runtimeFile)
                .setConfigFile(configFile)
                .setRuntimeService(runtimeService).build();
    }

    public void run() {
        if (addDevice()) {
            simpleRouter.openStreamChannel();
            simpleRouter.setPipelineConfig();
            simpleRouter.addTableEntry();
        }
    }

    public void close() {
        removeDevice();
    }

    private boolean removeDevice() {
        RemoveDeviceInputBuilder inputBuilder = new RemoveDeviceInputBuilder();
        inputBuilder.setNid(simpleRouter.getNodeId());
        boolean result;

        try {
            ListenableFuture<RpcResult<RemoveDeviceOutput>> output = deviceService.removeDevice(inputBuilder.build());
            result = output.get().isSuccessful();
            LOG.info("Simple router remove {}.", result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Simple router remove exception, message = {}.", e.getMessage());
        }
        return result;
    }

    private boolean addDevice() {
        AddDeviceInputBuilder inputBuilder = new AddDeviceInputBuilder();
        inputBuilder.setNid(simpleRouter.getNodeId());
        inputBuilder.setDid(new BigInteger(simpleRouter.getDeviceId().toString()));
        inputBuilder.setIp(new Ipv4Address(simpleRouter.getServerIp()));
        inputBuilder.setPort(new PortNumber(simpleRouter.getServerPort()));
        inputBuilder.setPipelineFile(simpleRouter.getConfigFile());
        inputBuilder.setRuntimeFile(simpleRouter.getRuntimeFile());
        boolean result;

        try {
            ListenableFuture<RpcResult<AddDeviceOutput>> output = deviceService.addDevice(inputBuilder.build());
            result = output.get().isSuccessful();
            LOG.info("Simple router add {}.", result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Simple router add exception, message = {}.", e.getMessage());
        }
        return result;
    }
}
