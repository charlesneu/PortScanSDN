/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.persistence.impl;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import org.opendaylight.netsec.persistence.NetsecFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetsecRepository {

    private static final Logger LOG = LoggerFactory.getLogger(NetsecRepository.class);

    @PersistenceContext(unitName = "netsecpu")
    private EntityManager entityManager;

    @Transactional
    public void create(NetsecFlow person) {
        entityManager.persist(person);
    }

    @Transactional
    public List<NetsecFlow> list() {
        TypedQuery<NetsecFlow> query = entityManager.createQuery(
                "select p from NetsecFlow p order by p.id",
                NetsecFlow.class);

        return new ArrayList<>(query.getResultList());
    }

    @Transactional
    public void delete(String... names) {
        for (String name : names) {
            NetsecFlow person = entityManager.find(NetsecFlow.class, name);
            entityManager.remove(person);
        }
    }
}
