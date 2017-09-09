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
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.P4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.rev170808.P4PacketReceivedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GrpcChannel {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcChannel.class);

    private final ManagedChannel channel;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private final P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private StreamObserver<StreamMessageRequest> requestStreamObserver;
    private String ip;
    private Integer port;
    private CountDownLatch countDownLatch;

    public GrpcChannel(String ip, Integer port) {
        this(ManagedChannelBuilder.forAddress(ip, port).usePlaintext(true));
        this.ip = ip;
        this.port = port;
        this.countDownLatch = new CountDownLatch(1);
    }

    private GrpcChannel(ManagedChannelBuilder<?> channelBuilder) {
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

    public boolean getChannelState() {
        boolean state = true;
        try {
            state = !countDownLatch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOG.info("Get channel state exception.");
            e.printStackTrace();
        }
        return state;
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public StreamObserver<StreamMessageRequest> initBidiStreamChannel() {
        StreamObserver<StreamMessageResponse> response = new StreamObserver<StreamMessageResponse>() {
            @Override
            public void onNext(StreamMessageResponse value) {
                P4PacketReceivedBuilder notification = new P4PacketReceivedBuilder();
                notification.setPayload(value.getPacket().getPayload().toByteArray());
                NotificationProvider.getInstance().notify(notification.build());
                LOG.info("Receive packet in.");
            }

            @Override
            public void onError(Throwable t) {
                ResourceManager.removeChannel(ip, port);
                ResourceManager.removeDevices(ip, port);
                countDownLatch.countDown();
                LOG.info("Stream channel on error, reason = {}.", t.getMessage());
                LOG.info("Stream channel on error, backtrace:", t);
            }

            @Override
            public void onCompleted() {
                ResourceManager.removeChannel(ip, port);
                ResourceManager.removeDevices(ip, port);
                countDownLatch.countDown();
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
