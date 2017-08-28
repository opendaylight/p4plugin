/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.p4plugin.p4config.proto.P4DeviceConfig;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import static org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigRequest.Action.VERIFY_AND_COMMIT;

public class Channel {
    private static final Logger LOG = LoggerFactory.getLogger(Channel.class);

    private final ManagedChannel channel;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private final P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private StreamObserver<StreamMessageRequest> requestStreamObserver;
    private P4Info p4Info;
    private ByteString deviceConfig;
    private boolean state = true;

    public Channel(String host, Integer port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    private Channel(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
        asyncStub = P4RuntimeGrpc.newStub(channel);
        requestStreamObserver = initBidiStreamChannel();
    }

    public void sendMasterArbitration(long deviceId) {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        MasterArbitrationUpdate.Builder masterArbitrationBuilder = MasterArbitrationUpdate.newBuilder();
        masterArbitrationBuilder.setDeviceId(deviceId);
        requestBuilder.setArbitration(masterArbitrationBuilder);
        requestStreamObserver.onNext(requestBuilder.build());
        //wait 2s, in order to get the correct result,
        try {
            Thread.currentThread().sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public StreamObserver<StreamMessageRequest> initBidiStreamChannel() {
        StreamObserver<StreamMessageRequest> observer = asyncStub.streamChannel(new StreamObserver<StreamMessageResponse>() {
            @Override
            public void onNext(StreamMessageResponse value) {
                LOG.info("receive packet in");
            }

            @Override
            public void onError(Throwable t) {
                state = false;
                LOG.info("Stream Channel on Error");
            }

            @Override
            public void onCompleted() {
                state = false;
                LOG.info("Stream Channel on Complete");
            }
        });

        return observer;
    }

    /**
     *
     * @param deviceConfig for bmv2 it is json file, for tofino it is a bin file;
     * @param p4Info runtime info file;
     * @param deviceId
     * @return
     * @throws IOException
     */
    public SetForwardingPipelineConfigResponse setPipelineConfig(@Nullable String deviceConfig,
                                                                 String p4Info,
                                                                 long deviceId) throws IOException {
        Preconditions.checkArgument(p4Info != null, "P4Info is null");

        this.p4Info = Utils.parseRuntimeInfo(p4Info);
        this.deviceConfig = Utils.parseDeviceConfigInfo(deviceConfig);

        ForwardingPipelineConfig.Builder configBuilder = ForwardingPipelineConfig.newBuilder();
        P4DeviceConfig.Builder p4DeviceConfigBuilder = P4DeviceConfig.newBuilder();
        configBuilder.setDeviceId(deviceId);

        if(this.p4Info != null) {
            configBuilder.setP4Info(this.p4Info);
        }

        if(this.deviceConfig != null) {
            p4DeviceConfigBuilder.setDeviceData(this.deviceConfig);
        }

        configBuilder.setP4DeviceConfig(p4DeviceConfigBuilder.build().toByteString());
        SetForwardingPipelineConfigRequest request = SetForwardingPipelineConfigRequest.newBuilder()
                                                     .setAction(VERIFY_AND_COMMIT)
                                                     .addConfigs(configBuilder.build())
                                                     .build();
        SetForwardingPipelineConfigResponse response;

        try {
            /* response is empty now */
            response = blockingStub.setForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Set pipeline config RPC failed: {}", e.getStatus());
        }

        return null;
    }

    /**
     *  write RPC, unary call;
     */
    public WriteResponse write(WriteRequest request) {
        @Nullable WriteResponse response;
        try {
            response = blockingStub.write(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Write RPC failed: {}", e.getStatus());
        }
        return null;
    }

    /**
     *  read RPC, server stream;
     */
    public Iterator<ReadResponse> read(ReadRequest request) {
        @Nullable Iterator<ReadResponse> responses;
        try {
            responses = blockingStub.read(request);
            return responses;
        } catch (StatusRuntimeException e) {
            LOG.info("Write RPC failed: {}", e.getStatus());
        }
        return null;
    }

    public boolean getState() {
        return state;
    }

    public P4Info getP4Info() {
        return this.p4Info;
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
