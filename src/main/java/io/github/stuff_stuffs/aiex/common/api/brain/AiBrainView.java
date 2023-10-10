package io.github.stuff_stuffs.aiex.common.api.brain;

import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventType;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryEntry;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;

import java.util.List;
import java.util.stream.Stream;

public interface AiBrainView {
    long age();

    long randomSeed();

    BrainConfig config();

    Events events();

    Memories memories();

    BrainResources resources();

    interface Events {
        void remember(AiBrainEvent event);

        boolean forget(AiBrainEvent event);

        List<AiBrainEvent> query(long since, boolean reversed);

        default List<AiBrainEvent> query(final long since) {
            return query(since, false);
        }

        Stream<AiBrainEvent> streamQuery(long since, boolean reversed);

        default Stream<AiBrainEvent> streamQuery(final long since) {
            return streamQuery(since, false);
        }

        <T extends AiBrainEvent> List<T> query(AiBrainEventType<T> type, long since, boolean reversed);

        default <T extends AiBrainEvent> List<T> query(final AiBrainEventType<T> type, final long since) {
            return query(type, since, false);
        }

        <T extends AiBrainEvent> Stream<T> streamQuery(AiBrainEventType<T> type, long since, boolean reversed);

        default <T extends AiBrainEvent> Stream<T> streamQuery(final AiBrainEventType<T> type, final long since) {
            return streamQuery(type, since, false);
        }
    }

    interface Memories {
        boolean has(Memory<?> memory);

        <T> MemoryEntry<T> get(Memory<T> memory);
    }
}
