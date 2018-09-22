/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.persistence.completers;

import java.util.List;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.opendaylight.netsec.persistence.api.NetsecFlow;
import org.opendaylight.netsec.persistence.api.NetsecFlowService;

@Service
public class NetsecIdCompleter implements Completer {

    @Reference
    private NetsecFlowService netsecService;

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        for (NetsecFlow netsec : netsecService.list()) {
            delegate.getStrings().add(String.valueOf(netsec.getId()));
        }
        return delegate.complete(session, commandLine, candidates);
    }

}