/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.persistence.api;

import java.util.List;

public interface NetsecFlowService {

    List<NetsecFlow> list();

    NetsecFlow get(Long id);

    void add(NetsecFlow netsecFlow);

    void add(String flowName);

    void remove(Long id);

}