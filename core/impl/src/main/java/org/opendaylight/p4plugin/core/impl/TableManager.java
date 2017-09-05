/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.read.entry.type.ReadType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.read.entry.type.read.type.ALLTABLES;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.read.entry.type.read.type.ONETABLE;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.inmemory.datastore.provider.rev140617.modules.module.configuration.InmemoryOperationalDatastoreProvider;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public class TableManager implements TableService {
    private static final Logger LOG = LoggerFactory.getLogger(TableManager.class);
    public TableManager() {}

    public static boolean doSetTableEntry(SetTableEntryInput input) {
        String host = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        String deviceConfig = input.getDeviceConfig();
        String runtimeInfo = input.getRuntimeInfo();
        Target target = null;
        WriteResponse response = null;

        try {
            target = DeviceManager.getTarget(host, port, deviceId, runtimeInfo, deviceConfig);
        } catch (IOException e) {
            LOG.info("Get target exception when set table entry, reason = {}.", e.getMessage());
            e.printStackTrace();
        }

        if (target != null) {
            WriteRequest.Builder request = WriteRequest.newBuilder();
            request.setDeviceId(input.getDeviceId().longValue());
            Update.Builder updateBuilder = Update.newBuilder();
            updateBuilder.setType(Update.Type.valueOf(input.getOperation().toString()));
            Entity.Builder entityBuilder = Entity.newBuilder();
            entityBuilder.setTableEntry(target.toMessage(input));
            updateBuilder.setEntity(entityBuilder.build());
            request.addUpdates(updateBuilder.build());
            response = target.write(request.build());
        }
        return response != null;
    }

    public static List<TableEntry> doGetTableEntry(GetTableEntryInput input) {
        String host = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        Target target = DeviceManager.findTarget(host, port, deviceId);
        ReadRequest.Builder request = ReadRequest.newBuilder();
        request.setDeviceId(deviceId);
        Entity.Builder entityBuilder = Entity.newBuilder();
        TableEntry.Builder entryBuilder = TableEntry.newBuilder();
        ReadType readType = input.getReadType();

        if (readType instanceof ALLTABLES) {
            entryBuilder.setTableId(0);
        } else if (readType instanceof ONETABLE) {
            entryBuilder.setTableId(target.getTableId(((ONETABLE)readType).getTable()));
        } else {
            return null;
        }

        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        Iterator<ReadResponse> responses = target.read(request.build());
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
    public Future<RpcResult<SetTableEntryOutput>> setTableEntry(SetTableEntryInput input) {
        SetTableEntryOutputBuilder builder = new SetTableEntryOutputBuilder();
        builder.setResult(doSetTableEntry(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetTableEntryOutput>> getTableEntry(GetTableEntryInput input) {
        GetTableEntryOutputBuilder builder = new GetTableEntryOutputBuilder();
        builder.setResult(true);
        List<TableEntry> entryList = doGetTableEntry(input);
        List<String> result = new ArrayList<>();
        Target target = DeviceManager.findTarget(input.getIp().getValue(),
                                                 input.getPort().getValue(),
                                                 input.getDeviceId().longValue());
        entryList.forEach(entry -> {
            String str = target.tableEntryToString(entry);
            result.add(str);
        });

        builder.setEntry(result);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
