/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.appcommon;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.p4plugin.appcommon.swtich.P4Switch;
import org.opendaylight.p4plugin.appcommon.swtich.P4SwitchBuilder;
import org.opendaylight.p4plugin.appcommon.topo.Topo;
import org.opendaylight.p4plugin.appcommon.topo.TopoParser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationRunner.class);
    protected Map<String, P4Switch> p4SwitchMap = new LinkedHashMap<>();
    private P4pluginDeviceService deviceService;
    private P4pluginP4runtimeService runtimeService;


    public ApplicationRunner(final P4pluginDeviceService deviceService,
                             final P4pluginP4runtimeService runtimeService,
                             final String topoFilePath) {
        this.deviceService = deviceService;
        this.runtimeService = runtimeService;
        createLocalTopo(topoFilePath);
    }

    private void createLocalTopo(String path) {
        Topo topo = getTopo(path);
        if (topo != null) {
            List<Topo.SwitchConfig> configs = topo.getSwitches();
            for (Topo.SwitchConfig c : configs) {
                P4Switch p4Switch = new P4SwitchBuilder()
                        .setNodeId(c.getNodeId())
                        .setServerIp(c.getgRPCServerIp())
                        .setServerPort(c.getgRPCServerPort())
                        .setDeviceId(c.getDeviceId())
                        .setConfigFile(c.getConfigFile())
                        .setRuntimeFile(c.getRuntimeFile())
                        .setRuntimeService(runtimeService).build();
                p4SwitchMap.put(c.getNodeId(), p4Switch);
            }
        }
    }

    private Topo getTopo(String path) {
        TopoParser topoParser = new TopoParser(path);
        topoParser.parse();
        return topoParser.getTopo();
    }

    public abstract void run();

    public boolean loadTopo() {
        boolean result = true;

        for(Map.Entry<String, P4Switch> entry : p4SwitchMap.entrySet()) {
            P4Switch p4Switch = entry.getValue();
            if (addNode(p4Switch)) {
                p4Switch.openStreamChannel();
                p4Switch.setPipelineConfig();
            } else {
                result = false;
                break;
            }
        }
        return result;
    }

    public void removeTopo() {
        for(Map.Entry<String, P4Switch> entry : p4SwitchMap.entrySet()) {
            removeNode(entry.getValue());
        }
    }

    private boolean addNode(P4Switch p4Switch) {
        AddDeviceInputBuilder inputBuilder = new AddDeviceInputBuilder();
        inputBuilder.setNid(p4Switch.getNodeId());
        inputBuilder.setDid(new BigInteger(p4Switch.getDeviceId().toString()));
        inputBuilder.setIp(new Ipv4Address(p4Switch.getgRPCServerIp()));
        inputBuilder.setPort(new PortNumber(p4Switch.getgRPCServerPort()));
        inputBuilder.setPipelineFile(p4Switch.getConfigFile());
        inputBuilder.setRuntimeFile(p4Switch.getRuntimeFile());
        String nodeId = p4Switch.getNodeId();
        boolean result;

        try {
            ListenableFuture<RpcResult<AddDeviceOutput>> output = deviceService.addDevice(inputBuilder.build());
            result = output.get().isSuccessful();
            LOG.info("Add switch {} {}.", nodeId, result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Add switch {} exception, message = {}.", nodeId, e.getMessage());
        }
        return result;
    }

    public void removeNode(P4Switch p4Switch) {
        RemoveDeviceInputBuilder inputBuilder = new RemoveDeviceInputBuilder();
        String nodeId = p4Switch.getNodeId();
        inputBuilder.setNid(nodeId);
        boolean result;

        try {
            ListenableFuture<RpcResult<RemoveDeviceOutput>> output = deviceService.removeDevice(inputBuilder.build());
            result = output.get().isSuccessful();
            LOG.info("Remove switch {} {}.", nodeId, result ? "success" : "failed");
        } catch (InterruptedException | ExecutionException e) {
            result = false;
            LOG.error("Remove switch {} exception, message = {}.", nodeId, e.getMessage());
        }
    }
}
