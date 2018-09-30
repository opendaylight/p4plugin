/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.p4runtime;

import com.google.protobuf.ByteString;
import org.opendaylight.p4plugin.NotificationPublisher;
import org.opendaylight.p4plugin.p4info.proto.ControllerPacketMetadata;
import org.opendaylight.p4plugin.p4info.proto.P4Info;
import org.opendaylight.p4plugin.p4runtime.proto.PacketMetadata;
import org.opendaylight.p4plugin.p4runtime.proto.StreamMessageResponse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.packet.metadata.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.packet.metadata.MetadataBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PacketInHandler {
    private String nodeId;
    private P4Info p4Info;

    public PacketInHandler(String nodeId, P4Info p4Info) {
        this.nodeId = nodeId;
        this.p4Info = p4Info;
    }

    public void process(StreamMessageResponse streamMessageResponse) {
        PacketReceivedBuilder builder = new PacketReceivedBuilder();
        byte[] payload = streamMessageResponse.getPacket().getPayload().toByteArray();
        List<PacketMetadata> packetMetadataList = streamMessageResponse.getPacket().getMetadataList();
        List<ControllerPacketMetadata> cPacketMetadataList = p4Info.getControllerPacketMetadataList();
        List<Metadata> metadataList = new ArrayList<>();

        for(PacketMetadata metadata : packetMetadataList) {
            ByteString value = metadata.getValue();
            int id = metadata.getMetadataId();

            Optional<ControllerPacketMetadata> cPacketMetadataOptional = cPacketMetadataList
                    .stream()
                    .filter(cm -> cm.getPreamble().getName().equals("packet_in") || cm.getPreamble().getAlias().equals("packet_in"))
                    .findFirst();

            Optional<ControllerPacketMetadata.Metadata> cPacketMetadataMetadataOptional = cPacketMetadataOptional
                    .orElseThrow(() -> new RuntimeException("No controller metadata named \"packet_in\""))
                    .getMetadataList()
                    .stream()
                    .filter(m -> m.getId() == id)
                    .findFirst();

            MetadataBuilder metadataBuilder =  new MetadataBuilder();
            metadataBuilder.setMetadataName(cPacketMetadataMetadataOptional
                    .orElseThrow(() -> new RuntimeException("No controller metadata id = " + id)).getName());
            metadataBuilder.setMetadataValue(value.toByteArray());
            metadataList.add(metadataBuilder.build());
        }

        builder.setMetadata(metadataList);
        builder.setNid(nodeId);
        builder.setPayload(payload);
        NotificationPublisher.getInstance().notify(builder.build());
    }
}
