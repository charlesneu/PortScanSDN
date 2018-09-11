/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main;

import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.netsec.main.handler.AbstractPacketHandler;
import org.opendaylight.netsec.main.handler.EthernetHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netsec.netsec.config.rev180901.NetsecConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetsecMainProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NetsecMainProvider.class);


    private final DataBroker dataBroker;
    private final NetsecConfig netsecConfig;
    private final NotificationProviderService notificationService;
    private final SalFlowService salFlowService;
    ImmutableSet<AbstractPacketHandler> packetHandlers;

    public NetsecMainProvider(final DataBroker dataBroker,
                              final NetsecConfig netsecConfig,
                              final NotificationProviderService notificationService,
                              final SalFlowService salFlowService) {
        this.dataBroker = dataBroker;
        this.netsecConfig = netsecConfig;
        this.notificationService = notificationService;
        this.salFlowService = salFlowService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("NetsecMainProvider Session Initiated");
        LOG.info("NetsecMainConfig {}", netsecConfig.isIsLearningOnlyMode());
        packetHandlers = new ImmutableSet.Builder<AbstractPacketHandler>()
                .add(new EthernetHandler(notificationService)).build();

    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        if (Objects.nonNull(packetHandlers) && !packetHandlers.isEmpty()) {
            for (AbstractPacketHandler handler : packetHandlers) {
                handler.close();
            }
        }
        LOG.info("PacketHandler (instance {}) torn down.", this);
        LOG.info("NetsecMainProvider Closed");
    }
}