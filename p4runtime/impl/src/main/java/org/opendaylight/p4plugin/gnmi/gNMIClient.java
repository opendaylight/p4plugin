/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.gnmi;

import com.google.common.util.concurrent.*;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.opendaylight.p4plugin.channel.ChannelFactory;
import org.opendaylight.p4plugin.gnmi.proto.SubscribeRequest;
import org.opendaylight.p4plugin.gnmi.proto.SubscribeResponse;
import org.opendaylight.p4plugin.gnmi.proto.gNMIGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class gNMIClient {
    private static final Logger LOG = LoggerFactory.getLogger(gNMIClient.class);
    private String nodeId;
    private String ip;
    private Integer port;
    private gNMIGrpc.gNMIBlockingStub blockingStub;
    private gNMIGrpc.gNMIStub asyncStub;
    private StreamObserver<SubscribeRequest> requestStreamObserver;
    //private PacketInHandler packetInHandler;
    private boolean isFinished;
    private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    public gNMIClient(String ip, Integer port, String nodeId) {
        ManagedChannel managedChannel = getManagedChannel(ip, port);
        this.nodeId = nodeId;
        this.ip = ip;
        this.port = port;
        this.blockingStub = gNMIGrpc.newBlockingStub(managedChannel);
        this.asyncStub = gNMIGrpc.newStub(managedChannel);
    }

    private ManagedChannel getManagedChannel(String ip , Integer port) {
        String key = String.format("%s:%d", ip, port);
        return ChannelFactory.getInstance().getManagedChannel(key);
    }

    public void subscribe(SubscribeRequest request) {
        StreamObserver<SubscribeResponse> responseStreamObserver = new StreamObserver<SubscribeResponse>() {
            @Override
            public void onNext(SubscribeResponse subscribeResponse) {
                switch(subscribeResponse.getResponseCase()){
                    case UPDATE:
                        org.opendaylight.p4plugin.gnmi.proto.Notification notification = subscribeResponse.getUpdate();
                        DataStorage.getInstance().writeData2Tsdr(nodeId, notification);
                        break;
                    case SYNC_RESPONSE:
                    case ERROR:
                    case RESPONSE_NOT_SET:
                        break;
                    default:break;
                }
            }

            @Override
            public void onError(Throwable throwable) {
                isFinished = false;
            }

            @Override
            public void onCompleted() {

            }
        };

        StreamObserver<SubscribeRequest> requestStreamObserver = asyncStub.subscribe(responseStreamObserver);
        requestStreamObserver.onNext(request);
        ListenableFuture<Boolean> listenableFuture = executorService.submit(() -> {
            while(!isFinished) {
              try {
                  TimeUnit.SECONDS.sleep(1);
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
            }
            return true;
        });

        Futures.addCallback(listenableFuture, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(@NullableDecl Boolean aBoolean) {
                if(aBoolean) {
                    requestStreamObserver.onCompleted();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
                LOG.error("Listenable future failed.");
            }
        });
    }
}
