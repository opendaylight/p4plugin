/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.core.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.p4plugin.core.impl.cluster.ElectionId;
import org.opendaylight.p4plugin.core.impl.cluster.ElectionIdGenerator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.cluster.rev170808.GetElectionIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.cluster.rev170808.GetElectionIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.cluster.rev170808.P4pluginCoreClusterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.core.cluster.rev170808.SetElectionIdInput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.math.BigInteger;
import java.util.concurrent.Future;

public class ClusterServiceProvider implements P4pluginCoreClusterService {
    @Override
    public Future<RpcResult<GetElectionIdOutput>> getElectionId() {
        ElectionId electionId = ElectionIdGenerator.getInstance().getElectionId();
        GetElectionIdOutputBuilder builder = new GetElectionIdOutputBuilder();
        builder.setHigh(BigInteger.valueOf(electionId.getHigh()));
        builder.setLow(BigInteger.valueOf(electionId.getLow()));
        return Futures.immediateFuture(RpcResultBuilder.success(builder.build()).build());
    }

    @Override
    public Future<RpcResult<java.lang.Void>> setElectionId(SetElectionIdInput input) {
        Preconditions.checkArgument(input != null, "Set election Id input is null.");
        long high = input.getHigh().longValue();
        long low = input.getLow().longValue();
        ElectionIdGenerator.getInstance().setElectionId(new ElectionId(high, low));
        return Futures.immediateFuture(RpcResultBuilder.success((Void)null).build());
    }
}
