/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main.handler;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthernetHandler extends AbstractPacketHandler implements PacketProcessingListener {

    private static final Logger LOG = LoggerFactory.getLogger(EthernetHandler.class);

    public EthernetHandler(NotificationProviderService notificationProviderService) {
        super(notificationProviderService);
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        LOG.info("Entrou no onPacketReceived");
    }

    public NotificationListener getConsumedNotificationListener() {
        return null;
    }
}
