package com.interview.storage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Chunker {
    private final int chunkSizeBytes;

    public Chunker(int chunkSizeBytes) {
        if (chunkSizeBytes <= 0) {
            throw new IllegalArgumentException("chunkSizeBytes must be positive");
        }
        this.chunkSizeBytes = chunkSizeBytes;
    }

    public List<Chunk> split(String objectId, byte[] bytes) {
        List<Chunk> chunks = new ArrayList<>();
        for (int offset = 0, index = 0; offset < bytes.length; offset += chunkSizeBytes, index++) {
            int end = Math.min(offset + chunkSizeBytes, bytes.length);
            chunks.add(new Chunk(objectId, index, Arrays.copyOfRange(bytes, offset, end)));
        }
        if (chunks.isEmpty()) {
            chunks.add(new Chunk(objectId, 0, new byte[0]));
        }
        return chunks;
    }
}
