package io.github.stuff_stuffs.aiex.common.impl.brain.resource;

import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Optional;

public class DebugBrainResourcesImpl extends AbstractBrainResourcesImpl implements BrainResources {
    private final Object2IntOpenHashMap<BrainResource> resourceCounts = new Object2IntOpenHashMap<>();
    private final ReferenceQueue<Token> referenceQueue = new ReferenceQueue<>();

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
        return Optional.of(new TokenImpl(resource, referenceQueue));
    }

    @Override
    public void release(final Token token) {
        if (token.active()) {
            ((TokenImpl) token).active = false;
            ((TokenImpl) token).ref.active = false;
            final int i = resourceCounts.addTo(token.resource(), 1);
            if (i + 1 == token.resource().maxTicketCount()) {
                resourceCounts.removeInt(token.resource());
            }
        }
    }

    @Override
    public void tick() {
        TokenRef ref;
        while ((ref = (TokenRef) referenceQueue.poll()) != null) {
            if (ref.active) {
                AiExCommon.LOGGER.error("Somebody didn't return toke for resource {}", ref.resource.id());
            }
        }
    }

    @Override
    public void clear() {
        resourceCounts.clear();
    }

    private static final class TokenRef extends WeakReference<Token> {
        private final BrainResource resource;
        private boolean active;

        public TokenRef(final Token referent, final ReferenceQueue<? super Token> q) {
            super(referent, q);
            resource = referent.resource();
        }
    }

    private static final class TokenImpl implements Token {
        private final BrainResource resource;
        private final TokenRef ref;
        private boolean active = true;

        private TokenImpl(final BrainResource resource, final ReferenceQueue<Token> queue) {
            ref = new TokenRef(this, queue);
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
