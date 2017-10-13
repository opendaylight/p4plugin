/*
 * Copyright © 2017 zte and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.p4plugin.netconf.adapter.impl;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
//import org.opendaylight.yang.gen.v1.urn.ietf.interfaces.test.rev170908.NodeInterfacesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;


public class NetconfStateChangeListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfStateChangeListener.class);

    //private DeviceInterfaceDataOperator deviceInterfaceDataOperator;
    private static final InstanceIdentifier<Node> NETCONF_NODE_IID = InstanceIdentifier
            .create(NetworkTopology.class).child(Topology.class, new TopologyKey(new TopologyId(TopologyNetconf
                    .QNAME.getLocalName()))).child(Node.class);

    public NetconfStateChangeListener() {
        //this.deviceInterfaceDataOperator = deviceInterfaceDataOperator;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        LOG.info("Netconf nodes change!");
        Node nodeBefore = null;
        Node nodeAfter = null;
        Node nodeInfo = null;
        for (DataTreeModification<Node> change: changes) {
            DataObjectModification<Node> rootNode = change.getRootNode();
            nodeBefore = rootNode.getDataBefore();
            nodeAfter = rootNode.getDataAfter();
            if (nodeBefore != null) {
                nodeInfo  = nodeBefore;
            } else {
                nodeInfo = nodeAfter;
            }
            if (nodeInfo == null) {
                LOG.info("Netconf node info null");
                continue;
            }
            if (nodeInfo.getNodeId().getValue().equals("controller-config")) {
                LOG.info("Netconf node {} ignored",rootNode.getDataAfter().getNodeId().getValue());
                continue;
            }
            switch (rootNode.getModificationType()) {
                case WRITE:
                    LOG.info("Node {} was created", nodeAfter.getNodeId().getValue());
                    break;
                case SUBTREE_MODIFIED:
                    LOG.info("Process modify procedure");
                    NetconfNode ncNodeNew = nodeAfter.getAugmentation(NetconfNode.class);
                    NetconfNode ncNodeOld = nodeBefore.getAugmentation(NetconfNode.class);
                    if ((ncNodeNew.getConnectionStatus() == NetconfNodeConnectionStatus.ConnectionStatus.Connected)
                            && (ncNodeOld.getConnectionStatus() != NetconfNodeConnectionStatus
                            .ConnectionStatus.Connected)) {
                        LOG.info("Node {} was connected", nodeAfter.getNodeId().getValue());
                        LOG.info("Start write interfaces");
//                        deviceInterfaceDataOperator.writeInterfacesToDevice(nodeAfter.getNodeId().getValue());
//
//                        LOG.info("Start read interfaces");
//                        NodeInterfacesState interfacesData = deviceInterfaceDataOperator
//                                .readInterfacesFromDevice(nodeAfter.getNodeId().getValue());
//                        if (null == interfacesData) {
//                            LOG.info("InterFacesData is null");
//                        }
//                        if (null != interfacesData.getNode() && 0 != interfacesData.getNode().size()) {
//                            LOG.info("NodeList from device is {}", interfacesData.getNode());
//                        }
//
//                        LOG.info("Send p4-device Info to module core");
//                        deviceInterfaceDataOperator.sendP4DeviceInfo(interfacesData.getNode());
//
//                        LOG.info("Start write device interfaces info to controller data store");
//                        deviceInterfaceDataOperator.writeInterfacesToControllerDataStore(interfacesData.getNode());
//
//                        LOG.info("Start read interfaces from controller data store");
//                        NodeInterfacesState data1 = deviceInterfaceDataOperator.readInterfacesFromControllerDataStore();
//                        LOG.info("Data from controller data store is {}", data1);
//
//                        LOG.info("Start read interfaces from controller data store again");
//                        NodeInterfacesState data2 = deviceInterfaceDataOperator.readInterfacesFromControllerDataStore();
//                        LOG.info("Data from controller data store is {}", data2);

                    }
                    break;
                case DELETE:
                    LOG.info("Node {} was deleted", nodeBefore.getNodeId().getValue());
                    //do something, such as remove notification
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type : {}"
                            + change.getRootNode().getModificationType());
            }
        }
    }

    public InstanceIdentifier<Node> getNodeId() {
        return NETCONF_NODE_IID;
    }

}
