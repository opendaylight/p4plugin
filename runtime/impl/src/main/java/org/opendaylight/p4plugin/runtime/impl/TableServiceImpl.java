/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl;

import io.grpc.StatusRuntimeException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.p4plugin.runtime.impl.device.DeviceManager;
import org.opendaylight.p4plugin.runtime.impl.utils.NotificationPublisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class TableServiceImpl implements P4pluginTableService {
    private static final Logger LOG = LoggerFactory.getLogger(TableServiceImpl.class);
    private final DataBroker dataBroker;
    private final NotificationPublishService notificationPublishService;
    private DeviceManager deviceManager;
    private ExecutorService executorService;

    public TableServiceImpl(final DataBroker dataBroker,
                            final NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
    }

    public void init() {
        NotificationPublisher.getInstance().setNotificationService(notificationPublishService);
        executorService = Executors.newCachedThreadPool(new TableServiceThreadFactory());
        deviceManager = DeviceManager.getInstance();
    }

    public void close() {
        executorService.shutdown();
    }

    private String getErrMsg(StatusRuntimeException e) {
        return String.format("RPC exception, Status = %s, Reason = %s", e.getStatus(), e.getMessage());
    }

    private <T> RpcResult<T> rpcResultFailed(String errMsg) {
        return RpcResultBuilder.<T>failed()
                .withError(RpcError.ErrorType.APPLICATION, errMsg).build();
    }

    private <T> RpcResult<T> rpcResultSuccess(T value) {
        return RpcResultBuilder.success(value).build();
    }

    private Callable<RpcResult<Void>> addEntry(AddTableEntryInput input) {
        return () -> {
            String nodeId = input.getNid();
            deviceManager.findDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .addTableEntry(input);
            LOG.info("Add entry to device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> modifyEntry(ModifyTableEntryInput input) {
        return () -> {
            String nodeId = input.getNid();
            deviceManager.findDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .modifyTableEntry(input);
            LOG.info("Modify entry to device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> deleteEntry(DeleteTableEntryInput input) {
        return () -> {
            String nodeId = input.getNid();
            deviceManager.findDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .deleteTableEntry(input);
            LOG.info("Delete entry from device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<ReadTableEntryOutput>> readEntry(ReadTableEntryInput input) {
        return ()->{
            String nodeId = input.getNid();
            ReadTableEntryOutputBuilder outputBuilder = new ReadTableEntryOutputBuilder();
            List<String> result = deviceManager.findDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .readTableEntry(input.getTableName());
            outputBuilder.setEntry(result);
            LOG.info("Read entry from device = {} RPC success.", nodeId);
            return RpcResultBuilder.success(outputBuilder.build()).build();
        };
    }

    @Override
    public Future<RpcResult<Void>> addTableEntry(AddTableEntryInput input) {
        return executorService.submit(addEntry(input));
    }

    @Override
    public Future<RpcResult<Void>> modifyTableEntry(ModifyTableEntryInput input) {
        return executorService.submit(modifyEntry(input));
    }

    @Override
    public Future<RpcResult<Void>> deleteTableEntry(DeleteTableEntryInput input) {
        return executorService.submit(deleteEntry(input));
    }

    @Override
    public Future<RpcResult<ReadTableEntryOutput>> readTableEntry(ReadTableEntryInput input) {
        return executorService.submit(readEntry(input));
    }

    private static class TableServiceThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "TableServiceThread");
        }
    }
}
