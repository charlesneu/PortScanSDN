/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main.handler;

import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.netsec.main.flow.FlowWriterService;
import org.opendaylight.netsec.main.util.OpenflowUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.netsec.rev180911.NetsecPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.netsec.rev180911.NetsecPacketReceived;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PacketHandler implements NetsecPacketListener {

    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    private final FlowWriterService flowWriterService;

    private final DataBroker dataBroker;

    public PacketHandler(FlowWriterService flowWriterService, final DataBroker dataBroker) {
        this.flowWriterService = flowWriterService;
        this.dataBroker = dataBroker;
    }

    @Override
    public void onNetsecPacketReceived(NetsecPacketReceived packet) {
        LOG.info("Entrou no onPacketReceived");


        InstanceIdentifier<Node> nodeIId = packet.getIngressPort().firstIdentifierOf(Node.class);
        //InstanceIdentifier<NodeConnector> inNodeConnectorIi =
        // packet.getIngressPort().firstIdentifierOf(NodeConnector.class);

        LOG.trace("Pacote: {}", packet.toString());
        Integer outPort = getOutPort(nodeIId, packet);
        LOG.trace("OutPort {}", outPort);
        Node node = OpenflowUtils.getOperationalData(dataBroker.newReadOnlyTransaction(), nodeIId);
        List<NodeConnector> nodeConnetorList = node.getNodeConnector();
        NodeConnector outNodeConnector = null;
        for (NodeConnector nc : nodeConnetorList) {
            LOG.trace("Found {}", nc.getId().getValue());
            if (("" + outPort).equals(nc.getId().getValue().split(":")[2])) {
                outNodeConnector = nc;
            }
        }
        if (outNodeConnector == null) {
            LOG.error("Output node connector not found.");
            return;
        }

//        InstanceIdentifier<NodeConnector> outNodeConnectorIi = InstanceIdentifier.builder(Nodes.class)
//                .child(Node.class, nodeIId.firstKeyOf(Node.class))
//                .child(NodeConnector.class, outNodeConnector.getKey())
//                .build();
    }


    /**
     * Returns the output port number according destination IP address and Node.
     *
     * @param nodeIId the node instance identifier.
     * @param packet  the packet header fields.
     * @return the output port number.
     */
    private Integer getOutPort(InstanceIdentifier<Node> nodeIId, NetsecPacketReceived packet) {


        switch (nodeIId.firstKeyOf(Node.class).getId().getValue().split(":")[1]) {
            case "1":
                switch (packet.getDstIpv4()) {
                    case "10.0.0.11/32":
                        return 1;
                    case "10.0.0.12/32":
                        return 2;
                    case "10.0.0.13/32":
                        return 3;
                    case "10.0.0.21/32":
                    case "10.0.0.31/32":
                    case "10.0.0.32/32":
                    case "10.0.0.33/32":
                    case "10.0.0.41/32":
                    case "10.0.0.42/32":
                        return 4;
                    default:
                        LOG.error("case 1 default");
                }
                break;
            case "2":
                switch (packet.getDstIpv4()) {
                    case "10.0.0.11/32":
                    case "10.0.0.12/32":
                    case "10.0.0.13/32":
                        return 1;
                    case "10.0.0.21/32":
                        return 2;
                    case "10.0.0.31/32":
                    case "10.0.0.32/32":
                    case "10.0.0.33/32":
                        return 3;
                    case "10.0.0.41/32":
                    case "10.0.0.42/32":
                        return 4;
                    default:
                        LOG.error("case 2 default");
                }
                break;
            case "3":
                switch (packet.getDstIpv4()) {
                    case "10.0.0.11/32":
                    case "10.0.0.12/32":
                    case "10.0.0.13/32":
                    case "10.0.0.21/32":
                        return 1;
                    case "10.0.0.31/32":
                        return 2;
                    case "10.0.0.32/32":
                        return 3;
                    case "10.0.0.33/32":
                        return 4;
                    case "10.0.0.41/32":
                    case "10.0.0.42/32":
                        return 1;
                    default:
                        LOG.error("case 3 default");
                }
                break;
            case "4":
                switch (packet.getDstIpv4()) {
                    case "10.0.0.11/32":
                    case "10.0.0.12/32":
                    case "10.0.0.13/32":
                    case "10.0.0.21/32":
                    case "10.0.0.31/32":
                    case "10.0.0.32/32":
                    case "10.0.0.33/32":
                        return 1;
                    case "10.0.0.41/32":
                        return 2;
                    case "10.0.0.42/32":
                        return 3;
                    default:
                        break;
                }
                break;
            default:
                LOG.error("case geral default");
                break;
        }
        return null;
    }
}
