package com.interview.storage;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class DemoApp {
    private DemoApp() {
    }

    public static void main(String[] args) {
        List<StorageNode> nodes = List.of(
                new StorageNode("node-a", 1_000_000_000_000L),
                new StorageNode("node-b", 1_000_000_000_000L),
                new StorageNode("node-c", 1_000_000_000_000L),
                new StorageNode("node-d", 1_000_000_000_000L)
        );

        StorageCoordinator storage = new StorageCoordinator(
                nodes,
                64,
                8,
                3,
                2,
                1
        );

        byte[] payload = "TB-scale architecture demo: chunks, consistent hashing, quorum, repair."
                .getBytes(StandardCharsets.UTF_8);

        ObjectManifest manifest = storage.put("resume-project.pdf", payload);
        nodes.get(0).setAvailable(false);
        byte[] loaded = storage.get("resume-project.pdf");

        nodes.get(0).setAvailable(true);
        ChunkMetadata firstChunk = manifest.chunks().getFirst();
        nodes.stream()
                .filter(node -> firstChunk.replicaNodeIds().contains(node.id()))
                .findFirst()
                .ifPresent(node -> node.deleteLocalReplica(firstChunk.chunkId()));
        RepairReport report = storage.repair("resume-project.pdf");

        System.out.println("Stored object: " + manifest.objectId());
        System.out.println("Chunks: " + manifest.chunks().size());
        System.out.println("Replica factor: 3, write quorum: 2, read quorum: 1");
        System.out.println("Read while one node was down: " + new String(loaded, StandardCharsets.UTF_8));
        System.out.println("Repair restored replicas: " + report.repairedReplicas());
        nodes.forEach(node -> System.out.println(node.id() + " used bytes: " + node.usedBytes()));
    }
}
