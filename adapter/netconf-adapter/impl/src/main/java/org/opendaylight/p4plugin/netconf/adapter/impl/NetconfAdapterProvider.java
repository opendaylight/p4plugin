/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfAdapterProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfAdapterProvider.class);

    private final DataBroker dataBroker;
    private DeviceInterfaceDataOperator deviceInterfaceDataOperator;
    private NetconfStateChangeListener netconfStateChangeListener;

    public NetconfAdapterProvider(final DataBroker dataBroker,
                                  DeviceInterfaceDataOperator deviceInterfaceDataOperator) {
        this.dataBroker = dataBroker;
        this.deviceInterfaceDataOperator = deviceInterfaceDataOperator;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("register netconfstate listener");
        netconfStateChangeListener = new NetconfStateChangeListener(deviceInterfaceDataOperator);
        dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<Node>(
                LogicalDatastoreType.OPERATIONAL, netconfStateChangeListener.getNodeId()), netconfStateChangeListener);
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        LOG.info("Driver provider closed.");
    }

}
