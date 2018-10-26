/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.p4plugin.device.DeviceManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class DeviceServiceProvider implements P4pluginDeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceServiceProvider.class);
    private final DataBroker dataBroker;
    private DeviceManager deviceManager;
    private ListeningExecutorService executorService;

    public DeviceServiceProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
        deviceManager = DeviceManager.getInstance();
        LOG.info("P4 plugin device service provider initiated.");
    }

    public void close() {
        executorService.shutdown();
        LOG.info("P4 plugin device service provider closed.");
    }

    private <T> RpcResult<T> rpcResultSuccess(T value) {
        return RpcResultBuilder.success(value).build();
    }

    private Callable<RpcResult<AddDeviceOutput>> addDev(AddDeviceInput input) {
        return () -> {
            String nodeId = input.getNid();
            String ip = input.getIp().getValue();
            Integer port = input.getPort().getValue();
            Long deviceId = input.getDid().longValue();
            String runtimeFile = input.getRuntimeFile();
            String configFile = input.getPipelineFile();
            DeviceManager.Status status = deviceManager.addDevice(nodeId, deviceId, ip, port, runtimeFile, configFile);
            LOG.info("Add device = {}-{}-{}:{}-{}-{} status = {}.", nodeId, deviceId, ip, port, runtimeFile, configFile, status);
            AddDeviceOutputBuilder outputBuilder = new AddDeviceOutputBuilder();
            AddDeviceOutput.Status outputStatus = AddDeviceOutput.Status.forValue(status.getValue());
            outputBuilder.setStatus(outputStatus);
            return rpcResultSuccess(outputBuilder.build());
        };
    }

    private Callable<RpcResult<RemoveDeviceOutput>> removeDev(RemoveDeviceInput input) {
        return () -> {
            deviceManager.removeDevice(input.getNid());
            LOG.info("Remove device success, nodeId = {}.", input.getNid());
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<QueryDevicesOutput>> queryDev() {
        return () -> {
            QueryDevicesOutputBuilder outputBuilder = new QueryDevicesOutputBuilder();
            outputBuilder.setNode(deviceManager.queryNodes());
            LOG.info("Query devices success.");
            return rpcResultSuccess(outputBuilder.build());
        };
    }

    @Override
    public ListenableFuture<RpcResult<AddDeviceOutput>> addDevice(AddDeviceInput input) {
        return executorService.submit(addDev(input));
    }

    @Override
    public ListenableFuture<RpcResult<QueryDevicesOutput>> queryDevices(QueryDevicesInput input) {
        return executorService.submit(queryDev());
    }

    @Override
    public ListenableFuture<RpcResult<RemoveDeviceOutput>> removeDevice(RemoveDeviceInput input) {
        return executorService.submit(removeDev(input));
    }
}
