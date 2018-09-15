/*
 * Copyright © 2018 Cássio Tatsch and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.netsec.main.handler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.netsec.main.flow.FlowWriterService;
import org.opendaylight.netsec.main.flow.NetsecPacket;
import org.opendaylight.netsec.main.util.PacketUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netsec.netsec.config.rev180901.NetsecConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PacketHandler implements PacketProcessingListener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);

    final NetsecConfig netsecConfig;
    final FlowWriterService flowWriterService;

    /* Number of packets received */
    private AtomicInteger packetCount;

    /* Last packet in received, used to not process repeated packets */
    private final Map<Integer, Long> pktInBuffer;

    private final ExecutorService publishExecutor =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public PacketHandler(final NetsecConfig netsecConfig, final FlowWriterService flowWriterService) {
        this.netsecConfig = netsecConfig;
        this.packetCount = new AtomicInteger(0);
        this.pktInBuffer = new HashMap<>();
        this.flowWriterService = flowWriterService;
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
        if (packetCount.incrementAndGet() > netsecConfig.getHandlerPacketCountPurge()) {
            packetCount.getAndSet(0);
            purgePktInBuffer();
        }

        final byte[] rawPacket = packetReceived.getPayload();

        if (rawPacket.length < 52) { //Not TCP or UDP
            return;
        }

        byte[] flags = PacketUtils.extractFlags(rawPacket);

        int flag = (int) flags[0];

        if (flag != 2) {
            LOG.warn("Not SYN Packet, discarded.");
            return;
        }

        NetsecPacket packet = NetsecPacket.builder()
                .etherType(PacketUtils.extractEtherTypeAsInt(rawPacket))
                .srcIpv4(PacketUtils.extractSrcIpAsString(rawPacket) + "/32")
                .dstIpv4(PacketUtils.extractDstIpAsString(rawPacket) + "/32")
                .srcMac(PacketUtils.extractSrcMacAsString(rawPacket))
                .dstMac(PacketUtils.extractDstMacAsString(rawPacket))
                .srcPort((int) PacketUtils.extractSrcPortNumberAsShort(rawPacket))
                .dstPort((int) PacketUtils.extractDstPortNumberAsShort(rawPacket))
                .incomeTime(LocalDateTime.now())
                .ingressPort(packetReceived.getIngress().getValue()).build();

        LOG.debug("Received from {} to {} on port {}.", packet.getSrcIpv4(),
                packet.getDstIpv4(), packet.getDstPort());

        // Process only IPv4 packets
        if (!packet.getEtherType().equals(2048)) {
            LOG.warn("Not Ipv4 {}", packet.getEtherType());
            return;
        }

        // Since all packets sent to SF are PktIn, only need to handle the first
        // one. In OpenFlow 1.5 we'll be able to do the PktIn on TCP Syn only.
        if (bufferPktIn(packet)) {
            LOG.trace("Ipv4PacketInHandler PacketIn buffered");
            return;
        }

        // Get the metadata
        if (packetReceived.getMatch() == null) {
            LOG.error("Ipv4PacketInHandler Cant get packet flow match {}", packetReceived.toString());
            return;
        }

        LOG.info("Publishing NetsecPacket {}", packet.toString());
        publishExecutor.execute(() -> {
            LOG.info("Adding flow");
            Uri inputPort = packet.getIngressPort().firstKeyOf(NodeConnector.class).getId();

            flowWriterService.addBidirecionalFlows(packet);
        });


    }

    /**
     * Decide if packets with the same src/dst IP have already been processed.
     * If they haven't been processed, store the
     * IPs so they will be considered processed.
     *
     * @param netsecPacket destination IP
     * @return {@code true} if the src/dst IP has already been processed, {@code false} otherwise
     */
    private boolean bufferPktIn(final NetsecPacket netsecPacket) {
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
        if (currentMillis - bufferedTime > netsecConfig.getHandlerMaxBufferTime()) {
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
            if (currentMillis - pktInBuffer.get(key) > netsecConfig.getHandlerMaxBufferTime()) {
                // This also removes the entry from the pktInBuffer map and
                // doesnt invalidate iteration
                keySet.remove(key);
            }
        });
    }

    @Override
    public void close() {
        LOG.info("Closing Packet Handler");
    }
}