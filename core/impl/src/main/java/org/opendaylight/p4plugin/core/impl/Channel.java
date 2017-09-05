/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

public class Channel {
    private static final Logger LOG = LoggerFactory.getLogger(Channel.class);

    private final ManagedChannel channel;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private final P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private StreamObserver<StreamMessageRequest> requestStreamObserver;

    public Channel(String host, Integer port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    private Channel(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
        asyncStub = P4RuntimeGrpc.newStub(channel);
        requestStreamObserver = initBidiStreamChannel();
    }

    public P4RuntimeGrpc.P4RuntimeBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public P4RuntimeGrpc.P4RuntimeStub getAsyncStub() {
        return asyncStub;
    }

    public StreamObserver<StreamMessageRequest> initBidiStreamChannel() {
        StreamObserver<StreamMessageResponse> response = new StreamObserver<StreamMessageResponse>() {
            @Override
            public void onNext(StreamMessageResponse value) {
                LOG.info("receive packet in");
            }

            @Override
            public void onError(Throwable t) {
                DeviceManager.getChannelMap().forEach((k,v) ->v.shutdown());
                DeviceManager.getTargetMap().forEach((k,v) ->v.setTargetState(Target.TargetState.Unknown));
                LOG.info("Stream channel on error.");
            }

            @Override
            public void onCompleted() {
                DeviceManager.getChannelMap().forEach((k,v) ->v.shutdown());
                DeviceManager.getTargetMap().forEach((k,v) ->v.setTargetState(Target.TargetState.Unknown));
                LOG.info("Stream channel on complete.");
            }
        };

        return asyncStub.streamChannel(response);
    }

    public StreamObserver<StreamMessageRequest> getRequestStreamObserver() {
        return requestStreamObserver;
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
