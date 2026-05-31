package com.interview.storage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public final class ConsistentHashRing {
    private final int virtualNodes;
    private final NavigableMap<Long, StorageNode> ring = new TreeMap<>();

    public ConsistentHashRing(int virtualNodes) {
        if (virtualNodes < 1) {
            throw new IllegalArgumentException("virtualNodes must be at least 1");
        }
        this.virtualNodes = virtualNodes;
    }

    public void addNode(StorageNode node) {
        for (int i = 0; i < virtualNodes; i++) {
            ring.put(hash64(node.id() + "#" + i), node);
        }
    }

    public List<StorageNode> replicasFor(String key, int replicaCount) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("hash ring has no nodes");
        }
        Set<StorageNode> selected = new LinkedHashSet<>();
        long hash = hash64(key);
        NavigableMap<Long, StorageNode> tail = ring.tailMap(hash, true);
        collect(tail, selected, replicaCount);
        if (selected.size() < replicaCount) {
            collect(ring, selected, replicaCount);
        }
        return new ArrayList<>(selected);
    }

    private static void collect(NavigableMap<Long, StorageNode> source, Set<StorageNode> selected, int replicaCount) {
        for (StorageNode node : source.values()) {
            selected.add(node);
            if (selected.size() == replicaCount) {
                return;
            }
        }
    }

    private static long hash64(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            String firstEightBytes = HexFormat.of().formatHex(digest, 0, 8);
            return Long.parseUnsignedLong(firstEightBytes, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
