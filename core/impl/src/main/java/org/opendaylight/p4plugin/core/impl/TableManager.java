/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.util.concurrent.Futures;
import com.google.protobuf.ByteString;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.read.table.entry.input.type.ALL;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.read.table.entry.input.type.ONE;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.core.general.entity.rev150820.EntityBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import static org.opendaylight.p4plugin.p4runtime.proto.FieldMatch.FieldMatchTypeCase.EXACT;

public class TableManager implements TableService {
    private static final Logger LOG = LoggerFactory.getLogger(TableManager.class);
    public TableManager() {}
    
    public boolean doPopulateTableEntry(PopulateTableEntryInput input) {
        String host = input.getIP().getIpv4Address().getValue();
        Integer port = input.getPort().getValue();
        BigInteger deviceId = input.getDeviceID();
        String deviceConfig = input.getDeviceConfig();
        String runtimeInfo = input.getRuntimeInfo();
        String key = String.format("%s:%d:%d", host, port, deviceId);
        Channel channel = DeviceManager.findChannel(host, port, deviceId);

        if(channel == null) {
            channel = DeviceManager.newChannel(host, port, deviceId, deviceConfig, runtimeInfo);
            DeviceManager.addNewChannelToMap(key, channel);
        }

        WriteRequest.Builder request = WriteRequest.newBuilder();
        request.setDeviceId(input.getDeviceID().longValue());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.valueOf(input.getOperation().toString()));

        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(Utils.toMessage(channel.getRuntimeInfo(), input));
        updateBuilder.setEntity(entityBuilder.build());
        request.addUpdates(updateBuilder.build());

        WriteResponse response = channel.write(request.build());
        return response == null ? false : true;
    }

    boolean doReadTableEntry(ReadTableEntryInput input, List<String> result) {
        String host = input.getIP().getIpv4Address().getValue();
        Integer port = input.getPort().getValue();
        BigInteger deviceId = input.getDeviceID();
        String deviceConfig = input.getDeviceConfig();
        String runtimeInfo = input.getRuntimeInfo();
        String key = String.format("%s:%d:%d", host, port, deviceId);
        Channel channel = DeviceManager.findChannel(host, port, deviceId);

        if (channel == null) {
            channel = DeviceManager.newChannel(host, port, deviceId, deviceConfig, runtimeInfo);
            DeviceManager.addNewChannelToMap(key, channel);
        }

        ReadRequest.Builder request = ReadRequest.newBuilder();
        request.setDeviceId(input.getDeviceID().longValue());
        Entity.Builder entityBuilder = Entity.newBuilder();
        TableEntry.Builder entryBuilder = TableEntry.newBuilder();
        if (input.getType() instanceof ALL) {
            entryBuilder.setTableId(0);
        } else if (input.getType() instanceof ONE) {
            entryBuilder.setTableId(Utils.getTableId(channel.getRuntimeInfo(),((ONE)input.getType()).getTable()));
        }

        entityBuilder.setTableEntry(entryBuilder);
        request.addEntities(entityBuilder);
        Iterator<ReadResponse> responses = channel.read(request.build());
        P4Info runtime = channel.getRuntimeInfo();

        while (responses.hasNext()) {
            ReadResponse response = responses.next();
            List<Entity> entityList = response.getEntitiesList();
            boolean isCompleted = response.getComplete();

            entityList.forEach(entity->{
                TableEntry entry = entity.getTableEntry();
                Action action = entry.getAction().getAction();
                int tableId = entry.getTableId();
                int actionId = action.getActionId();
                List<Action.Param> paramList = action.getParamsList();
                StringBuffer buffer = new StringBuffer();
                paramList.forEach(param->{
                    buffer.append(String.format("%s = %s", Utils.getParamName(runtime, actionId, param.getParamId()),
                                                           Utils.byteArrayToStr(param.getValue().toByteArray())));
                });

                List<FieldMatch> fieldList = entry.getMatchList();
                fieldList.forEach(field->{
                    int fieldId = field.getFieldId();
                    switch (field.getFieldMatchTypeCase()) {
                        case EXACT: {
                            FieldMatch.Exact exact = field.getExact();
                            String tmp = String.format("%s = ",Utils.getMatchFieldName(runtime, tableId, fieldId));
                            tmp += Utils.byteArrayToStr(exact.getValue().toByteArray());
                            tmp += " : EXACT ";
                            buffer.append(tmp);
                            break;
                        }

                        case LPM: {
                            FieldMatch.LPM lpm = field.getLpm();
                            String tmp = String.format("%s = ",Utils.getMatchFieldName(runtime, tableId, fieldId));
                            tmp += Utils.byteArrayToStr(lpm.getValue().toByteArray());
                            tmp += " /";
                            tmp += String.valueOf(lpm.getPrefixLen());
                            tmp += " : LPM ";
                            buffer.append(tmp);
                            break;
                        }
                    }
                });

                result.add(new String(buffer));
            });
            if (isCompleted) break;
        }

        return true;
    }

    @Override
    public Future<RpcResult<PopulateTableEntryOutput>> populateTableEntry(PopulateTableEntryInput input) {
        PopulateTableEntryOutputBuilder builder = new PopulateTableEntryOutputBuilder();
        builder.setResult(doPopulateTableEntry(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<ReadTableEntryOutput>> readTableEntry(ReadTableEntryInput input) {
        ReadTableEntryOutputBuilder builder = new ReadTableEntryOutputBuilder();
        builder.setResult(true);
        List<String> list = new ArrayList<>();
        doReadTableEntry(input, list);
        builder.setEntry(list);
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
