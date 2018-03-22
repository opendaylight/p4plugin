/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.runtime.impl;

import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionId;
import org.opendaylight.p4plugin.runtime.impl.cluster.ElectionIdGenerator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.cluster.rev170808.GetElectionIdOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.cluster.rev170808.GetElectionIdOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.cluster.rev170808.P4pluginClusterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.p4plugin.cluster.rev170808.SetElectionIdInput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.concurrent.Future;

public class ClusterServiceImpl implements P4pluginClusterService  {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterServiceImpl.class);

    @Override
    public Future<RpcResult<Void>> setElectionId(SetElectionIdInput input) {
        long high = input.getHigh().longValue();
        long low = input.getLow().longValue();
        ElectionIdGenerator.getInstance().setElectionId(new ElectionId(high, low));
        LOG.info("Set election ID RPC success, high = {}, low = {}.", high, low);
        return RpcResultBuilder.success((Void)null).buildFuture();
    }

    @Override
    public Future<RpcResult<GetElectionIdOutput>> getElectionId() {
        ElectionId electionId = ElectionIdGenerator.getInstance().getElectionId();
        GetElectionIdOutputBuilder builder = new GetElectionIdOutputBuilder();
        builder.setHigh(BigInteger.valueOf(electionId.getHigh()));
        builder.setLow(BigInteger.valueOf(electionId.getLow()));
        LOG.info("Get election ID RPC success, high = {}, low = {}.", electionId.getHigh(), electionId.getLow());
        return RpcResultBuilder.success(builder.build()).buildFuture();
    }
}
