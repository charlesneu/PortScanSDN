/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.persistence.impl;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import org.opendaylight.netsec.persistence.api.NetsecFlow;
import org.opendaylight.netsec.persistence.api.NetsecFlowService;

@Transactional
public class NetsecFlowServiceImpl implements NetsecFlowService {

    @PersistenceContext(unitName = "netsecpu")
    private EntityManager entityManager;

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public void add(NetsecFlow booking) {
        entityManager.persist(booking);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public void add(String flowName) {
        NetsecFlow netsecFlow = new NetsecFlow();
        netsecFlow.setFlowName(flowName);
        entityManager.persist(netsecFlow);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public List<NetsecFlow> list() {
        TypedQuery<NetsecFlow> query = entityManager.createQuery("SELECT n FROM NetsecFlow n", NetsecFlow.class);
        return query.getResultList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    @Override
    public NetsecFlow get(Long id) {
        TypedQuery<NetsecFlow> query = entityManager
             .createQuery("SELECT n FROM NetsecFlow n WHERE n.id=:id", NetsecFlow.class);
        query.setParameter("id", id);
        NetsecFlow netsecFlow = null;
        try {
            netsecFlow = query.getSingleResult();
        } catch (NoResultException e) {
            // nothing to do
        }
        return netsecFlow;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Override
    public void remove(Long id) {
        NetsecFlow netsecFlow = get(id);
        entityManager.remove(netsecFlow);
    }
}