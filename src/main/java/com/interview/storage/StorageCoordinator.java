package com.interview.storage;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class StorageCoordinator {
    private final Chunker chunker;
    private final ConsistentHashRing hashRing;
    private final MetadataStore metadataStore;
    private final int chunkSizeBytes;
    private final int replicationFactor;
    private final int writeQuorum;
    private final int readQuorum;
    private final Map<String, StorageNode> nodesById;

    public StorageCoordinator(
            List<StorageNode> nodes,
            int virtualNodes,
            int chunkSizeBytes,
            int replicationFactor,
            int writeQuorum,
            int readQuorum
    ) {
        if (replicationFactor > nodes.size()) {
            throw new IllegalArgumentException("replicationFactor cannot exceed node count");
        }
        this.chunker = new Chunker(chunkSizeBytes);
        this.hashRing = new ConsistentHashRing(virtualNodes);
        this.metadataStore = new MetadataStore();
        this.chunkSizeBytes = chunkSizeBytes;
        this.replicationFactor = replicationFactor;
        this.writeQuorum = writeQuorum;
        this.readQuorum = readQuorum;
        this.nodesById = nodes.stream().collect(Collectors.toMap(StorageNode::id, Function.identity()));
        nodes.forEach(hashRing::addNode);
    }

    public ObjectManifest put(String objectId, byte[] bytes) {
        List<Chunk> chunks = chunker.split(objectId, bytes);
        List<ChunkMetadata> metadata = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            List<StorageNode> replicas = hashRing.replicasFor(chunk.chunkId(), replicationFactor);
            int acknowledgements = 0;
            List<String> replicaIds = new ArrayList<>();

            for (StorageNode node : replicas) {
                if (node.write(chunk)) {
                    acknowledgements++;
                    replicaIds.add(node.id());
                }
            }

            if (acknowledgements < writeQuorum) {
                throw new StorageException("write quorum failed for " + chunk.chunkId());
            }

            metadata.add(new ChunkMetadata(chunk.chunkId(), i, chunk.size(), chunk.checksum(), replicaIds));
        }

        ObjectManifest manifest = new ObjectManifest(objectId, bytes.length, chunkSizeBytes, metadata);
        metadataStore.save(manifest);
        return manifest;
    }

    public byte[] get(String objectId) {
        ObjectManifest manifest = metadataStore.find(objectId)
                .orElseThrow(() -> new StorageException("object not found: " + objectId));
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        manifest.chunks().stream()
                .sorted(Comparator.comparingInt(ChunkMetadata::index))
                .forEach(chunkMetadata -> appendChunk(result, chunkMetadata));

        return result.toByteArray();
    }

    public RepairReport repair(String objectId) {
        ObjectManifest manifest = metadataStore.find(objectId)
                .orElseThrow(() -> new StorageException("object not found: " + objectId));
        int repairedReplicas = 0;

        for (ChunkMetadata chunkMetadata : manifest.chunks()) {
            byte[] healthyCopy = readChunk(chunkMetadata);
            for (String nodeId : chunkMetadata.replicaNodeIds()) {
                StorageNode node = nodesById.get(nodeId);
                if (node != null && node.isAvailable() && !node.hasChunk(chunkMetadata.chunkId())) {
                    node.write(Chunk.existing(chunkMetadata.chunkId(), healthyCopy));
                    repairedReplicas++;
                }
            }
        }
        return new RepairReport(objectId, repairedReplicas);
    }

    public ObjectManifest manifest(String objectId) {
        return metadataStore.find(objectId)
                .orElseThrow(() -> new StorageException("object not found: " + objectId));
    }

    private void appendChunk(ByteArrayOutputStream result, ChunkMetadata metadata) {
        byte[] chunk = readChunk(metadata);
        result.writeBytes(chunk);
    }

    private byte[] readChunk(ChunkMetadata metadata) {
        int reads = 0;
        for (String nodeId : metadata.replicaNodeIds()) {
            StorageNode node = nodesById.get(nodeId);
            if (node == null) {
                continue;
            }
            ReadResult result = node.read(metadata.chunkId()).orElse(null);
            if (result != null && metadata.checksum().equals(Checksum.sha256(result.bytes()))) {
                reads++;
                if (reads >= readQuorum) {
                    return result.bytes();
                }
            }
        }
        throw new StorageException("read quorum failed for " + metadata.chunkId());
    }
}
