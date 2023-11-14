package io.github.stuff_stuffs.aiex.common.impl.brain.resource;

import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BrainResourcesImpl extends AbstractBrainResourcesImpl implements BrainResources {
    private final Object2ReferenceMap<Identifier, ResourceTokenEntry> map = new Object2ReferenceOpenHashMap<>();

    @Override
    public Optional<Token> get(final BrainResource resource) {
        final ResourceTokenEntry entry = map.get(resource.id());
        final BrainResource.Priority priority = resource.priority();
        if (entry == null || (priority == BrainResource.Priority.ACTIVE && entry.active == null) || (priority == BrainResource.Priority.PASSIVE && entry.passive == null)) {
            final TokenImpl token = new TokenImpl(resource, null);
            final ResourceTokenEntry newEntry;
            if (entry == null) {
                newEntry = new ResourceTokenEntry(priority == BrainResource.Priority.ACTIVE ? token : null, priority == BrainResource.Priority.PASSIVE ? token : null);
            } else {
                newEntry = entry.with(token, priority);
            }
            if (priority == BrainResource.Priority.ACTIVE && newEntry.passive != null) {
                newEntry.passive.active = false;
            }
            map.put(resource.id(), newEntry);
            return Optional.of(token);
        }
        return Optional.empty();
    }

    @Override
    public void release(final Token token) {
        final TokenImpl casted = ((TokenImpl) token);
        casted.active = false;
        if (casted.parent != null) {
            if (casted.parent.activeChild == casted) {
                casted.parent.activeChild = null;
            }
        } else {
            final Identifier id = token.resource().id();
            final ResourceTokenEntry entry = map.get(id);
            if (entry != null) {
                if (entry.hasOther(casted.resource.priority())) {
                    final ResourceTokenEntry newEntry = entry.with(null, casted.resource.priority());
                    if (casted.resource.priority() == BrainResource.Priority.ACTIVE) {
                        newEntry.passive.active = true;
                    }
                    map.put(id, newEntry);
                } else {
                    map.remove(id);
                }
            }
        }
    }

    @Override
    public void clear() {
        for (final ResourceTokenEntry value : map.values()) {
            if (value.active != null) {
                value.active.active = false;
            }
            if (value.passive != null) {
                value.passive.active = false;
            }
        }
        map.clear();
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
            return active && (parent == null || (parent.activeChild == this && parent.active()));
        }
    }

    private record ResourceTokenEntry(@Nullable TokenImpl active, @Nullable TokenImpl passive) {
        public boolean hasOther(final BrainResource.Priority priority) {
            return switch (priority) {
                case ACTIVE -> passive != null;
                case PASSIVE -> active != null;
            };
        }

        public boolean has(final BrainResource.Priority priority) {
            return switch (priority) {
                case ACTIVE -> active != null;
                case PASSIVE -> passive != null;
            };
        }

        public ResourceTokenEntry with(final TokenImpl token, final BrainResource.Priority priority) {
            return switch (priority) {
                case ACTIVE -> new ResourceTokenEntry(token, passive);
                case PASSIVE -> new ResourceTokenEntry(active, token);
            };
        }
    }
}
