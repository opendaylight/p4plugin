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
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.runtime.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class RuntimeServiceProvider implements P4pluginRuntimeService {
    private static final Logger LOG = LoggerFactory.getLogger(RuntimeServiceProvider.class);
    private final DataBroker dataBroker;
    private final NotificationPublishService notificationPublishService;
    private DeviceManager manager;
    private ExecutorService executorService;

    public RuntimeServiceProvider(final DataBroker dataBroker,
                                  final NotificationPublishService notificationPublishService) {
        this.dataBroker = dataBroker;
        this.notificationPublishService = notificationPublishService;
    }

    public void init() {
        NotificationPublisher.getInstance().setNotificationService(notificationPublishService);
        executorService = Executors.newFixedThreadPool(2);
        manager = DeviceManager.getInstance();
        LOG.info("P4Plugin runtime service provider initiated.");
    }

    public void close() {
        executorService.shutdown();
        LOG.info("P4Plugin runtime service provider closed.");
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
        return ()->{
            String nodeId = input.getNid();
            manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .addTableEntry(input);
            LOG.info("Add entry to device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> modifyEntry(ModifyTableEntryInput input) {
        return ()->{
            String nodeId = input.getNid();
            manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .modifyTableEntry(input);
            LOG.info("Modify entry to device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> deleteEntry(DeleteTableEntryInput input) {
        return ()->{
            String nodeId = input.getNid();
            manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .deleteTableEntry(input);
            LOG.info("Delete entry from device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> addMember(AddActionProfileMemberInput input) {
        return ()->{
            String nodeId = input.getNid();
            manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .addActionProfileMember(input);
            LOG.info("Add member to device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> modifyMember(ModifyActionProfileMemberInput input) {
        return ()->{
            String nodeId = input.getNid();
            manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .modifyActionProfileMember(input);
            LOG.info("Modify member to device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> deleteMember(DeleteActionProfileMemberInput input) {
        return ()->{
            String nodeId = input.getNid();
            manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .deleteActionProfileMember(input);
            LOG.info("Delete member from device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> addGroup(AddActionProfileGroupInput input) {
        return ()->{
            String nodeId = input.getNid();
            manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .addActionProfileGroup(input);
            LOG.info("Add group to device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> modifyGroup(ModifyActionProfileGroupInput input) {
        return ()->{
            String nodeId = input.getNid();
            manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .modifyActionProfileGroup(input);
            LOG.info("Modify group to device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<Void>> deleteGroup(DeleteActionProfileGroupInput input) {
        return ()->{
            String nodeId = input.getNid();
            manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .deleteActionProfileGroup(input);
            LOG.info("Delete group from device = {} RPC success.", nodeId);
            return rpcResultSuccess(null);
        };
    }

    private Callable<RpcResult<ReadTableEntryOutput>> readEntry(ReadTableEntryInput input) {
        return ()->{
            String nodeId = input.getNid();
            ReadTableEntryOutputBuilder outputBuilder = new ReadTableEntryOutputBuilder();
            List<String> result = manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .readTableEntry(input.getTableName());
            outputBuilder.setEntry(result);
            LOG.info("Read entry from device = {} RPC success.", nodeId);
            return RpcResultBuilder.success(outputBuilder.build()).build();
        };
    }

    private Callable<RpcResult<ReadActionProfileMemberOutput>> readMember(ReadActionProfileMemberInput input) {
        return ()->{
            String nodeId = input.getNid();
            ReadActionProfileMemberOutputBuilder outputBuilder = new ReadActionProfileMemberOutputBuilder();
            List<String> result = manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .readActionProfileMember(input.getActionProfileName());
            outputBuilder.setMember(result);
            LOG.info("Read member from device = {} RPC success.", nodeId);
            return RpcResultBuilder.success(outputBuilder.build()).build();
        };
    }

    private Callable<RpcResult<ReadActionProfileGroupOutput>> readGroup(ReadActionProfileGroupInput input) {
        return ()->{
            String nodeId = input.getNid();
            ReadActionProfileGroupOutputBuilder outputBuilder = new ReadActionProfileGroupOutputBuilder();
            List<String> result = manager.findConfiguredDevice(nodeId)
                    .orElseThrow(IllegalArgumentException::new)
                    .readActionProfileGroup(input.getActionProfileName());
            outputBuilder.setGroup(result);
            LOG.info("Read group from device = {} RPC success.", nodeId);
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
    public Future<RpcResult<Void>> addActionProfileMember(AddActionProfileMemberInput input) {
        return executorService.submit(addMember(input));
    }

    @Override
    public Future<RpcResult<Void>> modifyActionProfileMember(ModifyActionProfileMemberInput input) {
        return executorService.submit(modifyMember(input));
    }

    @Override
    public Future<RpcResult<Void>> deleteActionProfileMember(DeleteActionProfileMemberInput input) {
        return executorService.submit(deleteMember(input));
    }

    @Override
    public Future<RpcResult<Void>> addActionProfileGroup(AddActionProfileGroupInput input) {
        return executorService.submit(addGroup(input));
    }

    @Override
    public Future<RpcResult<Void>> modifyActionProfileGroup(ModifyActionProfileGroupInput input) {
        return executorService.submit(modifyGroup(input));
    }

    @Override
    public Future<RpcResult<Void>> deleteActionProfileGroup(DeleteActionProfileGroupInput input) {
        return executorService.submit(deleteGroup(input));
    }

    @Override
    public Future<RpcResult<ReadTableEntryOutput>> readTableEntry(ReadTableEntryInput input) {
        return executorService.submit(readEntry(input));
    }

    @Override
    public Future<RpcResult<ReadActionProfileMemberOutput>> readActionProfileMember(ReadActionProfileMemberInput input) {
        return executorService.submit(readMember(input));
    }

    @Override
    public Future<RpcResult<ReadActionProfileGroupOutput>> readActionProfileGroup(ReadActionProfileGroupInput input) {
        return executorService.submit(readGroup(input));
    }
}
