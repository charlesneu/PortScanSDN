/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.persistence.impl;

import java.util.List;
import org.opendaylight.netsec.persistence.NetsecFlow;
import org.opendaylight.netsec.persistence.NetsecService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetsecServiceImpl implements NetsecService {

    private static final Logger LOG = LoggerFactory.getLogger(NetsecServiceImpl.class);

    private NetsecRepository netsecRepository;

    public void setNetsecRepository(NetsecRepository netsecRepository) {

        LOG.debug("Createin repository");
        this.netsecRepository = netsecRepository;
    }

    @Override
    public List<NetsecFlow> list() {
        LOG.debug("LIST");
        return netsecRepository.list();
    }

    @Override
    public void create(NetsecFlow flow) {
        LOG.debug("CREATE");
        netsecRepository.create(flow);
    }

    @Override
    public void delete(String... names) {
        LOG.debug("DELETE");
        netsecRepository.delete(names);
    }
}
