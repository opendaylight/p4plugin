/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.channel;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;

public class ChannelFactory {
    private HashMap<String, ManagedChannel> pool = new HashMap<>();
    private static ChannelFactory singleton = new ChannelFactory();
    private ChannelFactory() {}

    public static ChannelFactory getInstance() {
        return singleton;
    }

    public synchronized ManagedChannel getManagedChannel(String key) {
        if (key.matches("\\d+.\\d+.\\d+.\\d+:\\d+")) {
            ManagedChannel managedChannel = pool.get(key);
            if (managedChannel == null) {
                String[] arr = key.split(":");
                String ip = arr[0];
                Integer port = Integer.parseInt(arr[1]);
                managedChannel = ManagedChannelBuilder.forAddress(ip, port).usePlaintext(true).build();
                pool.put(key, managedChannel);
            }
            return managedChannel;
        } else {
            throw new RuntimeException("Invalid key.");
        }
    }
}
