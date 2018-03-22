/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package stub;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.p4plugin.p4runtime.proto.*;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionId;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionIdGenerator;
import org.opendaylight.p4plugin.runtime.impl.stub.RuntimeStub;

public class RuntimeStubTest {
    @InjectMocks
    private RuntimeStub runtimeStub = new RuntimeStub("127.0.0.1", 50051, (long)0, "node0");

    @Mock
    ManagedChannel channel;

    @Mock
    private StreamObserver<StreamMessageRequest> requestStreamObserver;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

//    @Test
//    public void testElectionIdObserver() {
//        ElectionId electionId1 = new ElectionId((long)100,(long)200);
//        ElectionIdGenerator.getInstance().setElectionId(electionId1);
//        Assert.assertEquals("", runtimeStub.getElectionId(), electionId1);
//    }

    @Test
    public void testShutdown() {
        runtimeStub.shutdown();
        Mockito.verify(channel).shutdown();
    }

    @Test(expected = RuntimeException.class)
    public void testSetPipelineConfig() {
        SetForwardingPipelineConfigRequest request = Mockito.mock(SetForwardingPipelineConfigRequest.class);
        runtimeStub.setPipelineConfig(request);
    }

    @Test(expected = RuntimeException.class)
    public void testGetPipelineConfig() {
        GetForwardingPipelineConfigRequest request = Mockito.mock(GetForwardingPipelineConfigRequest.class);
        runtimeStub.getPipelineConfig(request);
    }

//    @Test
//    public void testSendMasterArbitration() {
//        Mockito.doNothing().when(requestStreamObserver).onNext(Mockito.any());
//        runtimeStub.sendMasterArbitration());
//        Mockito.verify(requestStreamObserver).onNext(Mockito.any());
//    }

    @Test(expected = RuntimeException.class)
    public void testRead() {
        ReadRequest request = Mockito.mock(ReadRequest.class);
        runtimeStub.read(request);
    }

    @Test(expected = RuntimeException.class)
    public void testWrite() {
        WriteRequest request = Mockito.mock(WriteRequest.class);
        runtimeStub.write(request);
    }

    @After
    public void after() {}
}
