/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.p4plugin.channel;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4runtime.proto.ForwardingPipelineConfig;
import org.opendaylight.p4plugin.p4runtime.proto.P4RuntimeGrpc;
import org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest;
import org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigResponse;
import org.opendaylight.p4plugin.p4runtime.proto.Entity;
import org.opendaylight.p4plugin.p4runtime.proto.Update;
import org.opendaylight.p4plugin.p4runtime.proto.WriteRequest;
import java.io.FileInputStream;
import java.io.FileReader;
import org.opendaylight.p4plugin.p4runtime.proto.WriteResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChannelImpl {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelImpl.class.getName());

    private final ManagedChannel channel;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private P4Info info;
    public ChannelImpl(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    ChannelImpl(ManagedChannelBuilder<?> channelBuilder) {
          channel = channelBuilder.build();
          blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        //channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void setForwardingPipelineConfig() {
        P4Info.Builder p4InfoBuilder = P4Info.newBuilder();

        try {
            TextFormat.merge(new FileReader("simple_router.proto.txt"), p4InfoBuilder);
        } catch (IOException e) {
            LOG.info("simple_router.proto.txt file not found");
        }

        info = p4InfoBuilder.build();
        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        configBuilder.setDeviceId(0);
        configBuilder.setP4Info(p4InfoBuilder.build());

        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();

        try {
            p4DeviceConfigBuilder.setDeviceData(ByteString.readFrom(new FileInputStream("simple_router.json")));
        } catch (IOException e) {
            LOG.info("simple_router.json file not found");
        }

        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());

        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                                                     .setAction(org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest.Action
                                                                .VERIFY_AND_COMMIT)
                                                     .addConfigs(configBuilder.build())
                                                     .build();
        SetForwardingPipelineConfigResponse response;

        try {
            response = blockingStub.setForwardingPipelineConfig(request);
        } catch (StatusRuntimeException e) {
            LOG.info("RPC failed: {}", e.getStatus());
        }
    }

    public void writeTableEntry(WriteTableEntryData data) {
        WriteRequest.Builder request = WriteRequest.newBuilder();
        request.setDeviceId(data.getDeviceId());
        Update.Builder updateBuilder = Update.newBuilder();
        updateBuilder.setType(Update.Type.valueOf(data.getUpdateType().toString()));

        Entity.Builder entityBuilder = Entity.newBuilder();
        entityBuilder.setTableEntry(data.toMessage(info));
        updateBuilder.setEntity(entityBuilder.build());
        request.addUpdates(updateBuilder.build());

        WriteResponse response = null;
        try {
            response = blockingStub.write(request.build());
        } catch (StatusRuntimeException e) {
            LOG.info("RPC failed: {}", e.getStatus());
        }
    }
}
