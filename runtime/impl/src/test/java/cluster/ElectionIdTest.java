/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package cluster;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionId;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionIdGenerator;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionIdObserver;

import java.util.ArrayList;
import java.util.List;

public class ElectionIdTest {


    @Before
    public void before() {}

    @Test
    public void testObserver() {
        List<ElectionId> list = new ArrayList<>();
        ElectionId electionId1 = new ElectionId((long)1, (long)2);
        ElectionId electionId2 = new ElectionId((long)3, (long)4);
        ElectionIdObserver observer = electionId->list.add(electionId);
        ElectionIdGenerator.getInstance().addObserver(observer);
        ElectionIdGenerator.getInstance().setElectionId(electionId1);
        Assert.assertTrue(list.contains(electionId1));
        ElectionIdGenerator.getInstance().deleteObserver(observer);
        ElectionIdGenerator.getInstance().setElectionId(electionId2);
        Assert.assertFalse(list.contains(electionId2));
    }

    @After
    public void after() {}


}
