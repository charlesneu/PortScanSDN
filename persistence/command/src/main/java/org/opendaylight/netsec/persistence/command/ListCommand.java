/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netsec.persistence.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;
import org.opendaylight.netsec.persistence.api.NetsecFlow;
import org.opendaylight.netsec.persistence.api.NetsecFlowService;

@Service
@Command(scope = "netsec", name = "list", description = "List current flow")
public class ListCommand implements Action {

    @Reference
    private NetsecFlowService netsecService;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("id");
        table.column("flowName");
        for (NetsecFlow netsecFlow : netsecService.list()) {
            table.addRow().addContent(netsecFlow.getId(), netsecFlow.getFlowName());
        }
        return table;
    }

}