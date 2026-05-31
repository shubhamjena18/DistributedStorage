# Distributed Storage System - Java Interview Project

This project models a TB-scale distributed object store in Java. It is intentionally small enough for an interview discussion, but it includes the core architecture choices expected in a scalable storage system.

## What It Demonstrates

- Object chunking for large files.
- Consistent hashing with virtual nodes for balanced placement.
- Replication factor based durability.
- Quorum writes and reads.
- Node failure simulation.
- Metadata manifest per stored object.
- Anti-entropy repair that restores missing replicas.
- Capacity-aware storage nodes.

## Architecture

```text
Client
  |
  v
StorageCoordinator
  |-- Chunker splits object bytes into chunks
  |-- ConsistentHashRing chooses replica nodes
  |-- MetadataStore records object manifests
  |-- StorageNode persists chunk replicas
```

For a TB-scale production version, the same shape maps naturally to:

- API layer: upload/download APIs and auth.
- Metadata plane: strongly consistent metadata store such as FoundationDB, Spanner, etcd, or DynamoDB.
- Data plane: storage nodes that write chunk replicas to disks/object volumes.
- Placement service: consistent hashing plus rack/zone awareness.
- Repair service: background scanner that restores under-replicated chunks.
- Observability: per-node capacity, latency, quorum failures, repair backlog.

## Run

```powershell
mvn -q compile exec:java
```

## Test

```powershell
mvn -q test-compile
java -cp target/classes`;target/test-classes com.interview.storage.StorageSystemTest
```

## Interview Talking Points

1. Why chunking matters: large objects can be distributed, retried, repaired, and streamed independently.
2. Why consistent hashing matters: adding or removing nodes remaps only part of the keyspace.
3. Why quorum matters: the coordinator can tolerate node failures while protecting against partial writes.
4. Why metadata is separate: object manifests need transactional semantics different from bulk chunk storage.
5. How to scale to TB/PB: use erasure coding for cold data, multi-zone placement, background compaction, checksums, and repair queues.
