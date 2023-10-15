package io.github.stuff_stuffs.aiex.common.impl.brain.resource;

import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.jetbrains.annotations.Nullable;

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
            resourceCounts.removeInt(resource);
            return Optional.empty();
        }
        resourceCounts.addTo(resource, -1);
        return Optional.of(new TokenImpl(resource, null));
    }

    @Override
    public void release(final Token token) {
        if (token.active()) {
            final TokenImpl casted = ((TokenImpl) token);
            casted.active = false;
            if (casted.parent != null) {
                if (casted.parent.activeChild == casted) {
                    casted.parent.activeChild = null;
                }
            } else {
                final int i = resourceCounts.addTo(token.resource(), 1);
                if (i + 1 == token.resource().maxTicketCount()) {
                    resourceCounts.removeInt(token.resource());
                }
            }
        }
    }

    @Override
    public void clear() {
        resourceCounts.clear();
    }

    @Override
    public Optional<Token> createChild(final Token token) {
        final TokenImpl casted = (TokenImpl) token;
        if (!casted.active) {
            return Optional.empty();
        } else if (casted.activeChild != null && casted.activeChild.active) {
            return Optional.empty();
        }
        casted.activeChild = new TokenImpl(casted.resource, casted);
        return Optional.of(casted.activeChild);
    }

    private static final class TokenImpl implements Token {
        private final BrainResource resource;
        private final @Nullable TokenImpl parent;
        private @Nullable TokenImpl activeChild;
        private boolean active = true;

        private TokenImpl(final BrainResource resource, @Nullable final TokenImpl parent) {
            this.resource = resource;
            this.parent = parent;
        }

        @Override
        public BrainResource resource() {
            return resource;
        }

        @Override
        public boolean active() {
            return active && (parent == null || parent.active());
        }
    }
}
