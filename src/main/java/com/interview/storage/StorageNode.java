package com.interview.storage;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class StorageNode {
    private final String id;
    private final long capacityBytes;
    private final AtomicLong usedBytes = new AtomicLong();
    private final AtomicBoolean available = new AtomicBoolean(true);
    private final Map<String, byte[]> chunks = new ConcurrentHashMap<>();

    public StorageNode(String id, long capacityBytes) {
        this.id = id;
        this.capacityBytes = capacityBytes;
    }

    public String id() {
        return id;
    }

    public long usedBytes() {
        return usedBytes.get();
    }

    public long capacityBytes() {
        return capacityBytes;
    }

    public boolean isAvailable() {
        return available.get();
    }

    public void setAvailable(boolean value) {
        available.set(value);
    }

    public boolean write(Chunk chunk) {
        if (!isAvailable()) {
            return false;
        }
        if (usedBytes.get() + chunk.size() > capacityBytes) {
            return false;
        }
        byte[] previous = chunks.putIfAbsent(chunk.chunkId(), chunk.bytes());
        if (previous == null) {
            usedBytes.addAndGet(chunk.size());
        }
        return true;
    }

    public Optional<ReadResult> read(String chunkId) {
        if (!isAvailable()) {
            return Optional.empty();
        }
        byte[] bytes = chunks.get(chunkId);
        if (bytes == null) {
            return Optional.empty();
        }
        return Optional.of(new ReadResult(Arrays.copyOf(bytes, bytes.length), id));
    }

    public boolean hasChunk(String chunkId) {
        return chunks.containsKey(chunkId);
    }

    public void deleteLocalReplica(String chunkId) {
        byte[] removed = chunks.remove(chunkId);
        if (removed != null) {
            usedBytes.addAndGet(-removed.length);
        }
    }
}
