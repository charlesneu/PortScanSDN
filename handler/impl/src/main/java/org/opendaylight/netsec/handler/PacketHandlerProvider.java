/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.handler;

import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netsec.handler.config.rev180901.HandlerConfig;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketHandlerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PacketHandlerProvider.class);

    private final NotificationProviderService notificationService;
    private final HandlerConfig handlerConfig;
    private Registration packetHandlerReg;

    public PacketHandlerProvider(
            final NotificationProviderService notificationService,
            final HandlerConfig handlerConfig) {
        this.notificationService = notificationService;
        this.handlerConfig = handlerConfig;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        packetHandlerReg = notificationService
                .registerNotificationListener(new PacketDecoder(handlerConfig, notificationService));
        LOG.info("Netsec Packet Handler Initialized");
    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        if (packetHandlerReg != null) {
            packetHandlerReg.close();
        }
        LOG.info("Netsec Packet Handler Closed");
    }
}
