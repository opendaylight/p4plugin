/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.appcommon;

import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeService;

public abstract class P4SwitchBuilder {
    protected String gRPCServerIp;
    protected Integer gRPCServerPort;
    protected Long deviceId;
    protected String nodeId;
    protected String configFile;
    protected String runtimeFile;
    protected P4pluginP4runtimeService runtimeService;

    public P4SwitchBuilder() {}
    public abstract P4Switch build();

    public P4SwitchBuilder setServerIp(String gRPCServerIp) {
        this.gRPCServerIp = gRPCServerIp;
        return this;
    }

    public P4SwitchBuilder setServerPort(Integer gRPCServerPort) {
        this.gRPCServerPort = gRPCServerPort;
        return this;
    }

    public P4SwitchBuilder setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public P4SwitchBuilder setNodeId(String nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    public P4SwitchBuilder setRuntimeFile(String runtimeFile) {
        this.runtimeFile = runtimeFile;
        return this;
    }

    public P4SwitchBuilder setConfigFile(String configFile) {
        this.configFile = configFile;
        return this;
    }

    public P4SwitchBuilder setRuntimeService(P4pluginP4runtimeService runtimeService) {
        this.runtimeService = runtimeService;
        return this;
    }
}
