/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main.collection;

import java.util.Iterator;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.netsec.main.flow.NetsecPacket;
import org.opendaylight.netsec.main.util.OpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatch;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Collects openflow statistics.
 *
 * @author Cassio Tatsch (tatschcassio@gmail.com)
 */
public class FlowStatsCollection implements Runnable, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FlowStatsCollection.class);

    private boolean stop;
    private DataBroker dataBroker;

    //private TcipsFlowService tcipsService;

    public FlowStatsCollection(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        //this.tcipsService = tcipsService;
    }

    @Override
    public void run() {

        LOG.info("FlowStatsCollection started");
        try {
            while (!stop) {
                collectAllFlowStatistics();
                Thread.sleep(3000);
            }
        } catch (InterruptedException e) {
            LOG.error("Falha no loop principal", e);
        }
    }

    public void collectAllFlowStatistics() {

        // Create a top node identifier
        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();

        // Get all nodes
        InstanceIdentifier<Nodes> nodesID = InstanceIdentifier.create(Nodes.class);
        Nodes nodes = OpenflowUtils.getOperationalData(readOnlyTransaction, nodesID);
        if (nodes == null) {
            return;
        }

        // For each node...
        for (Iterator<Node> iterator = nodes.getNode().iterator(); iterator.hasNext(); ) {
            // Reads child id
            NodeKey childNodeKey = iterator.next().getKey();
            InstanceIdentifier<FlowCapableNode> childNodeRef = InstanceIdentifier
                    .create(Nodes.class)
                    .child(Node.class, childNodeKey)
                    .augmentation(FlowCapableNode.class);
            FlowCapableNode childNode = OpenflowUtils.getOperationalData(readOnlyTransaction, childNodeRef);
            if (childNode != null) {

                // Gets node tables and iterate over it
                for (Iterator<Table> iterator2 = childNode.getTable().iterator(); iterator2.hasNext(); ) {

                    // Gets table id
                    TableKey tableKey = iterator2.next().getKey();
                    InstanceIdentifier<Table> tableRef = InstanceIdentifier
                            .create(Nodes.class).child(Node.class, childNodeKey)
                            .augmentation(FlowCapableNode.class).child(Table.class, tableKey);
                    Table table = OpenflowUtils.getOperationalData(readOnlyTransaction, tableRef);
                    if (table != null) {
                        if (table.getFlow() != null) {

                            // Gets table flows and iterate over it
                            for (Iterator<Flow> iterator3 = table.getFlow().iterator(); iterator3.hasNext(); ) {

                                FlowKey flowKey = iterator3.next().getKey();

                                InstanceIdentifier<Flow> flowRef = InstanceIdentifier
                                        .create(Nodes.class)
                                        .child(Node.class, childNodeKey)
                                        .augmentation(FlowCapableNode.class)
                                        .child(Table.class, tableKey)
                                        .child(Flow.class, flowKey);
                                Flow flow = OpenflowUtils.getOperationalData(readOnlyTransaction, flowRef);
                                if (flow != null) {
                                    Match match = flow.getMatch();

                                    if (match != null && match.getLayer4Match() != null
                                            && match.getLayer3Match() != null) {

                                        Layer3Match layer3Match = match.getLayer3Match();
                                        Layer4Match layer4Match = match.getLayer4Match();
                                        Ipv4Match ipMatch = (Ipv4Match) layer3Match;
                                        TcpMatch tcpMatch = (TcpMatch) layer4Match;

                                        // Add only mapped flows
                                        if (tcpMatch.getTcpDestinationPort() != null
                                                && ipMatch.getIpv4Destination() != null
                                                && ipMatch.getIpv4Source() != null) {

                                            if (flowKey.getId().getValue().startsWith("netsec")) {
                                                NetsecPacket stats = build(childNodeKey.getId().getValue(),
                                                        flow);

                                                //tcipsService.add(stats);
                                                LOG.debug("Got stats from {} - FLOW {} - {}",
                                                        childNodeKey.getId().getValue(),
                                                        flowKey.getId().getValue(), stats);

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a new {@link NetsecPacket} instance using {@link Flow} values.
     *
     * @param flow The {@link Flow} instance.
     */
    public static NetsecPacket build(String nodeName, Flow flow) {
        NetsecPacket.NetsecPacketBuilder tcipsFlow = NetsecPacket.builder();
        FlowStatisticsData data = flow.getAugmentation(FlowStatisticsData.class);
        if (null != data) {

            tcipsFlow.nodeName(nodeName);

            FlowStatistics flosStatistics = data.getFlowStatistics();
            tcipsFlow.packetCount(flosStatistics.getPacketCount().getValue());
            tcipsFlow.byteCount(flosStatistics.getByteCount().getValue());
            tcipsFlow.durationSeconds(flosStatistics.getDuration().getSecond().getValue());
            tcipsFlow.durationNanoSeconds(flosStatistics.getDuration().getNanosecond().getValue());

            Match flowMatch = flow.getMatch();
            if (flowMatch != null) {

                // EthernetMatch ethernetMatch = flowMatch.getEthernetMatch();
                // if (ethernetMatch != null &&
                // ethernetMatch.getEthernetSource() != null
                // && ethernetMatch.getEthernetDestination() != null) {
                //
                // tcipsFlow.setSrcMac(ethernetMatch.getEthernetSource().getAddress().getValue());
                // tcipsFlow.setDstMac(ethernetMatch.getEthernetDestination().getAddress().getValue());
                // } else {
                // LOG.warn("Creating a new TcipsMatch without ethernet
                // match.");
                // }

                Layer3Match layer3Match = flowMatch.getLayer3Match();
                if (layer3Match != null) {
                    if (layer3Match instanceof Ipv4Match) {

                        Ipv4Match ipMatch = (Ipv4Match) layer3Match;
                        tcipsFlow.srcIpv4(ipMatch.getIpv4Source().getValue());
                        tcipsFlow.dstIpv4(ipMatch.getIpv4Destination().getValue());

                    }
                } else {
                    LOG.warn("Creating a new TcipsMatch without layer 4 match.");
                }

                Layer4Match layer4Match = flowMatch.getLayer4Match();
                if (layer4Match != null) {
                    if (layer4Match instanceof TcpMatch) {

                        TcpMatch tcpMatch = (TcpMatch) layer4Match;
                        tcipsFlow.srcPort(tcpMatch.getTcpSourcePort().getValue());
                        tcipsFlow.dstPort(tcpMatch.getTcpDestinationPort().getValue());

                    } else if (layer4Match instanceof UdpMatch) {

                        UdpMatch udpMatch = (UdpMatch) layer4Match;
                        tcipsFlow.srcPort(udpMatch.getUdpSourcePort().getValue());
                        tcipsFlow.dstPort(udpMatch.getUdpDestinationPort().getValue());

                    }
                } else {
                    LOG.warn("Creating a new TcipsMatch without layer 4 match.");
                }
            }
        }
        return tcipsFlow.build();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close() throws Exception {
        this.stop = true;

    }
}