/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl.connection;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.opendaylight.p4plugin.core.impl.NotificationServiceProvider;
import org.opendaylight.p4plugin.core.impl.device.DeviceManager;
import org.opendaylight.p4plugin.core.impl.utils.Utils;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.packet.rev170808.P4PacketReceivedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An abstract runtime stub, including blocking and async stubs, and encapsulates
 * various methods for the upper layers.
 */
public class P4RuntimeStub {
    private static final Logger LOG = LoggerFactory.getLogger(P4RuntimeStub.class);
    private final P4RuntimeChannel runtimeChannel;
    private final P4RuntimeGrpc.P4RuntimeBlockingStub blockingStub;
    private final P4RuntimeGrpc.P4RuntimeStub asyncStub;
    private StreamChannel streamChannel;

    public P4RuntimeStub(String nodeId, Long deviceId, String ip, Integer port) {
        runtimeChannel = FlyweightFactory.getInstance().getChannel(ip, port);
        blockingStub = P4RuntimeGrpc.newBlockingStub(runtimeChannel.getManagedChannel());
        asyncStub = P4RuntimeGrpc.newStub(runtimeChannel.getManagedChannel());
        streamChannel = new StreamChannel(nodeId, deviceId);
    }

    private P4RuntimeGrpc.P4RuntimeBlockingStub getBlockingStub() {
        return blockingStub;
    }

    private P4RuntimeGrpc.P4RuntimeStub getAsyncStub() {
        return asyncStub;
    }

    /**
     * Open the bi-directional stream channel.
     * @return The result of the stream channel connection.
     */
    public boolean connect() {
        streamChannel.openStreamChannel();
        return streamChannel.getStreamChannelState();
    }

    public SetForwardingPipelineConfigResponse setPipelineConfig(SetForwardingPipelineConfigRequest request) {
        return getBlockingStub().setForwardingPipelineConfig(request);
    }

    public GetForwardingPipelineConfigResponse getPipelineConfig(GetForwardingPipelineConfigRequest request) {
        return getBlockingStub().getForwardingPipelineConfig(request);
    }

    public WriteResponse write(WriteRequest request) {
        return getBlockingStub().write(request);
    }

    public Iterator<ReadResponse> read(ReadRequest request) {
        return getBlockingStub().read(request);
    }

    /**
     * Send packet through stream channel, not support metadata now.
     * @param payload
     */
    public void transmitPacket(byte[] payload) {
        streamChannel.transmitPacket(payload);
    }

    public void sendMasterArbitration() {
        streamChannel.sendMasterArbitration();
    }

    public void shutdown() {
        streamChannel.shutdown();
    }

    /**
     * There is a single StreamChannel bi-directional stream per (P4RuntimeStub, Switch)
     * pair. The first thing a controller needs to do when it opens the stream is send a
     * MasterArbitrationUpdate message, advertising its election id. This message includes
     * a device id. All subsequent arbitration & packet IO messages on that stream will be
     * for that device.
     */
    private class StreamChannel {
        private final Long deviceId;
        private final String nodeId;
        private StreamObserver<StreamMessageRequest> observer;
        private CountDownLatch countDownLatch;

        private StreamChannel(String nodeId, Long deviceId) {
            this.deviceId = deviceId;
            this.nodeId = nodeId;
        }

        boolean getStreamChannelState() {
            boolean state = true;
            try {
                state = !countDownLatch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.info("Get stream channel state exception.");
                e.printStackTrace();
            }
            return state;
        }

        /**
         * Not support for cluster right now, so the election id 0;
         */
        private void sendMasterArbitration() {
            StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
            MasterArbitrationUpdate.Builder masterArbitrationBuilder = MasterArbitrationUpdate.newBuilder();
            Uint128.Builder electionIdBuilder = Uint128.newBuilder();
            electionIdBuilder.setHigh(0);
            electionIdBuilder.setLow(0);
            masterArbitrationBuilder.setDeviceId(deviceId);
            masterArbitrationBuilder.setElectionId(electionIdBuilder);
            requestBuilder.setArbitration(masterArbitrationBuilder);
            observer.onNext(requestBuilder.build());
        }

        /**
         * Not support metadata in p4_v14, we will support it in the near future.
         */
        private void transmitPacket(byte[] payload) {
            StreamMessageRequest.Builder requestBuilder = StreamMessageRequest.newBuilder();
            PacketOut.Builder packetOutBuilder = PacketOut.newBuilder();
            packetOutBuilder.setPayload(ByteString.copyFrom(payload));
            //metadataList.forEach(packetOutBuilder::addMetadata);
            requestBuilder.setPacket(packetOutBuilder);
            observer.onNext(requestBuilder.build());
            //For debug
            LOG.info("Transmit packet = {}.", Utils.bytesToHexString(payload));
        }

        private void onPacketReceived(StreamMessageResponse response) {
            switch(response.getUpdateCase()) {
                case PACKET: {
                    P4PacketReceivedBuilder builder = new P4PacketReceivedBuilder();
                    byte[] payload = response.getPacket().getPayload().toByteArray();
                    builder.setNodeId(nodeId);
                    builder.setPayload(payload);
                    NotificationServiceProvider.getInstance().notify(builder.build());
                    //For debug
                    LOG.info("Receive packet from node = {}, body = {}.", nodeId, Utils.bytesToHexString(payload));
                    break;
                }
                case ARBITRATION:
                case UPDATE_NOT_SET:
                default:break;
            }
        }

        private void onStreamChannelError(Throwable t) {
            runtimeChannel.removeStub(P4RuntimeStub.this);
            DeviceManager.getInstance().removeDevice(nodeId);
            countDownLatch.countDown();
            LOG.info("Stream channel on error, reason = {}, backtrace = {}.", t.getMessage(), t);
        }

        private void onStreamChannelCompleted() {
            runtimeChannel.removeStub(P4RuntimeStub.this);
            countDownLatch.countDown();
            LOG.info("Stream channel on complete.");
        }

        /**
         * Each P4RuntimeStub needs to open a bidirectional stream connection to the server
         * using the streamChannel RPC, the connector should advertise its election id right
         * away using a MasterArbitrationUpdate message.
         */
        void openStreamChannel() {
            runtimeChannel.addStub(P4RuntimeStub.this);
            countDownLatch = new CountDownLatch(1);
            StreamObserver<StreamMessageResponse> response = new StreamObserver<StreamMessageResponse>() {
                @Override
                public void onNext(StreamMessageResponse response) {
                    onPacketReceived(response);
                }
                @Override
                public void onError(Throwable t) {
                    onStreamChannelError(t);
                }
                @Override
                public void onCompleted() {
                    onStreamChannelCompleted();
                }
            };
            observer = getAsyncStub().streamChannel(response);
            sendMasterArbitration();
        }

        void shutdown() {
            observer.onCompleted();
        }
    }
}
