/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl.stub;

import com.google.protobuf.ByteString;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionId;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionIdGenerator;
import org.opendaylight.p4plugin.runtime.impl.utils.NotificationPublisher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.packet.rev170808.P4PacketReceivedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Iterator;

public class RuntimeStub {
    private static final Logger LOG = LoggerFactory.getLogger(RuntimeStub.class);
    private ManagedChannel channel;
    private P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private StreamObserver<StreamMessageRequest> requestStreamObserver;
    private ElectionId electionId;
    private Long deviceId;
    private String nodeId;

    public RuntimeStub(String ip, Integer port, Long deviceId, String nodeId) {
        this(ManagedChannelBuilder.forAddress(ip, port).usePlaintext(true), deviceId, nodeId);
    }

    private RuntimeStub(ManagedChannelBuilder<?> channelBuilder, Long deviceId, String nodeId) {
        this.channel = channelBuilder.build();
        this.blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
        this.asyncStub = P4RuntimeGrpc.newStub(channel);
        this.electionId = ElectionIdGenerator.getInstance().getElectionId();
        this.deviceId = deviceId;
        this.nodeId = nodeId;
    }

    public void notifyWhenStateChanged(ConnectivityState state, Runnable callback) {
        channel.notifyWhenStateChanged(state, callback);
    }

    public ConnectivityState getConnectState() {
        return channel.getState(false);
    }

    public void shutdown() {
        channel.shutdown();
    }

    public WriteResponse write(WriteRequest request) {
        WriteResponse response;
        try {
            response = blockingStub.write(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.error("Write RPC exception, Status = {}, Reason = {}.", e.getStatus(), e.getMessage());
            throw e;
        }
    }

    public Iterator<ReadResponse> read(ReadRequest request) {
        Iterator<ReadResponse> responses;
        try {
            responses = blockingStub.read(request);
            return responses;
        } catch (StatusRuntimeException e) {
            LOG.error("Read RPC exception, Status = {}, Reason = {}.", e.getStatus(), e.getMessage());
            throw e;
        }
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig(SetForwardingPipelineConfigRequest request) {
        SetForwardingPipelineConfigResponse response;
        try {
            response = blockingStub.setForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.error("Set pipeline exception, Status = {}, Reason = {}.", e.getStatus(), e.getMessage());
            throw e;
        }
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig(GetForwardingPipelineConfigRequest request) {
        GetForwardingPipelineConfigResponse response;
        try {
            response = blockingStub.getForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.error("Get pipeline config exception, Status = {}, Reason = {}.", e.getStatus(), e.getMessage());
            throw e;
        }
    }

    public void streamChannel() {
        StreamObserver<StreamMessageResponse> responseStreamObserver = new StreamObserver<StreamMessageResponse>() {
            @Override
            public void onNext(StreamMessageResponse response) {
                onPacketReceived(response);
            }

            @Override
            public void onError(Throwable t) {
                requestStreamObserver = null;
                LOG.error("Stream channel on error, reason = {}.", t.getMessage());
            }

            @Override
            public void onCompleted() {
                requestStreamObserver = null;
                LOG.error("Stream channel on complete.");
            }
        };

        requestStreamObserver = asyncStub.streamChannel(responseStreamObserver);
        /* Send master arbitration message  immediately */
        sendMasterArbitration();
    }

    public void transmitPacket(byte[] payload) {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        PacketOut.Builder packetOutBuilder = PacketOut.newBuilder();
        packetOutBuilder.setPayload(ByteString.copyFrom(payload));
        requestBuilder.setPacket(packetOutBuilder);

        try {
            requestStreamObserver.onNext(requestBuilder.build());
        } catch (RuntimeException e) {
            requestStreamObserver.onError(e);
            throw e;
        }
    }

    public void updateElectionId(ElectionId electionId) {
        this.electionId = electionId;
        sendMasterArbitration();
    }

    private void sendMasterArbitration() {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        MasterArbitrationUpdate.Builder masterArbitrationBuilder = MasterArbitrationUpdate.newBuilder();
        Uint128.Builder electionIdBuilder = Uint128.newBuilder();
        electionIdBuilder.setHigh(electionId.getHigh());
        electionIdBuilder.setLow(electionId.getLow());
        masterArbitrationBuilder.setDeviceId(deviceId);
        masterArbitrationBuilder.setElectionId(electionIdBuilder);
        requestBuilder.setArbitration(masterArbitrationBuilder);

        try {
            requestStreamObserver.onNext(requestBuilder.build());
        } catch (RuntimeException e) {
            requestStreamObserver.onError(e);
            throw e;
        }
    }

    private void onPacketReceived(StreamMessageResponse response) {
        switch(response.getUpdateCase()) {
            case PACKET: {
                P4PacketReceivedBuilder builder = new P4PacketReceivedBuilder();
                byte[] payload = response.getPacket().getPayload().toByteArray();
                builder.setNid(nodeId);
                builder.setPayload(payload);
                NotificationPublisher.getInstance().notify(builder.build());
                break;
            }

            case ARBITRATION:
                //TODO
                break;
            case UPDATE_NOT_SET:
                break;
            default:
                break;
        }
    }
}
