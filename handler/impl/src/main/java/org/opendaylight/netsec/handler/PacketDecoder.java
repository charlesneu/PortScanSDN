/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.handler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.netsec.handler.util.PacketUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netsec.handler.config.rev180901.HandlerConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.netsec.rev180911.NetsecPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.netsec.rev180911.NetsecPacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketDecoder implements PacketProcessingListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PacketDecoder.class);

    /* Packet handler configuration */
    private final HandlerConfig handlerConfig;

    /* Number of packets received */
    private AtomicInteger packetCount;

    /* Last packet in received, used to not process repeated packets */
    private final Map<Integer, Long> pktInBuffer;

    final NotificationProviderService notificationService;

    public PacketDecoder(final HandlerConfig handlerConfig, final NotificationProviderService notificationService) {
        this.handlerConfig = handlerConfig;
        this.packetCount = new AtomicInteger(0);
        this.pktInBuffer = new HashMap<>();
        this.notificationService = notificationService;
        LOG.info("PacketIn listener started");
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        LOG.trace("PacketIn Received");

        if (Objects.isNull(packetReceived)) {
            return;
        }

        packetCount.incrementAndGet();
        // If number of packets receiver too high, purge buffer
        if (packetCount.incrementAndGet() > handlerConfig.getPacketCountPurge()) {
            packetCount.getAndSet(0);
            purgePktInBuffer();
        }

        final byte[] rawPacket = packetReceived.getPayload();

        byte[] flags = PacketUtils.extractFlags(rawPacket);

        int flag = (int) flags[0];

        if (flag != 2) {
            LOG.warn("Not SYN Packet, discarded.");
            return;
        }

        // Creates database instance
        NetsecPacketReceived npReceived = new NetsecPacketReceivedBuilder()
                .setEtherType(PacketUtils.extractEtherTypeAsInt(rawPacket))
                .setSrcIpv4(PacketUtils.extractSrcIpAsString(rawPacket) + "/32")
                .setDstIpv4(PacketUtils.extractDstIpAsString(rawPacket) + "/32")
                .setSrcMac(PacketUtils.extractSrcMacAsString(rawPacket))
                .setDstMac(PacketUtils.extractDstMacAsString(rawPacket))
                .setSrcPort((int) PacketUtils.extractSrcPortNumberAsShort(rawPacket))
                .setDstPort((int) PacketUtils.extractDstPortNumberAsShort(rawPacket))
                .setIncomeDateTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)).build();

        LOG.debug("Received from {} to {} on port {}.", npReceived.getSrcIpv4(),
                npReceived.getDstIpv4(), npReceived.getDstPort());

        // Process only IPv4 packets
        if (!npReceived.getEtherType().equals(2048)) {
            LOG.warn("Not Ipv4 {}", npReceived.getEtherType());
            return;
        }

        // Since all packets sent to SF are PktIn, only need to handle the first
        // one. In OpenFlow 1.5 we'll be able to do the PktIn on TCP Syn only.
        if (bufferPktIn(npReceived)) {
            LOG.trace("Ipv4PacketInHandler PacketIn buffered");
            return;
        }

        // Get the metadata
        if (packetReceived.getMatch() == null) {
            LOG.error("Ipv4PacketInHandler Cant get packet flow match {}", packetReceived.toString());
            return;
        }

        LOG.error("Publishing NetsecPacketReceived {}", packetReceived.toString());
        notificationService.publish(npReceived);

    }

    /**
     * Decide if packets with the same src/dst IP have already been processed. If they haven't been processed, store the
     * IPs so they will be considered processed.
     *
     * @param srcIpStr source IP
     * @param dstIpStr destination IP
     * @return {@code true} if the src/dst IP has already been processed, {@code false} otherwise
     */
    private boolean bufferPktIn(final NetsecPacketReceived netsecPacket) {
        int key = netsecPacket.toString().hashCode();
        long currentMillis = System.currentTimeMillis();

        Long bufferedTime = pktInBuffer.get(key);

        // If the entry does not exist, add it and return false indicating the
        // packet needs to be processed
        if (Objects.isNull(bufferedTime)) {
            // Add the entry
            pktInBuffer.put(key, currentMillis);
            return false;
        }

        // If the entry is old, update it and return false indicating the packet
        // needs to be processed
        if (currentMillis - bufferedTime > handlerConfig.getMaxBufferTime()) {
            // Update the entry
            pktInBuffer.put(key, currentMillis);
            return false;
        }

        return true;
    }

    /**
     * Purge packets that have been in the PktIn buffer too long.
     */
    private void purgePktInBuffer() {
        long currentMillis = System.currentTimeMillis();
        Set<Integer> keySet = pktInBuffer.keySet();
        keySet.forEach((key) -> {
            if (currentMillis - pktInBuffer.get(key) > handlerConfig.getMaxBufferTime()) {
                // This also removes the entry from the pktInBuffer map and
                // doesnt invalidate iteration
                keySet.remove(key);
            }
        });
    }

    @Override
    public void close() {

    }

}
