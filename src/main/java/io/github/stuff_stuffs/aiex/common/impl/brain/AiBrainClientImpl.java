package io.github.stuff_stuffs.aiex.common.impl.brain;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrain;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventType;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryName;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryReference;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryType;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class AiBrainClientImpl implements AiBrain {
    private long age;

    @Override
    public ServerPlayerEntity fakePlayerDelegate() {
        return null;
    }

    @Override
    public boolean hasFakePlayerDelegate() {
        return false;
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    public void writeNbt(final NbtCompound nbt) {

    }

    @Override
    public void readNbt(final NbtCompound nbt) {

    }

    @Override
    public long age() {
        return age;
    }

    @Override
    public BrainConfig config() {
        return new BrainConfig() {
            @Override
            public <T> T get(final Key<T> key) {
                return key.defaultValue();
            }
        };
    }

    @Override
    public Events events() {
        return new Events() {
            @Override
            public void remember(final AiBrainEvent event) {

            }

            @Override
            public boolean forget(final AiBrainEvent event) {
                return false;
            }

            @Override
            public List<AiBrainEvent> query(final long since, final boolean reversed) {
                return Collections.emptyList();
            }

            @Override
            public Stream<AiBrainEvent> streamQuery(final long since, final boolean reversed) {
                return Stream.empty();
            }

            @Override
            public <T extends AiBrainEvent> List<T> query(final AiBrainEventType<T> type, final long since, final boolean reversed) {
                return Collections.emptyList();
            }

            @Override
            public <T extends AiBrainEvent> Stream<T> streamQuery(final AiBrainEventType<T> type, final long since, final boolean reversed) {
                return Stream.empty();
            }
        };
    }

    @Override
    public Memories memories() {
        return new Memories() {
            @Override
            public boolean has(final MemoryName<?> memory) {
                return false;
            }

            @Override
            public boolean has(final MemoryReference<?> reference) {
                return false;
            }

            @Override
            public <T> Optional<MemoryName<T>> getName(final MemoryReference<T> reference) {
                return Optional.empty();
            }

            @Override
            public <T> Optional<Memory<T>> get(final MemoryName<T> memory) {
                return Optional.empty();
            }

            @Override
            public <T> Optional<Memory<T>> get(final MemoryReference<T> memory) {
                return Optional.empty();
            }

            @Override
            public <T> MemoryReference<T> add(final MemoryType<T> type, final T value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> void put(final MemoryName<T> name, final T value) {

            }

            @Override
            public boolean forget(final MemoryReference<?> memory) {
                return false;
            }

            @Override
            public boolean forget(final MemoryName<?> name) {
                return false;
            }
        };
    }

    @Override
    public BrainResources resources() {
        return new BrainResources() {
            @Override
            public Optional<Token> get(final BrainResource resource) {
                return Optional.empty();
            }

            @Override
            public void release(final Token token) {

            }
        };
    }
}
