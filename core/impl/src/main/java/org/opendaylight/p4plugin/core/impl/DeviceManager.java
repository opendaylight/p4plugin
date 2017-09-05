/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.protobuf.TextFormat;
import org.opendaylight.p4plugin.p4runtime.proto.ForwardingPipelineConfig;
import org.opendaylight.p4plugin.p4runtime.proto.GetForwardingPipelineConfigResponse;
import org.opendaylight.p4plugin.p4runtime.proto.SetForwardingPipelineConfigResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class DeviceManager implements DeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceManager.class);
    static ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, Target> targets = new ConcurrentHashMap<>();
    public DeviceManager() {}

    public static Channel findChannel(String ip, Integer port) {
        String key = String.format("%s:%d", ip, port);
        Channel channel = null;
        Optional<String> keyContainer = channels.keySet().stream().filter(k->k.equals(key)).findFirst();

        if (keyContainer.isPresent()) {
            channel = channels.get(key);
        }

        return channel;
    }

    public static Target findTarget(String ip, Integer port, Long deviceId) {
        String key = String.format("%s:%d:%d", ip, port, deviceId);
        Target target = null;
        Optional<String> keyContainer = targets.keySet().stream().filter(k->k.equals(key)).findFirst();

        if (keyContainer.isPresent()) {
            target = targets.get(key);
        }

        return target;
    }

    public static Channel getChannel(String ip, Integer port) {
        Channel channel = findChannel(ip, port);

        if (channel == null) {
            channel = new Channel(ip, port);
            channels.put(String.format("%s:%d", ip, port), channel);
        }

        return channel;
    }

    public static Target getTarget(String ip, Integer port, Long deviceId,
                            String runtimeInfo, String deviceConfig) throws IOException {
        Target target = findTarget(ip, port, deviceId);

        if (target == null) {
            target = newTarget(ip, port, deviceId, runtimeInfo, deviceConfig);
            target.setTargetState(Target.TargetState.Connected);
            targets.put(String.format("%s:%d", ip, port), target);
            target.sendMasterArbitration();
        } else {
            /**Cannot find the difference between the old and the new resource files
             * So update every time.
             */
            if (runtimeInfo != null) {
                target.setRuntimeInfo(runtimeInfo);
                target.setTargetState(Target.TargetState.Connected);
            }

            if (deviceConfig != null) {
                target.setDeviceConfig(deviceConfig);
                target.setTargetState(Target.TargetState.Connected);
            }
        }

        return target;
    }

    public static Target newTarget(String ip, Integer port, Long deviceId,
                            String runtimeInfo, String deviceConfig) throws IOException {
        Channel channel = getChannel(ip, port);
        Target target;
        Target.Builder builder = Target.newBuilder()
                                       .setChannel(channel)
                                       .setDeviceId(deviceId);
        if (runtimeInfo != null) {
            builder.setRuntimeInfo(runtimeInfo);
        }

        if(deviceConfig != null) {
            builder.setDeviceConfig(deviceConfig);
        }

        target = builder.build();
        return target;
    }

    public static boolean doSetPipelineConfig(SetPipelineConfigInput input) {
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        String deviceConfig = input.getDeviceConfig();
        String runtimeInfo = input.getRuntimeInfo();
        Target target = null;
        SetForwardingPipelineConfigResponse response = null;

        try {
            target = getTarget(ip, port, deviceId, runtimeInfo, deviceConfig);
            if (target.getTargetState() != Target.TargetState.Unknown) {
                response = target.setPipelineConfig();
            }
            if (response != null) {
                target.setTargetState(Target.TargetState.Configured);
            }
            return response != null;
        } catch (IOException e) {
            LOG.info("IO Exception, reason = {}", e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public static boolean doGetPipelineConfig(GetPipelineConfigInput input) {
        String ip = input.getIp().getValue();
        Integer port = input.getPort().getValue();
        Long deviceId = input.getDeviceId().longValue();
        String outputFile = input.getOutputFile();
        Target target = null;

        try {
            target = getTarget(ip, port, deviceId, null, null);
        } catch (IOException e) {
            LOG.info("Get pipeline config IOException, reason = {}.", e.getMessage());
        }

        if ((target != null)  && (target.getTargetState() != Target.TargetState.Unknown)) {
            GetForwardingPipelineConfigResponse response = target.getPipelineConfig();
            if (response != null) {
                ForwardingPipelineConfig config = response.getConfigs(0);
                File file = new File(outputFile);
                FileOutputStream outputStream;
                PrintStream printStream;
                try {
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    outputStream = new FileOutputStream(outputFile);
                    printStream = new PrintStream(outputStream);
                    TextFormat.printToString(config.getP4Info());
                    printStream.println(TextFormat.printToString(config.getP4Info()));
                    outputStream.close();
                    printStream.close();
                    return true;
                } catch (Exception e) {
                    LOG.info("Write output file exception, file = {}, reason = {}", outputFile, e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    public static ConcurrentHashMap<String, Target> getTargetMap() {
        return targets;
    }

    public static ConcurrentHashMap<String, Channel> getChannelMap() {
        return channels;
    }

    public static void clear() {
        getChannelMap().forEach((k, v) -> v.shutdown());
        getChannelMap().clear();
        getTargetMap().clear();
    }

    @Override
    public Future<RpcResult<SetPipelineConfigOutput>> setPipelineConfig(SetPipelineConfigInput input) {
        SetPipelineConfigOutputBuilder builder = new SetPipelineConfigOutputBuilder();
        builder.setResult(doSetPipelineConfig(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<GetPipelineConfigOutput>> getPipelineConfig(GetPipelineConfigInput input) {
        GetPipelineConfigOutputBuilder builder = new GetPipelineConfigOutputBuilder();
        builder.setResult(doGetPipelineConfig(input));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }
}
