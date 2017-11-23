/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package channel;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.p4plugin.core.impl.channel.FlyweightFactory;
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeChannel;

import java.util.concurrent.ConcurrentHashMap;

@RunWith(MockitoJUnitRunner.class)
public class FlyweightFactoryTest {
    @Spy
    @InjectMocks
    FlyweightFactory flyweightFactory;

    @Spy
    ConcurrentHashMap<String, P4RuntimeChannel> pool;

    @Before
    public void before() {}

    @Test
    public void testGetAnExistChannel() {
        P4RuntimeChannel p1 = Mockito.mock(P4RuntimeChannel.class);
        Mockito.doReturn(p1).when(pool).get("127.0.0.1:50051");
        P4RuntimeChannel p2 = flyweightFactory.getChannel("127.0.0.1", 50051);
        P4RuntimeChannel p3 = flyweightFactory.getChannel("127.0.0.1", 50051);
        Mockito.verify(pool, Mockito.times(2)).get("127.0.0.1:50051");
        Assert.assertEquals("Get an exist channel for the first time", p1, p2);
        Assert.assertEquals("Get an exist channel for the second time", p1, p3);
    }

    @Test
    public void testGetNoneExistChannel() {
        P4RuntimeChannel p1 = Mockito.mock(P4RuntimeChannel.class);
        Mockito.doReturn(p1).when(flyweightFactory).makeP4RuntimeChannel("127.0.0.1", 50051);
        P4RuntimeChannel p2 = flyweightFactory.getChannel("127.0.0.1", 50051);
        Mockito.verify(flyweightFactory).makeP4RuntimeChannel("127.0.0.1", 50051);
        Assert.assertEquals("Get an none-exist channel", p1, p2);
    }

    @Test
    public void testGc() {
        P4RuntimeChannel p1 = Mockito.mock(P4RuntimeChannel.class);
        P4RuntimeChannel p2 = Mockito.mock(P4RuntimeChannel.class);
        Mockito.doReturn(1).when(p1).getStubsCount();
        Mockito.doReturn(0).when(p2).getStubsCount();
        Mockito.doReturn(p1).when(flyweightFactory).makeP4RuntimeChannel("127.0.0.1", 50051);
        Mockito.doReturn(p2).when(flyweightFactory).makeP4RuntimeChannel("127.0.0.1", 50052);
        pool.put("127.0.0.1:50051", p1);
        pool.put("127.0.0.1:50052", p2);
        Mockito.doNothing().when(p2).shutdown();
        Mockito.doReturn(true).when(pool).remove("127.0.0.1", 50052);
        flyweightFactory.gc();
        Mockito.verify(p2).shutdown();
        Mockito.verify(pool).remove("127.0.0.1:50052");
        Assert.assertEquals("Test GC", p1, flyweightFactory.getChannel("127.0.0.1", 50051));
        Assert.assertEquals("Test GC", 1, pool.size());
    }

    @After
    public void after() {}
}
