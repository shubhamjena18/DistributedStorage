package com.interview.storage;

import java.util.Arrays;
import java.util.Objects;

public final class Chunk {
    private final String chunkId;
    private final byte[] bytes;
    private final String checksum;

    public Chunk(String objectId, int index, byte[] bytes) {
        this(objectId + ":chunk:" + index, bytes);
    }

    public static Chunk existing(String chunkId, byte[] bytes) {
        return new Chunk(chunkId, bytes);
    }

    private Chunk(String chunkId, byte[] bytes) {
        this.chunkId = chunkId;
        this.bytes = Arrays.copyOf(bytes, bytes.length);
        this.checksum = Checksum.sha256(this.bytes);
    }

    public String chunkId() {
        return chunkId;
    }

    public byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public int size() {
        return bytes.length;
    }

    public String checksum() {
        return checksum;
    }

    public boolean hasValidChecksum(byte[] candidate) {
        return Objects.equals(checksum, Checksum.sha256(candidate));
    }
}
