/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin;

import org.opendaylight.p4plugin.appcommon.P4Switch;
import org.opendaylight.p4plugin.appcommon.P4SwitchRunner;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.device.rev170808.P4pluginDeviceService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.AddTableEntryInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.AddTableEntryInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.P4pluginP4runtimeService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.TypedValue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.action.ActionParam;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.action.ActionParamBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.Field;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.FieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.match.field.field.match.type.LpmBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.table.entry.action.type.DirectActionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SimpleRouterRunner extends P4SwitchRunner {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleRouterRunner.class);

    public SimpleRouterRunner(final P4pluginDeviceService deviceService,
                              final P4pluginP4runtimeService runtimeService,
                              final String gRPCServerIp,
                              final Integer gRPCServerPort,
                              final Long deviceId,
                              final String nodeId,
                              final String configFile,
                              final String runtimeFile) {
        super(deviceService, runtimeService, gRPCServerIp, gRPCServerPort, deviceId, nodeId, configFile, runtimeFile);
    }

    @Override
    public void run() {
        if (addDevice()) {
            p4Switch.openStreamChannel();
            p4Switch.setPipelineConfig();
            p4Switch.addTableEntry(buildH1H2Entry());
            p4Switch.addTableEntry(buildH2H1Entry());
        }
    }

    /**
    {
        "input": {
            "nid": "node0",
            "table-name": "ipv4_lpm",
            "action-name": "ipv4_forward",
            "action-param": [
                {
                    "param-name": "port",
                    "param-value": "2"
                },
                {
                    "param-name": "dstAddr",
                    "param-value": "00:04:00:00:00:01"
                }
            ],
            
            "field": [
                {
                    "field-name": "hdr.ipv4.dstAddr",
                    "lpm-value": "10.0.1.0",
                    "prefix-len": "24"
                }
            ]
        }
    }
    
    {
        "input": {
            "nid": "node0",
            "table-name": "ipv4_lpm",
            "action-name": "ipv4_forward",
            "action-param": [
                {
                    "param-name": "port",
                    "param-value": "1"
                },
                {
                    "param-name": "dstAddr",
                    "param-value": "00:04:00:00:00:00"
                }
            ],
            
            "field": [
                {
                    "field-name": "hdr.ipv4.dstAddr",
                    "lpm-value": "10.0.0.0",
                    "prefix-len": "24"
                }
            ]
        }
    }
    */
    /* h1 -> h2 entry */
    private AddTableEntryInput buildH1H2Entry() {
        AddTableEntryInputBuilder inputBuilder = new AddTableEntryInputBuilder();
        inputBuilder.setNid(p4Switch.getNodeId());
        inputBuilder.setTableName("ipv4_lpm");

        /* build match field */
        FieldBuilder fieldBuilder = new FieldBuilder();
        fieldBuilder.setFieldName("hdr.ipv4.dstAddr");

        LpmBuilder lpmBuilder = new LpmBuilder();
        lpmBuilder.setLpmValue(new TypedValue("10.0.1.0"));
        lpmBuilder.setPrefixLen((long)24);
        fieldBuilder.setMatchType(lpmBuilder.build());

        List<Field> fieldList = new ArrayList<>();
        fieldList.add(fieldBuilder.build());

        /* build action */
        ActionParamBuilder actionParamBuilder1 = new ActionParamBuilder();
        actionParamBuilder1.setParamName("port");
        actionParamBuilder1.setParamValue(new TypedValue("2"));

        ActionParamBuilder actionParamBuilder2 = new ActionParamBuilder();
        actionParamBuilder2.setParamName("dstAddr");
        actionParamBuilder2.setParamValue(new TypedValue("00:04:00:00:00:01"));

        List<ActionParam> actionParamList = new ArrayList<>();
        actionParamList.add(actionParamBuilder1.build());
        actionParamList.add(actionParamBuilder2.build());

        DirectActionBuilder directActionBuilder = new DirectActionBuilder();
        directActionBuilder.setActionName("ipv4_forward");
        directActionBuilder.setActionParam(actionParamList);

        inputBuilder.setField(fieldList);
        inputBuilder.setActionType(directActionBuilder.build());
        return inputBuilder.build();
    }

    /* h2 -> h1 entry */
    private AddTableEntryInput buildH2H1Entry() {
        AddTableEntryInputBuilder inputBuilder = new AddTableEntryInputBuilder();
        inputBuilder.setNid(p4Switch.getNodeId());
        inputBuilder.setTableName("ipv4_lpm");

        /* build match field */
        FieldBuilder fieldBuilder = new FieldBuilder();
        fieldBuilder.setFieldName("hdr.ipv4.dstAddr");

        LpmBuilder lpmBuilder = new LpmBuilder();
        lpmBuilder.setLpmValue(new TypedValue("10.0.0.0"));
        lpmBuilder.setPrefixLen((long)24);
        fieldBuilder.setMatchType(lpmBuilder.build());

        List<Field> fieldList = new ArrayList<>();
        fieldList.add(fieldBuilder.build());

        /* build action */
        ActionParamBuilder actionParamBuilder1 = new ActionParamBuilder();
        actionParamBuilder1.setParamName("port");
        actionParamBuilder1.setParamValue(new TypedValue("1"));

        ActionParamBuilder actionParamBuilder2 = new ActionParamBuilder();
        actionParamBuilder2.setParamName("dstAddr");
        actionParamBuilder2.setParamValue(new TypedValue("00:04:00:00:00:00"));

        List<ActionParam> actionParamList = new ArrayList<>();
        actionParamList.add(actionParamBuilder1.build());
        actionParamList.add(actionParamBuilder2.build());

        DirectActionBuilder directActionBuilder = new DirectActionBuilder();
        directActionBuilder.setActionName("ipv4_forward");
        directActionBuilder.setActionParam(actionParamList);

        inputBuilder.setField(fieldList);
        inputBuilder.setActionType(directActionBuilder.build());
        return inputBuilder.build();
    }
}
