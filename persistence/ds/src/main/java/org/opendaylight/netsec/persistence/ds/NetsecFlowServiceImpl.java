/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.persistence.ds;

import java.util.List;

import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.opendaylight.netsec.persistence.api.NetsecFlow;
import org.opendaylight.netsec.persistence.api.NetsecFlowService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


@Component(service = NetsecFlowService.class, immediate = true)
public class NetsecFlowServiceImpl implements NetsecFlowService {


    @Reference(target = "(osgi.unit.name=netsecpu)")
    private JpaTemplate jpaTemplate;

    @Override
    public void add(NetsecFlow netsecFlow) {
        jpaTemplate.tx(TransactionType.RequiresNew, entityManager -> {
            entityManager.persist(netsecFlow);
            entityManager.flush();
        });
    }

    @Override
    public void add(String flowName) {
        NetsecFlow netsecFlow = new NetsecFlow();
        netsecFlow.setFlowName(flowName);
        jpaTemplate.tx(TransactionType.RequiresNew, entityManager -> {
            entityManager.persist(netsecFlow);
            entityManager.flush();
        });
    }

    @Override
    public List<NetsecFlow> list() {
        return jpaTemplate.txExpr(TransactionType.Supports,
            entityManager -> entityManager
                 .createQuery("SELECT n FROM NetsecFlow n", NetsecFlow.class).getResultList());
    }

    @Override
    public NetsecFlow get(Long id) {
        return jpaTemplate.txExpr(TransactionType.Supports,
            entityManager -> entityManager.find(NetsecFlow.class, id));
    }

    @Override
    public void remove(Long id) {
        jpaTemplate.tx(TransactionType.RequiresNew, entityManager -> {
            NetsecFlow netsecFlow = entityManager.find(NetsecFlow.class, id);
            if (netsecFlow != null) {
                entityManager.remove(netsecFlow);
            }
        });
    }
}