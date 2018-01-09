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
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionIdObserver;
import org.opendaylight.p4plugin.runtime.impl.utils.NotificationPublisher;
import org.opendaylight.p4plugin.runtime.impl.utils.Utils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.packet.rev170808.P4PacketReceivedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RuntimeStub implements ElectionIdObserver {
    private static final Logger LOG = LoggerFactory.getLogger(RuntimeStub.class);
    private ManagedChannel channel;
    private P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private String nodeId;
    private Long deviceId;
    private StreamObserver<StreamMessageRequest> requestStreamObserver;
    private ElectionId electionId;

    public RuntimeStub(String ip, Integer port, Long deviceId, String nodeId) {
        this(ManagedChannelBuilder.forAddress(ip, port).usePlaintext(true), deviceId, nodeId);
    }

    private RuntimeStub(ManagedChannelBuilder<?> channelBuilder, Long deviceId, String nodeId) {
        this.channel = channelBuilder.build();
        this.nodeId = nodeId;
        this.deviceId = deviceId;
        initStub();
        initElectionId();
    }

    private void initStub() {
        this.blockingStub = P4RuntimeGrpc.newBlockingStub(channel);
        this.asyncStub = P4RuntimeGrpc.newStub(channel);
    }

    private void initElectionId() {
        ElectionIdGenerator generator = ElectionIdGenerator.getInstance();
        generator.addObserver(this);
        electionId = generator.getElectionId();
    }

    public void notifyWhenStateChanged(ConnectivityState source, Runnable callback) {
        channel.notifyWhenStateChanged(source, callback);
    }

    public boolean getConnectState() {
        return channel.getState(true) == ConnectivityState.READY
                && requestStreamObserver != null;
    }

    public void shutdown() {
        ElectionIdGenerator.getInstance().deleteObserver(this);
        channel.shutdown();
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig(SetForwardingPipelineConfigRequest request) {
        SetForwardingPipelineConfigResponse response;
        try {
            response = blockingStub.setForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info(String.format("Set pipeline config exception, Status = %s, Reason = %s",
                    e.getStatus(), e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig(GetForwardingPipelineConfigRequest request) {
        GetForwardingPipelineConfigResponse response;
        try {
            response = blockingStub.getForwardingPipelineConfig(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info(String.format("Get pipeline config exception, Status = %s, Reason = %s",
                    e.getStatus(), e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public void StreamChannel() {
        StreamObserver<StreamMessageResponse> responseStreamObserver = new StreamObserver<StreamMessageResponse>() {
            @Override
            public void onNext(StreamMessageResponse value) {
                onPacketReceived(value);
            }

            @Override
            public void onError(Throwable t) {
                onStreamChannelError(t);
            }

            @Override
            public void onCompleted() {
                onStreamChannelComplete();
            }
        };

        requestStreamObserver = asyncStub.streamChannel(responseStreamObserver);
        sendMasterArbitration(electionId);
        awaitConnection(5000);
    }

    public void transmitPacket(byte[] payload) {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        PacketOut.Builder packetOutBuilder = PacketOut.newBuilder();
        packetOutBuilder.setPayload(ByteString.copyFrom(payload));
        requestBuilder.setPacket(packetOutBuilder);
        if (requestStreamObserver != null) {
            requestStreamObserver.onNext(requestBuilder.build());
            //For debug
            LOG.info("Transmit packet = {} to device = {}.", Utils.bytesToHexString(payload), nodeId);
        } else {
            LOG.info("Stream channel haven't been initialized, device = [{}].", nodeId);
        }
    }

    public WriteResponse write(WriteRequest request) {
        WriteResponse response;
        try {
            response = blockingStub.write(request);
            return response;
        } catch (StatusRuntimeException e) {
            LOG.info(String.format("Write RPC exception, Status = %s, Reason = %s",
                    e.getStatus(), e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public Iterator<ReadResponse> read(ReadRequest request) {
        Iterator<ReadResponse> responses;
        try {
            responses = blockingStub.read(request);
            return responses;
        } catch (StatusRuntimeException e) {
            LOG.info(String.format("Read RPC exception, Status = %s, Reason = %s",
                    e.getStatus(), e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    public void sendMasterArbitration(ElectionId electionId) {
        StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
        MasterArbitrationUpdate.Builder masterArbitrationBuilder = MasterArbitrationUpdate.newBuilder();
        Uint128.Builder electionIdBuilder = Uint128.newBuilder();
        electionIdBuilder.setHigh(electionId.getHigh());
        electionIdBuilder.setLow(electionId.getLow());
        masterArbitrationBuilder.setDeviceId(deviceId);
        masterArbitrationBuilder.setElectionId(electionIdBuilder);
        requestBuilder.setArbitration(masterArbitrationBuilder);
        if (requestStreamObserver != null) {
            requestStreamObserver.onNext(requestBuilder.build());
            LOG.info("Send MasterArbitrationUpdate to device = {}.", nodeId);
        } else {
            LOG.info("Stream channel haven't been initialized, device = [{}].", nodeId);
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
                //For debug
                LOG.info("Receive packet from node = {}, body = {}.", nodeId, Utils.bytesToHexString(payload));
                break;
            }
            case ARBITRATION:break;
            case UPDATE_NOT_SET:break;
            default:break;
        }
    }

    private void onStreamChannelError(Throwable t) {
        requestStreamObserver = null;
        LOG.info("Stream channel on error, reason = {}, node = {}.", t.getMessage(), nodeId);
    }

    private void onStreamChannelComplete() {
        requestStreamObserver = null;
        LOG.info("Stream channel on complete, node = {}.", nodeId);
    }

    @Override
    public void update(ElectionId electionId) {
        this.electionId = electionId;
        sendMasterArbitration(electionId);
    }

    private void awaitConnection(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
