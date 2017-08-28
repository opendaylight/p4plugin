/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.util.concurrent.Futures;
import io.grpc.StatusRuntimeException;

import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4runtime.proto.Entity;
import org.opendaylight.p4plugin.p4runtime.proto.Update;
import org.opendaylight.p4plugin.p4runtime.proto.WriteRequest;
import org.opendaylight.p4plugin.p4runtime.proto.WriteResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.PopulateTableEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.PopulateTableEntryOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.PopulateTableEntryOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.table.rev170808.TableService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.Future;

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

    @Override
    public Future<RpcResult<PopulateTableEntryOutput>> populateTableEntry(PopulateTableEntryInput input) {
        PopulateTableEntryOutputBuilder builder = new PopulateTableEntryOutputBuilder();
        builder.setResult(doPopulateTableEntry(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
