/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.appcommon.topo;

import java.util.List;

public class Topo {
    private List<SwitchConfig> switches;
    public List<SwitchConfig> getSwitches() {
        return switches;
    }

    @Override
    public String toString() {
        StringBuilder builder =  new StringBuilder();
        switches.forEach(s ->
            builder.append(s.toString()).append("\n")
        );
        return builder.toString();
    }

    public static class SwitchConfig {
        private String node_id;
        private Long device_id;
        private String grpc_server_ip;
        private Integer grpc_server_port;
        private String config_file;
        private String runtime_file;

        public String getConfigFile() {
            return config_file;
        }

        public String getRuntimeFile() {
            return runtime_file;
        }

        public String getgRPCServerIp() {
            return grpc_server_ip;
        }

        public Integer getgRPCServerPort() {
            return grpc_server_port;
        }

        public Long getDeviceId() {
            return device_id;
        }

        public String getNodeId() {
            return node_id;
        }

        public String toString() {
            return "[" +
                    node_id + " " +
                    grpc_server_ip + " " +
                    grpc_server_port + " " +
                    device_id + " " +
                    config_file + " " +
                    runtime_file + "]";
        }
    }
}
