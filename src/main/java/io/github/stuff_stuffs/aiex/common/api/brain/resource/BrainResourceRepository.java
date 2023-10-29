package io.github.stuff_stuffs.aiex.common.api.brain.resource;

import io.github.stuff_stuffs.aiex.common.impl.brain.resource.BrainResourceRepositoryImpl;

import java.util.Optional;
import java.util.Set;

public interface BrainResourceRepository {
    Optional<BrainResources.Token> get(BrainResource resource);

    void clear();

    static Builder builder() {
        return new BrainResourceRepositoryImpl.BuilderImpl();
    }

    static BrainResourceRepository buildEmpty(final BrainResources resources) {
        return BrainResourceRepository.builder().build(resources);
    }

    interface Builder {
        Builder add(BrainResources.Token token);

        Builder addAll(BrainResourceRepository repository);

        default Builder addAllExcept(final BrainResourceRepository repository, final BrainResource... except) {
            return addAllExcept(repository, Set.of(except));
        }

        Builder addAllExcept(BrainResourceRepository repository, Set<BrainResource> except);

        BrainResourceRepository build(BrainResources resources);
    }
}
