/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package channel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeChannel;
import org.opendaylight.p4plugin.core.impl.channel.P4RuntimeStub;
import org.opendaylight.p4plugin.core.impl.cluster.ElectionId;

public class P4RuntimeStubTest {
    @InjectMocks
    P4RuntimeStub stub = new P4RuntimeStub("node0", (long)1, "127.0.0.1", 50051);

    @Mock
    P4RuntimeChannel channel;

    @Mock
    P4RuntimeStub.StreamChannel streamChannel;

    @Mock
    ElectionId electionId;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdate() {
        ElectionId electionIdNew = new ElectionId((long)1,(long)1);
        Mockito.doNothing().when(streamChannel).sendMasterArbitration(electionIdNew);
        stub.update(electionIdNew);
        Mockito.verify(streamChannel, Mockito.times(1)).sendMasterArbitration(electionIdNew);
        Assert.assertEquals(electionIdNew, stub.getElectionId());
    }

}
