/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.p4plugin.device.DeviceManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.gnmi.rev170808.P4pluginGnmiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.gnmi.rev170808.SubscribeInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.gnmi.rev170808.SubscribeOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.gnmi.rev170808.SubscribeOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class gNMIServiceProvider implements P4pluginGnmiService {
    private static final Logger LOG = LoggerFactory.getLogger(gNMIServiceProvider.class);
    private final DataBroker dataBroker;
    private DeviceManager deviceManager;
    private ListeningExecutorService executorService;

    public gNMIServiceProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void init() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
        deviceManager = DeviceManager.getInstance();
        LOG.info("P4 plugin gNMI service provider initiated.");
    }

    public void close() {
        executorService.shutdown();
        LOG.info("P4 plugin gNMI service provider closed.");
    }

    private Callable<RpcResult<SubscribeOutput>> sub(SubscribeInput input) {
        return () -> {
            SubscribeOutputBuilder outputBuilder = new SubscribeOutputBuilder();
            deviceManager.findDevice(input.getNid())
                    .orElseThrow(IllegalArgumentException::new)
                    .subscribe(input);
            return RpcResultBuilder.success(outputBuilder.build()).build();
        };
    }

    @Override
    public ListenableFuture<RpcResult<SubscribeOutput>> subscribe(SubscribeInput input) {
        return executorService.submit(sub(input));
    }
}
