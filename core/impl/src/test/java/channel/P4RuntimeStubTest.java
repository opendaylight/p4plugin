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
import org.opendaylight.p4plugin.core.impl.cluster.ElectionIdGenerator;
import org.opendaylight.p4plugin.p4runtime.proto.P4RuntimeGrpc;

public class P4RuntimeStubTest {
    @InjectMocks
    P4RuntimeStub stub = new P4RuntimeStub("node0", (long)1, "127.0.0.1", 50051);

    @Mock
    P4RuntimeChannel channel;

    @Mock
    P4RuntimeStub.StreamChannel streamChannel;

    @Mock
    ElectionIdGenerator.ElectionId electionId;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testUpdate() {
        ElectionIdGenerator.ElectionId electionIdNew = new ElectionIdGenerator.ElectionId((long)1,(long)1);
        Mockito.doNothing().when(streamChannel).sendMasterArbitration();
        stub.update(electionIdNew);
        Mockito.verify(streamChannel, Mockito.times(1)).sendMasterArbitration();
        Assert.assertEquals(electionIdNew, stub.getElectionId());
    }

}
