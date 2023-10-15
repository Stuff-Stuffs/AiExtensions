package io.github.stuff_stuffs.aiex.common.impl.brain.resource;

import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Optional;

public class BrainResourceRepositoryImpl implements BrainResourceRepository {
    private final BrainResources resources;
    private final Map<BrainResource, BrainResources.Token> tokens;

    public BrainResourceRepositoryImpl(final BrainResources resources, final Map<BrainResource, BrainResources.Token> tokens) {
        this.resources = resources;
        this.tokens = tokens;
    }

    @Override
    public Optional<BrainResources.Token> get(final BrainResource resource) {
        final BrainResources.Token token = tokens.get(resource);
        if (token != null && token.active()) {
            final Optional<BrainResources.Token> child = ((AbstractBrainResourcesImpl) resources).createChild(token);
            if (child.isPresent()) {
                return child;
            }
        }
        return resources.get(resource);
    }

    public static final class BuilderImpl implements Builder {
        private final Map<BrainResource, BrainResources.Token> tokens;

        public BuilderImpl() {
            tokens = new Object2ObjectOpenHashMap<>();
        }

        @Override
        public Builder add(final BrainResources.Token token) {
            if (token.active()) {
                tokens.put(token.resource(), token);
            }
            return this;
        }

        @Override
        public BrainResourceRepository build(final BrainResources resources) {
            return new BrainResourceRepositoryImpl(resources, Map.copyOf(tokens));
        }
    }
}
