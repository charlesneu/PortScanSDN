/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main.handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPacketHandler implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractPacketHandler.class);

    private final NotificationProviderService notificationProviderService;
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public AbstractPacketHandler(NotificationProviderService notificationProviderService) {

        this.notificationProviderService = notificationProviderService;

    }

    @Override
    public void close() {
        LOG.info("TESTE NOF {}", notificationProviderService.toString());
        executorService.shutdown();
    }

    public abstract NotificationListener getConsumedNotificationListener();
}
