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
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeChannel;
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeStub;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testGetManagedChannel() {
        Assert.assertEquals(channel, p4RuntimeChannel.getManagedChannel());
    }

    @Test
    public void testAddStub() {
        P4RuntimeStub stub = Mockito.mock(P4RuntimeStub.class);
        p4RuntimeChannel.addStub(stub);
        Mockito.verify(stubs).add(stub);
        Assert.assertEquals(1, stubs.size());
        Assert.assertTrue(stubs.contains(stub));
    }

    @After
    public void after() {}
}
