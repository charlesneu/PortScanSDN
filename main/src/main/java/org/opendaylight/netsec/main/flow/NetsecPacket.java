/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main.flow;

import java.math.BigInteger;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Builder
@Getter
@ToString
public class NetsecPacket {
    private Integer etherType;
    private String flowName;
    private String nodeName;
    private String inPort;
    private String outPort;
    private String srcIpv4;
    private String dstIpv4;
    private String srcMac;
    private String dstMac;
    private Integer srcPort;
    private Integer dstPort;
    private LocalDateTime incomeTime;
    InstanceIdentifier<?> ingressPort;

    /* Flow Stats */
    private Long durationSeconds;
    private Long durationNanoSeconds;
    private BigInteger packetCount;
    private BigInteger byteCount;
}
