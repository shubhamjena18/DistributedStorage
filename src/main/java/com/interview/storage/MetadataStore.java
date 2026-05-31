package com.interview.storage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class MetadataStore {
    private final Map<String, ObjectManifest> manifests = new ConcurrentHashMap<>();

    public void save(ObjectManifest manifest) {
        manifests.put(manifest.objectId(), manifest);
    }

    public Optional<ObjectManifest> find(String objectId) {
        return Optional.ofNullable(manifests.get(objectId));
    }
}
