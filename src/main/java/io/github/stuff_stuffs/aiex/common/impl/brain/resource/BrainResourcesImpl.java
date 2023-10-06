package io.github.stuff_stuffs.aiex.common.impl.brain.resource;

import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Optional;

public class BrainResourcesImpl extends AbstractBrainResourcesImpl implements BrainResources {
    private final Object2IntOpenHashMap<BrainResource> resourceCounts = new Object2IntOpenHashMap<>();

    @Override
    public Optional<Token> get(final BrainResource resource) {
        if (resource.maxTicketCount() < 1) {
            return Optional.empty();
        }
        final int tickets = resourceCounts.computeIfAbsent(resource, BrainResource::maxTicketCount);
        if (tickets == 0) {
            return Optional.empty();
        }
        resourceCounts.addTo(resource, -1);
        return Optional.of(new TokenImpl(resource));
    }

    @Override
    public void release(final Token token) {
        if (token.active()) {
            ((TokenImpl) token).active = false;
            final int i = resourceCounts.addTo(token.resource(), 1);
            if (i + 1 == token.resource().maxTicketCount()) {
                resourceCounts.removeInt(token.resource());
            }
        }
    }

    @Override
    public void tick() {
    }

    private static final class TokenImpl implements Token {
        private final BrainResource resource;
        private boolean active = true;

        private TokenImpl(final BrainResource resource) {
            this.resource = resource;
        }

        @Override
        public BrainResource resource() {
            return resource;
        }

        @Override
        public boolean active() {
            return active;
        }
    }
}
