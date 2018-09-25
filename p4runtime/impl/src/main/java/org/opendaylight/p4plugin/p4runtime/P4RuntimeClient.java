/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.p4runtime;

import com.google.protobuf.ByteString;
import com.google.rpc.Code;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.opendaylight.p4plugin.NotificationPublisher;
import org.opendaylight.p4plugin.channel.ChannelFactory;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.p4runtime.rev170808.PacketReceivedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Iterator;

public class P4RuntimeClient {
    private static final Logger LOG = LoggerFactory.getLogger(P4RuntimeClient.class);
    private Long deviceId;
    private String nodeId;
    private String ip;
    private Integer port;
    private P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private StreamObserver<StreamMessageRequest> requestStreamObserver;

    public P4RuntimeClient(String ip, Integer port, Long deviceId, String nodeId) {
        ManagedChannel managedChannel = getManagedChannel(ip, port);
        this.deviceId = deviceId;
        this.nodeId = nodeId;
        this.ip = ip;
        this.port = port;
        this.blockingStub = P4RuntimeGrpc.newBlockingStub(managedChannel);
        this.asyncStub = P4RuntimeGrpc.newStub(managedChannel);
    }

    private ManagedChannel getManagedChannel(String ip , Integer port) {
        String key = String.format("%s:%d", ip, port);
        return ChannelFactory.getInstance().getManagedChannel(key);
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig(SetForwardingPipelineConfigRequest request) {
        SetForwardingPipelineConfigResponse response;
        try {
            response = blockingStub.setForwardingPipelineConfig(request);
        } catch (StatusRuntimeException e) {
            LOG.info("Set pipeline config exception, Status = {}, Reason = {}", e.getStatus(), e.getMessage());
            throw e;
        }
        return response;
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig(GetForwardingPipelineConfigRequest request) {
        GetForwardingPipelineConfigResponse response;
        try {
            response = blockingStub.getForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Get pipeline config exception, Status = {}, Reason = {}", e.getStatus(), e.getMessage());
            throw e;
        }
    }

    public WriteResponse write(WriteRequest request) {
        WriteResponse response;
        try {
            response = blockingStub.write(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info("Write RPC exception, Status = %s, Reason = %s", e.getStatus(), e.getMessage());
            throw e;
        }
    }

    public Iterator<ReadResponse> read(ReadRequest request) {
        Iterator<ReadResponse> responses;
        try {
            responses = blockingStub.read(request);
            return responses;
        } catch (StatusRuntimeException e) {
            LOG.info("Write RPC exception, Status = %s, Reason = %s", e.getStatus(), e.getMessage());
            throw e;
        }
    }

    public void openStreamChannel() {
        StreamObserver<StreamMessageResponse> responseStreamObserver = new StreamObserver<StreamMessageResponse>() {
            @Override
            public void onNext(StreamMessageResponse streamMessageResponse) {
                switch (streamMessageResponse.getUpdateCase()) {
                    case PACKET:
                        PacketReceivedBuilder builder = new PacketReceivedBuilder();
                        byte[] payload = streamMessageResponse.getPacket().getPayload().toByteArray();
                        builder.setNid(nodeId);
                        builder.setPayload(payload);
                        NotificationPublisher.getInstance().notify(builder.build());
                        break;

                    case ARBITRATION:
                        MasterArbitrationUpdate update = streamMessageResponse.getArbitration();
                        if (update.getStatus().getCode() == Code.OK_VALUE) {
                            LOG.info("I am the master controller to device = {}.", update.getDeviceId());
                        } else {
                            LOG.info("I am not the master to device = {}, election id = {}/{}.",
                                    update.getDeviceId(),
                                    update.getElectionId().getHigh(),
                                    update.getElectionId().getLow());
                        }
                        break;
                    case UPDATE_NOT_SET:break;
                    default:break;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                requestStreamObserver = null;
                LOG.info("Stream channel on error, reason = {}, node = {}", throwable.getMessage(), nodeId);
            }

            @Override
            public void onCompleted() {
                requestStreamObserver = null;
                LOG.info("Stream channel on error, node = {}", nodeId);
            }
        };

        requestStreamObserver =  asyncStub.streamChannel(responseStreamObserver);
        /* send master arbitration update packet immediately, right now we only support election id = 0 */
        Uint128.Builder electionIdBuilder = Uint128.newBuilder();
        electionIdBuilder.setHigh(0);
        electionIdBuilder.setLow(0);

        Role.Builder roleBuilder = Role.newBuilder();
        roleBuilder.setId(0);
        sendMasterArbitrationUpdate(electionIdBuilder.build(), roleBuilder.build());
    }

    public void closeStreamChannel() {
        if (requestStreamObserver != null) {
            requestStreamObserver.onCompleted();
        }
    }

    public void transmitPacket(StreamMessageRequest request) {
        if (requestStreamObserver != null) {
            requestStreamObserver.onNext(request);
        } else {
            LOG.info("Stream channel is null, node = {}.", nodeId);
        }
    }

    public void sendPacket(byte[] payload) {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        PacketOut.Builder packetOutBuilder = PacketOut.newBuilder();
        packetOutBuilder.setPayload(ByteString.copyFrom(payload));
        requestBuilder.setPacket(packetOutBuilder);

        LOG.info("Sending packet = {} to node = {}.", bytes2HexStr(payload), nodeId);
        transmitPacket(requestBuilder.build());
    }

    public void sendMasterArbitrationUpdate(Uint128 electionId, Role role) {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        MasterArbitrationUpdate.Builder masterArbitrationBuilder = MasterArbitrationUpdate.newBuilder();
        masterArbitrationBuilder.setDeviceId(deviceId);
        masterArbitrationBuilder.setElectionId(electionId);
        masterArbitrationBuilder.setRole(role);
        requestBuilder.setArbitration(masterArbitrationBuilder);

        LOG.info("Sending master arbitration update high = {} low = {} to node = {}.",
                electionId.getHigh(), electionId.getLow(),nodeId);
        transmitPacket(requestBuilder.build());
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    private String bytes2HexStr(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }

        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }

        return stringBuilder.toString();
    }
}
