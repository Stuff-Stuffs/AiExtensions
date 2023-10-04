package io.github.stuff_stuffs.aiex.common.api.entity_reference;

public interface EntityReferencable {
    default EntityReference aiex$getAndUpdateReference() {
        throw new AssertionError("Mixin failed to apply correctly!");
    }
}
