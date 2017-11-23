/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.channel;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Flyweight factory is a P4 runtime channel factory, multiple devices share
 * the same gRPC channel, only one factory instance. P4 runtime stub is over
 * the runtime channel.
 */
public class FlyweightFactory {
    private ConcurrentHashMap<String, P4RuntimeChannel> pool = new ConcurrentHashMap<>();
    private static FlyweightFactory singleton = new FlyweightFactory();
    private FlyweightFactory() {}
    public static FlyweightFactory getInstance() {
        return singleton;
    }

    /**
     * The ip address and port number determines a gRPC channel, in other words,
     * determines a tcp connection.
     */
    public synchronized P4RuntimeChannel getChannel(String ip, Integer port) {
        String key = String.format("%s:%d", ip, port);
        P4RuntimeChannel channel = pool.get(key);
        if (channel == null) {
            channel = makeP4RuntimeChannel(ip, port);
            pool.put(key, channel);
        }
        return channel;
    }

    /**
     * When there is no stream channel using a gRPC channel, then free
     * the gRPC channel.
     */
    public synchronized void gc() {
        List<String> keys = pool.keySet().stream()
                .filter(key->pool.get(key).getStubsCount() == 0)
                .collect(Collectors.toList());
        keys.forEach(key->{
            pool.get(key).shutdown();
            pool.remove(key);
        });
    }

    public P4RuntimeChannel makeP4RuntimeChannel(String ip, Integer port) {
        return new P4RuntimeChannel(ip, port);
    }

    private void setPool(ConcurrentHashMap<String, P4RuntimeChannel> pool) {
        this.pool = pool;
    }
}
