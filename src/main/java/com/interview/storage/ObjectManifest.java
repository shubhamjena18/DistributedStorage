package com.interview.storage;

import java.util.List;

public record ObjectManifest(
        String objectId,
        long objectSizeBytes,
        int chunkSizeBytes,
        List<ChunkMetadata> chunks
) {
}

record ChunkMetadata(
        String chunkId,
        int index,
        int sizeBytes,
        String checksum,
        List<String> replicaNodeIds
) {
}
