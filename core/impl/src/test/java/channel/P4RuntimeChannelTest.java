/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package channel;

import com.google.inject.Inject;
import io.grpc.ManagedChannel;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeChannel;
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeStub;
import org.opendaylight.p4plugin.p4runtime.proto.P4RuntimeGrpc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class P4RuntimeChannelTest {
    @InjectMocks
    P4RuntimeChannel p4RuntimeChannel = new P4RuntimeChannel("127.0.0.1", 50051);

    @Mock
    ManagedChannel channel;

    @Spy
    List<P4RuntimeStub> stubs = new ArrayList<>();

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Mock
    P4RuntimeStub stub;

    @Test
    public void testGetManagedChannel() {
        Assert.assertEquals(channel, p4RuntimeChannel.getManagedChannel());
    }

    @Test
    public void testAddStub() {
        p4RuntimeChannel.addStub(stub);
        Mockito.verify(stubs).add(stub);
        Assert.assertEquals(1, stubs.size());
        Assert.assertTrue(stubs.contains(stub));
    }

    @Test
    public void testRemoveStub() {
        p4RuntimeChannel.addStub(stub);
        p4RuntimeChannel.removeStub(stub);
        Mockito.verify(stubs).add(stub);
        Mockito.verify(stubs).remove(stub);
        Assert.assertEquals(0, stubs.size());
        Assert.assertFalse(stubs.contains(stub));
    }

    @Test
    public void testGetStubsCount() {
        p4RuntimeChannel.addStub(stub);
        Mockito.verify(stubs).add(stub);
        Assert.assertEquals((Integer) 1, p4RuntimeChannel.getStubsCount());
        Assert.assertTrue(stubs.contains(stub));

        p4RuntimeChannel.removeStub(stub);
        Mockito.verify(stubs).remove(stub);
        Assert.assertEquals((Integer) 0, p4RuntimeChannel.getStubsCount());
        Assert.assertFalse(stubs.contains(stub));
    }

    @Test
    public void testShutdown() throws InterruptedException {
        Mockito.doReturn(channel).when(channel).shutdown();
        Mockito.doReturn(true).doThrow(new InterruptedException()).when(channel).awaitTermination(5, TimeUnit.SECONDS);
        p4RuntimeChannel.shutdown();
        p4RuntimeChannel.shutdown();
        Mockito.verify(channel, Mockito.timeout(5000).times(2)).shutdown();
    }

    @After
    public void after() {}
}
