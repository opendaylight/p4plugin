/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.channelimpl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ChannelProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ChannelProvider.class);
    private final DataBroker dataBroker;

    ChannelImpl client = null;

    public ChannelProvider(final DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    /**
     * Method called when the blueprint container is created.
     */

    public void init() {
        LOG.info("ChannelProvider Session Initiated");
        client = new ChannelImpl("localhost",50051);

        client.setForwardingPipelineConfig("simple_router.json", "simple_router.proto.txt", 0);
        client.addTableEntry(new TableEntryMetaData.Builder()
                                .setDeviceId(0)
                                .setUpdateType(TableEntryMetaData.UpdateType.INSERT)
                                .setTableName("ipv4_lpm")
                                .setActionName("set_nhop")
                                .setFieldName("ipv4.dstAddr")
                                .setMatchType(TableEntryMetaData.MatchType.LPM)
                                .setMatchValue("10.0.0.0")
                                .setPrefixLen(24)
                                .addParams("nhop_ipv4","10.0.0.10")
                                .addParams("port", "1")
                                .build());

        client.addTableEntry(new TableEntryMetaData.Builder()
                                .setDeviceId(0)
                                .setUpdateType(TableEntryMetaData.UpdateType.INSERT)
                                .setTableName("ipv4_lpm")
                                .setActionName("set_nhop")
                                .setFieldName("ipv4.dstAddr")
                                .setMatchType(TableEntryMetaData.MatchType.LPM)
                                .setMatchValue("10.0.1.0")
                                .setPrefixLen(24)
                                .addParams("nhop_ipv4","10.0.1.10")
                                .addParams("port", "2")
                                .build());

        client.addTableEntry(new TableEntryMetaData.Builder()
                                .setDeviceId(0)
                                .setUpdateType(TableEntryMetaData.UpdateType.INSERT)
                                .setTableName("forward")
                                .setActionName("set_dmac")
                                .setFieldName("routing_metadata.nhop_ipv4")
                                .setMatchType(TableEntryMetaData.MatchType.EXACT)
                                .setMatchValue("10.0.0.10")
                                .addParams("dmac","00:04:00:00:00:00")
                                .build());

        client.addTableEntry(new TableEntryMetaData.Builder()
                                .setDeviceId(0)
                                .setUpdateType(TableEntryMetaData.UpdateType.INSERT)
                                .setTableName("forward")
                                .setActionName("set_dmac")
                                .setFieldName("routing_metadata.nhop_ipv4")
                                .setMatchType(TableEntryMetaData.MatchType.EXACT)
                                .setMatchValue("10.0.1.10")
                                .addParams("dmac","00:04:00:00:00:01")
                                .build());

        client.addTableEntry(new TableEntryMetaData.Builder()
                                .setDeviceId(0)
                                .setUpdateType(TableEntryMetaData.UpdateType.INSERT)
                                .setTableName("send_frame")
                                .setActionName("rewrite_mac")
                                .setFieldName("standard_metadata.egress_port")
                                .setMatchType(TableEntryMetaData.MatchType.EXACT)
                                .setMatchValue("1")
                                .addParams("smac","0a:0b:0c:0d:0e:0f")
                                .build());

        client.addTableEntry(new TableEntryMetaData.Builder()
                                .setDeviceId(0)
                                .setUpdateType(TableEntryMetaData.UpdateType.INSERT)
                                .setTableName("send_frame")
                                .setActionName("rewrite_mac")
                                .setFieldName("standard_metadata.egress_port")
                                .setMatchType(TableEntryMetaData.MatchType.EXACT)
                                .setMatchValue("2")
                                .addParams("smac","01:02:03:04:05:06")
                                .build());
        //client.setForwardingPipelineConfig("packet_io.json", "packet_io.proto.txt", 0);
        client.initBidirectionalStreamChannel();
        for(int i = 0; i < 1000; i++) {
            LOG.info("packet out cnt = {}", i++);
            client.packetOut();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.info("sleep exception");
            }
        }

        LOG.info("Write OK");
    }


    /**
     * Method called when the blueprint container is destroyed.
     */

    public void close() {
        LOG.info("ChannelProvider Closed");
        try {
            client.shutdown();
        } catch (InterruptedException e) {
            LOG.error("Close is Interrupted", e);
        }
    }
}
