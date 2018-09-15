/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main.util;

import com.google.common.base.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Provides utility methods for node manipulation.
 *
 * @author Cassio Tatsch (tatschcassio@gmail.com)
 */
public final class OpenflowUtils {

    private OpenflowUtils() {
        // do not create instance
    }

    /**
     * Gets operational data from logical data store.
     *
     * @param <T>                 a object that extends DataObject.
     * @param readOnlyTransaction a read only transaction instance.
     * @param identifier          the element (node, flow, table) instance identifier (path).
     * @return a data container which has structured contents, {@code null} if
     *     no data is present.
     */
    public static <T extends DataObject> T getOperationalData(final ReadTransaction readOnlyTransaction,
                                                              final InstanceIdentifier<T> identifier) {
        Optional<T> optionalData = null;
        try {
            optionalData = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, identifier).get();
            if (optionalData.isPresent()) {
                return optionalData.get();
            }
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
        return null;
    }

    /**
     * Gets configuration data from logical data store.
     *
     * @param <T>                 a object that extends DataObject.
     * @param readOnlyTransaction a read only transaction instance.
     * @param identifier          the element (node, flow, table) instance identifier (path).
     * @return a data container which has structured contents, {@code null} if
     *     no data is present.
     */
    public static <T extends DataObject> T getConfigData(final ReadTransaction readOnlyTransaction,
                                                         final InstanceIdentifier<T> identifier) {
        Optional<T> optionalData = null;
        try {
            optionalData = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, identifier).get();
            if (optionalData.isPresent()) {
                return optionalData.get();
            }
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
        return null;
    }

    /**
     * Gets a table instance identifier from the node and flow table id passed
     * by arguments.
     *
     * @param nodeId      the node instance identifier.
     * @param flowTableId the flow table id.
     * @return a table instance identifier.
     */
    public static InstanceIdentifier<Table> getTableInstanceId(InstanceIdentifier<Node> nodeId, Short flowTableId) {
        // get flow table key
        TableKey flowTableKey = new TableKey(flowTableId);

        return nodeId.builder()
                .augmentation(FlowCapableNode.class)
                .child(Table.class, flowTableKey)
                .build();
    }

    /**
     * Gets a flow instance identifier from the table instance id passed by
     * argument.
     *
     * @param tableId  the table node instance identifier.
     * @param flowName the name of the flow.
     * @return a flow instance identifier.
     */
    public static InstanceIdentifier<Flow> getFlowInstanceId(InstanceIdentifier<Table> tableId, String flowName) {
        // generate unique flow key
        FlowId flowId = new FlowId(flowName);
        FlowKey flowKey = new FlowKey(flowId);
        return tableId.child(Flow.class, flowKey);
    }

    public static NodeConnectorRef getNodeConnectorRef(NodeConnectorId nodeConnectorId) {
        NodeId nodeId = getNodeId(nodeConnectorId);
        return new NodeConnectorRef(InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(nodeId))
                .child(NodeConnector.class, new NodeConnectorKey(nodeConnectorId))
                .build());
    }

    public static NodeId getNodeId(NodeConnectorId nodeConnectorId) {
        if (nodeConnectorId == null) {
            return null;
        }
        String[] tokens = nodeConnectorId.getValue().split(":");
        if (tokens.length == 3) {
            return new NodeId("openflow:" + Long.parseLong(tokens[1]));
        } else {
            return null;
        }
    }
}