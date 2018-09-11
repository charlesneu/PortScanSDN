/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main.handler;

import org.opendaylight.netsec.main.flow.FlowWriterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.netsec.rev180911.NetsecPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.netsec.rev180911.NetsecPacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketHandler implements NetsecPacketListener {

    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    private final FlowWriterService flowWriterService;

    public PacketHandler(FlowWriterService flowWriterService) {
        this.flowWriterService = flowWriterService;
    }

    @Override
    public void onNetsecPacketReceived(NetsecPacketReceived notification) {
        LOG.info("Entrou no onPacketReceived");
    }
}
