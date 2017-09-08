/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.TextFormat;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.read.entry.type.ReadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.read.entry.type.read.type.ALLTABLES;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.read.entry.type.read.type.ONETABLE;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public class ServiceImpl implements P4pluginCoreService {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceImpl.class);
    public static boolean doSetPipelineConfig(SetPipelineConfigInput input) {
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        String deviceConfig = input.getDeviceConfig();
        String runtimeInfo = input.getRuntimeInfo();
        SetForwardingPipelineConfigResponse response = null;
        P4Device device = null;

        try {
            device = Manager.getDevice(ip, port, deviceId, runtimeInfo, deviceConfig);
            if (device != null) {
                if (device.getDeviceState() != P4Device.State.Unknown) {
                    response = device.setPipelineConfig();
                }

                if (response != null) {
                    device.setDeviceState(P4Device.State.Configured);
                }
            }
            return response != null;
        } catch (IOException e) {
            LOG.info("IO Exception, reason = {}", e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public static boolean doGetPipelineConfig(GetPipelineConfigInput input) {
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        String outputFile = input.getOutputFile();
        File file = new File(outputFile);
        FileOutputStream outputStream;
        PrintStream printStream;
        P4Device device = Manager.findDevice(ip, port, deviceId);

        if ((device == null) || (device.getDeviceState() != P4Device.State.Configured)) {
            return false;
        }

        GetForwardingPipelineConfigResponse response = device.getPipelineConfig();
        if (response != null) {
            ForwardingPipelineConfig config = response.getConfigs(0);
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                outputStream = new FileOutputStream(outputFile);
                printStream = new PrintStream(outputStream);
                printStream.println(TextFormat.printToString(config.getP4Info()));
                outputStream.close();
                printStream.close();
                return true;
            } catch (Exception e) {
                LOG.info("Write output file exception, file = {}, reason = {}", outputFile, e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }


    public static boolean doSetTableEntry(SetTableEntryInput input) {
        String host = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        WriteResponse response = null;
        P4Device device = Manager.findDevice(host, port, deviceId);

        if ((device != null) && (device.getDeviceState() == P4Device.State.Configured)) {
            WriteRequest.Builder request = WriteRequest.newBuilder();
            request.setDeviceId(input.getDeviceId().longValue());
            Update.Builder updateBuilder = Update.newBuilder();
            updateBuilder.setType(Update.Type.valueOf(input.getOperation().toString()));
            Entity.Builder entityBuilder = Entity.newBuilder();
            entityBuilder.setTableEntry(device.toMessage(input));
            updateBuilder.setEntity(entityBuilder.build());
            request.addUpdates(updateBuilder.build());
            response = device.write(request.build());
        } else {
            LOG.info("Set table entry error, cannot find configured target");
        }

        return response != null;
    }

    public static List<String> doGetTableEntryStr(GetTableEntryInput input) {
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        P4Device device = Manager.findDevice(ip, port, deviceId);

        if ((device == null) || (device.getDeviceState() != P4Device.State.Configured)) {
            return null;
        }

        ReadRequest.Builder request = ReadRequest.newBuilder();
        request.setDeviceId(deviceId);
        Entity.Builder entityBuilder = Entity.newBuilder();
        TableEntry.Builder entryBuilder = TableEntry.newBuilder();
        ReadType readType = input.getReadType();

        if ((readType instanceof ALLTABLES) || (readType == null)) {
            entryBuilder.setTableId(0);
        } else if (readType instanceof ONETABLE) {
            ONETABLE oneTable = (ONETABLE)readType;
            String tableName = oneTable.getTable();
            int tableId = device.getTableId(tableName);
            entryBuilder.setTableId(tableId);
        } else {
            return null;
        }

        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        Iterator<ReadResponse> responses = device.read(request.build());
        List<TableEntry> entryList = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                TableEntry entry = entity.getTableEntry();
                entryList.add(entry);
            });
            if (isCompleted) break;
        }

        List<String> result = new ArrayList<>();
        entryList.forEach(entry -> {
            String str = device.tableEntryToString(entry);
            result.add(str);
        });
        return result;
    }

    public static List<TableEntry> doGetTableEntry(GetTableEntryInput input) {
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        P4Device device = Manager.findDevice(ip, port, deviceId);

        if ((device == null) || (device.getDeviceState() != P4Device.State.Configured)) {
            return null;
        }

        ReadRequest.Builder request = ReadRequest.newBuilder();
        request.setDeviceId(deviceId);
        Entity.Builder entityBuilder = Entity.newBuilder();
        TableEntry.Builder entryBuilder = TableEntry.newBuilder();
        ReadType readType = input.getReadType();

        if ((readType instanceof ALLTABLES) || (readType == null)) {
            entryBuilder.setTableId(0);
        } else if (readType instanceof ONETABLE) {
            ONETABLE oneTable = (ONETABLE)readType;
            String tableName = oneTable.getTable();
            int tableId = device.getTableId(tableName);
            entryBuilder.setTableId(tableId);
        } else {
            return null;
        }

        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        Iterator<ReadResponse> responses = device.read(request.build());
        List<TableEntry> result = new ArrayList<>();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();
            entityList.forEach(entity-> {
                TableEntry entry = entity.getTableEntry();
                result.add(entry);
            });
            if (isCompleted) break;
        }
        return result;
    }

    @Override
    public Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfig(SetPipelineConfigInput input) {
        SetPipelineConfigOutputBuilder builder = new SetPipelineConfigOutputBuilder();
        builder.setResult(doSetPipelineConfig(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetPipelineConfigOutput>> getPipelineConfig(GetPipelineConfigInput input) {
        GetPipelineConfigOutputBuilder builder = new GetPipelineConfigOutputBuilder();
        builder.setResult(doGetPipelineConfig(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<SetTableEntryOutput>> setTableEntry(SetTableEntryInput input) {
        SetTableEntryOutputBuilder builder = new SetTableEntryOutputBuilder();
        builder.setResult(doSetTableEntry(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetTableEntryOutput>> getTableEntry(GetTableEntryInput input) {
        GetTableEntryOutputBuilder builder = new GetTableEntryOutputBuilder();
        List<String> result = doGetTableEntryStr(input);

        if (result == null) {
            builder.setResult(false);
        } else  {
            builder.setTotal(String.valueOf(result.size()));
            builder.setEntry(result);
            builder.setResult(true);
        }

        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
