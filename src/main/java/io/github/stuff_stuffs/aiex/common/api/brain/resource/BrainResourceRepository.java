package io.github.stuff_stuffs.aiex.common.api.brain.resource;

import io.github.stuff_stuffs.aiex.common.impl.brain.resource.BrainResourceRepositoryImpl;

import java.util.Optional;

public interface BrainResourceRepository {
    Optional<BrainResources.Token> get(BrainResource resource);

    static Builder builder() {
        return new BrainResourceRepositoryImpl.BuilderImpl();
    }

    static BrainResourceRepository buildEmpty(final BrainResources resources) {
        return BrainResourceRepository.builder().build(resources);
    }

    interface Builder {
        Builder add(BrainResources.Token token);

        BrainResourceRepository build(BrainResources resources);
    }
}
