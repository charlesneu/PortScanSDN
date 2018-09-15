/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main.flow;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.netsec.main.util.InstanceIdentifierUtils;
import org.opendaylight.netsec.main.util.OpenflowUtils;
import org.opendaylight.openflowplugin.api.OFConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FlowWriterService {
    private static final Logger LOG = LoggerFactory.getLogger(FlowWriterService.class);
    private static final String FLOW_ID_PREFIX = "netsec-";

    private final DataBroker dataBroker;
    private final SalFlowService salFlowService;
    private short flowTableId;
    private int flowPriority;
    private int flowIdleTimeout;
    private int flowHardTimeout;

    private final AtomicLong flowIdInc = new AtomicLong();
    private final AtomicLong flowCookieInc = new AtomicLong(0x2a00000000000000L);

    public FlowWriterService(SalFlowService salFlowService, final DataBroker dataBroker) {
        Preconditions.checkNotNull(salFlowService, "salFlowService should not be null.");
        this.salFlowService = salFlowService;
        this.dataBroker = dataBroker;
    }

    public void setFlowTableId(short flowTableId) {
        this.flowTableId = flowTableId;
    }

    public void setFlowPriority(int flowPriority) {
        this.flowPriority = flowPriority;
    }

    public void setFlowIdleTimeout(int flowIdleTimeout) {
        this.flowIdleTimeout = flowIdleTimeout;
    }

    public void setFlowHardTimeout(int flowHardTimeout) {
        this.flowHardTimeout = flowHardTimeout;
    }


    public void addBidirecionalFlows(NetsecPacket packet) {


        Node node = OpenflowUtils.getOperationalData(dataBroker.newReadOnlyTransaction(),
                packet.getIngressPort().firstIdentifierOf(Node.class));

        List<NodeConnector> nodeConnetorList = node.getNodeConnector();
        NodeConnector inNodeConnector = null;
        NodeConnector outNodeConnector = null;

        Integer inPort;
        Integer outPort;
        if (packet.getDstIpv4().startsWith("10.0.0.12")) {
            outPort = 2;
            inPort = 1;
        } else {
            outPort = 1;
            inPort = 2;
        }

        for (NodeConnector nc : nodeConnetorList) {
            if (("" + inPort).equals(nc.getId().getValue().split(":")[2])) {
                inNodeConnector = nc;
            }
        }

        for (NodeConnector nc : nodeConnetorList) {
            if (("" + outPort).equals(nc.getId().getValue().split(":")[2])) {
                outNodeConnector = nc;
            }
        }

        if (inNodeConnector == null || outNodeConnector == null) {
            LOG.error("input/Output node connector not found.");
            return;
        }

        // add destMac-To-sourceMac flow on source port
        addFlow(packet, OpenflowUtils.getNodeConnectorRef(outNodeConnector.getId()), false);

        // add sourceMac-To-destMac flow on destination port
        addFlow(packet, OpenflowUtils.getNodeConnectorRef(inNodeConnector.getId()), true);
    }

    public void addFlow(NetsecPacket packet, NodeConnectorRef destNodeConnectorRef, boolean response) {

        // get flow table key
        TableKey flowTableKey = new TableKey(flowTableId);

        // build a flow path based on node connector to program flow
        InstanceIdentifier<Flow> flowPath = buildFlowPath(destNodeConnectorRef, flowTableKey);

        // build a flow that target given mac id
        Flow flowBody = createFlow(packet,
                destNodeConnectorRef.getValue().firstIdentifierOf(NodeConnector.class)
                        .firstKeyOf(NodeConnector.class).getId(),
                response);

        // commit the flow in config data
        writeFlowToConfigData(flowPath, flowBody);
    }


    private InstanceIdentifier<Flow> buildFlowPath(NodeConnectorRef nodeConnectorRef, TableKey flowTableKey) {

        // generate unique flow key
        FlowId flowId = new FlowId(FLOW_ID_PREFIX + String.valueOf(flowIdInc.getAndIncrement()));
        FlowKey flowKey = new FlowKey(flowId);

        return InstanceIdentifierUtils.generateFlowInstanceIdentifier(nodeConnectorRef, flowTableKey, flowKey);
    }


    public Flow createFlow(NetsecPacket packet, Uri outputUri, boolean response) {

        // start building flow
        FlowBuilder outputFlow = new FlowBuilder(); //
        outputFlow.setTableId(flowTableId); //
        outputFlow.setFlowName(packet.getFlowName());

        // use its own hash code for id.
        outputFlow.setId(new FlowId(Integer.toString(outputFlow.hashCode())));

        MatchBuilder match = new MatchBuilder();
        match.setEthernetMatch(new EthernetMatchBuilder()
                .setEthernetType(new EthernetTypeBuilder()
                        .setType(new EtherType(Long.valueOf(2048))).build())
                .build());

        match.setIpMatch(new IpMatchBuilder().setIpProtocol((short) 6).build());


        if (response) {
            match.setLayer3Match(new Ipv4MatchBuilder()
                    .setIpv4Destination(new Ipv4Prefix(packet.getSrcIpv4()))
                    .setIpv4Source(new Ipv4Prefix(packet.getDstIpv4())).build());

            match.setLayer4Match(new TcpMatchBuilder()
                    .setTcpDestinationPort(new PortNumber(packet.getSrcPort()))
                    .setTcpSourcePort(new PortNumber(packet.getDstPort())).build());
        } else {

            match.setLayer3Match(new Ipv4MatchBuilder()
                    .setIpv4Destination(new Ipv4Prefix(packet.getDstIpv4()))
                    .setIpv4Source(new Ipv4Prefix(packet.getSrcIpv4())).build());

            match.setLayer4Match(new TcpMatchBuilder()
                    .setTcpDestinationPort(new PortNumber(packet.getDstPort()))
                    .setTcpSourcePort(new PortNumber(packet.getSrcPort())).build());
        }
        List<Action> actions = new ArrayList<Action>();
        // actions.add(getSendToControllerAction());
        // actions.add(getNormalAction());

        actions.add(getSendToOutputAction(outputUri));

        // Create an Apply Action
        ApplyActions applyActions = new ApplyActionsBuilder() //
                .setAction(ImmutableList.copyOf(actions)) //
                .build();

        // Wrap our Apply Action in an Instruction
        Instruction applyActionsInstruction = new InstructionBuilder() //
                .setOrder(0)
                .setInstruction(new ApplyActionsCaseBuilder()//
                        .setApplyActions(applyActions) //
                        .build()) //
                .build();

//        List<Instruction> instr = new ArrayList<>();
//        instr.add(applyActionsInstruction);

        // Put our Instruction in a list of Instructions
        outputFlow
                .setMatch(match.build()) //
                .setInstructions(new InstructionsBuilder() //
                        .setInstruction(ImmutableList.of(applyActionsInstruction)) //
                        .build()) //
                .setPriority(flowPriority) //
                .setBufferId(OFConstants.OFP_NO_BUFFER) //
                .setHardTimeout(flowHardTimeout) //
                .setIdleTimeout(flowIdleTimeout) //
                .setCookie(new FlowCookie(BigInteger.valueOf(flowCookieInc.getAndIncrement())))
                .setFlags(new FlowModFlags(false, false, false, false, false));

        return outputFlow.build();

    }

    /**
     * Creates and returns a OUTPUT action, a respective flow will be redirected
     * to the output port passed by argument.
     *
     * @return a OUTPUT action.
     */
    private Action getSendToOutputAction(Uri outputUri) {
        Action sendToController = new ActionBuilder()
                .setOrder(0)
                .setKey(new ActionKey(0))
                .setAction(new OutputActionCaseBuilder()
                        .setOutputAction(new OutputActionBuilder()
                                .setMaxLength(0xffff)
                                .setOutputNodeConnector(outputUri)
                                .build())
                        .build())
                .build();
        return sendToController;
    }

    /**
     * Starts and commits data change transaction which modifies provided flow
     * path with supplied body.
     *
     * @param flowPath the Flow path
     * @param flow     the Flow
     * @return transaction commit
     */
    private Future<RpcResult<AddFlowOutput>> writeFlowToConfigData(InstanceIdentifier<Flow> flowPath, Flow flow) {
        final InstanceIdentifier<Table> tableInstanceId = flowPath.<Table>firstIdentifierOf(Table.class);
        final InstanceIdentifier<Node> nodeInstanceId = flowPath.<Node>firstIdentifierOf(Node.class);
        final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setFlowRef(new FlowRef(flowPath));
        builder.setFlowTable(new FlowTableRef(tableInstanceId));
        builder.setTransactionUri(new Uri(flow.getId().getValue()));
        LOG.trace("Writing flow");
        return salFlowService.addFlow(builder.build());
    }
}
