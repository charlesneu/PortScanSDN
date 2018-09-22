/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.persistence.impl;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.opendaylight.netsec.persistence.NetsecFlow;
import org.opendaylight.netsec.persistence.NetsecService;
import org.ops4j.pax.cdi.api.OsgiServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OsgiServiceProvider(classes = {NetsecService.class})
@Transactional
public class NetsecServiceImpl implements NetsecService {

    private static final Logger LOG = LoggerFactory.getLogger(NetsecServiceImpl.class);

    @PersistenceContext(unitName = "netsecpu")
    private EntityManager entityManager;

    public void setEntityManager(EntityManager entityManager) {
        LOG.debug("Creating entityManager");
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<NetsecFlow> list() {
        LOG.debug("LIST");
        return entityManager.createQuery("select n from NetsecFlow n").getResultList();
    }

    @Override
    public void createOrUpdate(NetsecFlow flow) {

        NetsecFlow flowEntity = (NetsecFlow) entityManager.createNamedQuery("NetsecFlow.findByFlowName")
                .setMaxResults(1)
                .getSingleResult();

        if (Objects.isNull(flowEntity)) {
            flow.setCreatedDate(new Date());
            flow.setUpdatedDate(flow.getCreatedDate());
            entityManager.persist(flow);
        } else {
            flowEntity.setByteCount(flow.getByteCount());
            flowEntity.setPacketCount(flow.getPacketCount());
            flowEntity.setDurationSeconds(flow.getDurationSeconds());
            flowEntity.setDurationNanoSeconds(flow.getDurationNanoSeconds());
            flowEntity.setUpdatedDate(new Date());
            entityManager.merge(flowEntity);
        }
    }
}
