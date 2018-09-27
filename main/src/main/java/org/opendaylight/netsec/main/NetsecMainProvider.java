/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.netsec.main.collection.FlowStatsCollection;
import org.opendaylight.netsec.main.flow.FlowWriterService;
import org.opendaylight.netsec.main.flow.InitialFlowWriter;
import org.opendaylight.netsec.main.handler.PacketHandler;
import org.opendaylight.netsec.persistence.api.NetsecFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netsec.netsec.config.rev180901.NetsecConfig;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetsecMainProvider {

    private static final Logger LOG = LoggerFactory.getLogger(NetsecMainProvider.class);

    private Registration topoNodeListenerReg;
    private Registration flowWriterReg;

    private final DataBroker dataBroker;
    private final NetsecConfig netsecConfig;
    private final NotificationProviderService notificationService;
    private final SalFlowService salFlowService;
    private FlowStatsCollection statsCollection;
    private final NetsecFlowService netsecService;

    public NetsecMainProvider(final DataBroker dataBroker,
                              final NetsecConfig netsecConfig,
                              final NotificationProviderService notificationService,
                              final SalFlowService salFlowService,
                              final NetsecFlowService netsecService) {
        this.dataBroker = dataBroker;
        this.netsecConfig = netsecConfig;
        this.notificationService = notificationService;
        this.salFlowService = salFlowService;
        this.netsecService = netsecService;
    }

    /**
     * Method called when the blueprint container is created.
     */
    public void init() {
        LOG.info("NetsecMainProvider Session Initiated");

        // Write initial flows
        if (netsecConfig.isIsInstallDropallFlow()) {
            LOG.info("Netsec will install a dropall flow on each switch");
            InitialFlowWriter initialFlowWriter = new InitialFlowWriter(salFlowService);
            initialFlowWriter.setFlowTableId(netsecConfig.getDropallFlowTableId());
            initialFlowWriter.setFlowPriority(netsecConfig.getDropallFlowPriority());
            initialFlowWriter.setFlowIdleTimeout(netsecConfig.getDropallFlowIdleTimeout());
            initialFlowWriter.setFlowHardTimeout(netsecConfig.getDropallFlowHardTimeout());
            topoNodeListenerReg = initialFlowWriter.registerAsDataChangeListener(dataBroker);
        } else {
            LOG.info("Dropall flows will not be installed");
        }

        if (netsecConfig.isIsLearningOnlyMode()) {
            LOG.info("Netsec is in Learning Only Mode");
        } else {
            // Setup reactive flow writer
            LOG.info("Netsec will react to network traffic and install flows");
            FlowWriterService flowWriterService = new FlowWriterService(salFlowService, dataBroker);
            flowWriterService.setFlowTableId(netsecConfig.getReactiveFlowTableId());
            flowWriterService.setFlowPriority(netsecConfig.getReactiveFlowPriority());
            flowWriterService.setFlowIdleTimeout(netsecConfig.getReactiveFlowIdleTimeout());
            flowWriterService.setFlowHardTimeout(netsecConfig.getReactiveFlowHardTimeout());

            PacketHandler packetHandler = new PacketHandler(netsecConfig, flowWriterService);
            flowWriterReg = notificationService.registerNotificationListener(packetHandler);
        }

        statsCollection = new FlowStatsCollection(dataBroker);
        new Thread(statsCollection, "StatsCollection").start();

    }

    /**
     * Method called when the blueprint container is destroyed.
     */
    public void close() {
        if (flowWriterReg != null) {
            flowWriterReg.close();
        }

        if (topoNodeListenerReg != null) {
            topoNodeListenerReg.close();
        }
        LOG.info("PacketHandler (instance {}) torn down.", this);
        LOG.info("NetsecMainProvider Closed");
    }
}
