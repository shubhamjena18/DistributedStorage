package com.interview.storage;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class StorageSystemTest {
    private StorageSystemTest() {
    }

    public static void main(String[] args) {
        storesAndReadsObject();
        survivesSingleNodeFailure();
        repairsMissingReplica();
        System.out.println("All storage system tests passed.");
    }

    private static void storesAndReadsObject() {
        StorageCoordinator storage = newStorage();
        byte[] payload = "hello distributed storage".getBytes(StandardCharsets.UTF_8);

        storage.put("obj-1", payload);

        assertEquals("object should round trip", "hello distributed storage",
                new String(storage.get("obj-1"), StandardCharsets.UTF_8));
    }

    private static void survivesSingleNodeFailure() {
        List<StorageNode> nodes = nodes();
        StorageCoordinator storage = new StorageCoordinator(nodes, 32, 5, 3, 2, 1);
        byte[] payload = "availability under node failure".getBytes(StandardCharsets.UTF_8);

        storage.put("obj-2", payload);
        nodes.getFirst().setAvailable(false);

        assertEquals("read should survive one failed node", "availability under node failure",
                new String(storage.get("obj-2"), StandardCharsets.UTF_8));
    }

    private static void repairsMissingReplica() {
        List<StorageNode> nodes = nodes();
        StorageCoordinator storage = new StorageCoordinator(nodes, 32, 4, 3, 2, 1);
        ObjectManifest manifest = storage.put("obj-3", "repair me".getBytes(StandardCharsets.UTF_8));
        ChunkMetadata firstChunk = manifest.chunks().getFirst();
        StorageNode replica = nodes.stream()
                .filter(node -> firstChunk.replicaNodeIds().contains(node.id()))
                .findFirst()
                .orElseThrow();

        replica.deleteLocalReplica(firstChunk.chunkId());
        RepairReport report = storage.repair("obj-3");

        assertTrue("repair should restore at least one replica", report.repairedReplicas() >= 1);
        assertTrue("deleted replica should exist after repair", replica.hasChunk(firstChunk.chunkId()));
    }

    private static StorageCoordinator newStorage() {
        return new StorageCoordinator(nodes(), 32, 6, 3, 2, 1);
    }

    private static List<StorageNode> nodes() {
        return List.of(
                new StorageNode("node-a", 1_000_000),
                new StorageNode("node-b", 1_000_000),
                new StorageNode("node-c", 1_000_000),
                new StorageNode("node-d", 1_000_000)
        );
    }

    private static void assertEquals(String message, String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=[" + expected + "] actual=[" + actual + "]");
        }
    }

    private static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
