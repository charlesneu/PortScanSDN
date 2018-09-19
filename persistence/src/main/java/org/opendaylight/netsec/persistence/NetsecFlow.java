/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.persistence;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "netsec_flow")
public class NetsecFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String lastName;

    @Column
    private String flowName;

    @Column
    private Integer getId;

    @Column
    private Integer getEtherType;

    @Column
    private String getFlowName;

    @Column
    private String getInPort;

    @Column
    private String getOutPort;

    @Column
    private String getSrcIpv4;

    @Column
    private String getDstIpv4;

    @Column
    private String getSrcMac;

    @Column
    private String getDstMac;

    @Column
    private Integer getSrcPort;

    @Column
    private Integer getDstPort;

    @Column
    private String getIncomeDateTime;

    @Column
    private String ingressPort;

    @Column
    private Long getDurationSeconds;

    @Column
    private Long getDurationNanoSeconds;

    @Column
    private Long getPacketCount;

    @Column
    private Long getByteCount;

}
