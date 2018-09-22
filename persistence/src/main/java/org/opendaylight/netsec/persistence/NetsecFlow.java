/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.persistence;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Entity
@Table(name = "netsec_flow")
@NamedQueries(value = {
        @NamedQuery(name = "NetsecFlow.findAll", query = "SELECT n FROM NetsecFlow n"),
        @NamedQuery(name = "NetsecFlow.findByFlowName",
                query = "SELECT n FROM NetsecFlow n WHERE n.flowName = :flowName")
})
public class NetsecFlow implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Integer id;

    @Column
    private String flowName;

    @Column
    private Integer etherType;

    @Column
    private String inPort;

    @Column
    private String outPort;

    @Column
    private String srcIpv4;

    @Column
    private String dstIpv4;

    @Column
    private String srcMac;

    @Column
    private String dstMac;

    @Column
    private Integer srcPort;

    @Column
    private Integer dstPort;

    @Column
    private String ingressPort;

    @Column
    private Long durationSeconds;

    @Column
    private Long durationNanoSeconds;

    @Column
    private Long packetCount;

    @Column
    private Long byteCount;

    @Column
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private Date createdDate;

    @Column
    @Getter(value = AccessLevel.NONE)
    @Setter(value = AccessLevel.NONE)
    private Date updatedDate;

    public Date getCreatedDate() {
        return new Date(createdDate.getTime());
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = new Date(createdDate.getTime());
    }

    public Date getUpdatedDate() {
        return new Date(updatedDate.getTime());
    }

    public void setUpdatedDate(Date updatedDate) {
        this.updatedDate = new Date(updatedDate.getTime());
    }
}
