package io.github.stuff_stuffs.aiex.common.impl.brain.resource;

import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    @Override
    public void clear() {
        for (final BrainResources.Token value : tokens.values()) {
            resources.release(value);
        }
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
        public Builder addAll(final BrainResourceRepository repository) {
            for (final Map.Entry<BrainResource, BrainResources.Token> entry : ((BrainResourceRepositoryImpl) repository).tokens.entrySet()) {
                final Optional<BrainResources.Token> child = ((AbstractBrainResourcesImpl) (((BrainResourceRepositoryImpl) repository).resources)).createChild(entry.getValue());
                if (child.isPresent()) {
                    tokens.putIfAbsent(entry.getKey(), child.get());
                }
            }
            return this;
        }

        @Override
        public Builder addAllExcept(BrainResourceRepository repository, Set<BrainResource> except) {
            for (final Map.Entry<BrainResource, BrainResources.Token> entry : ((BrainResourceRepositoryImpl) repository).tokens.entrySet()) {
                if(except.contains(entry.getKey())) {
                    continue;
                }
                final Optional<BrainResources.Token> child = ((AbstractBrainResourcesImpl) (((BrainResourceRepositoryImpl) repository).resources)).createChild(entry.getValue());
                if (child.isPresent()) {
                    tokens.putIfAbsent(entry.getKey(), child.get());
                }
            }
            return this;
        }

        @Override
        public BrainResourceRepository build(final BrainResources resources) {
            return new BrainResourceRepositoryImpl(resources, Map.copyOf(tokens));
        }
    }
}
