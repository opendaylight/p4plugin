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
import org.mockito.Mockito;
import org.opendaylight.p4plugin.core.impl.channel.FlyweightFactory;
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeChannel;
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeStub;


public class FlyweightFactoryTest {
    FlyweightFactory flyweightFactory = FlyweightFactory.getInstance();

    @Before
    public void before() {}

    @Test
    public void testGetChannel() {
        P4RuntimeChannel p1 = flyweightFactory.getChannel("127.0.0.1", 50051);
        P4RuntimeChannel p2 = flyweightFactory.getChannel("127.0.0.1", 50051);
        Assert.assertEquals("The same ip and port will get the same channel instance", p1, p2);
    }

    @Test
    public void testGc() {
        P4RuntimeChannel p1 = flyweightFactory.getChannel("127.0.0.1", 50051);
        P4RuntimeChannel p2 = flyweightFactory.getChannel("127.0.0.1", 50052);
        p1.addStub(Mockito.mock(P4RuntimeStub.class));
        flyweightFactory.gc();
        Assert.assertEquals("", 1, flyweightFactory.pool.size());
        Assert.assertEquals("", p1, flyweightFactory.getChannel("127.0.0.1", 50051));
    }

    @After
    public void after() {
        flyweightFactory.pool.clear();
    }
}
