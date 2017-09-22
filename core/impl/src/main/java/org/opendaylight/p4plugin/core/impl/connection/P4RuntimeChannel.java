/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.connection;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * P4 runtime channel, including a gRPC channel and a list which records
 * the stubs. Multiple stubs can share the same channel.
 */
public class P4RuntimeChannel {
    private final ManagedChannel channel;
    private final List<P4RuntimeStub> stubs;

    public P4RuntimeChannel(String ip, Integer port) {
        this(ManagedChannelBuilder.forAddress(ip, port).usePlaintext(true));
    }

    private P4RuntimeChannel(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        stubs = new ArrayList<>();
    }

    public ManagedChannel getManagedChannel() {
        return channel;
    }

    public void addStub(P4RuntimeStub stub) {
        stubs.add(stub);
    }

    public void removeStub(P4RuntimeStub stub) {
        stubs.remove(stub);
        FlyweightFactory.getInstance().gc();
    }

    public Integer getStubsCount() {
        return stubs.size();
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
