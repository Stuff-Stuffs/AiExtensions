package io.github.stuff_stuffs.aiex.common.api.brain;

import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventType;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryEntry;

import java.util.List;
import java.util.stream.Stream;

public interface AiBrainView {
    long age();

    long randomSeed();

    BrainConfig config();

    Events events();

    Memories memories();

    interface Events {
        void remember(AiBrainEvent event);

        boolean forget(AiBrainEvent event);

        List<AiBrainEvent> query(long since);

        Stream<AiBrainEvent> streamQuery(long since);

        List<AiBrainEvent> queryReversed(long since);

        Stream<AiBrainEvent> streamQueryReversed(long since);

        <T extends AiBrainEvent> List<T> query(AiBrainEventType<T> type, long since);

        <T extends AiBrainEvent> Stream<T> streamQuery(AiBrainEventType<T> type, long since);

        <T extends AiBrainEvent> List<T> queryReversed(AiBrainEventType<T> type, long since);

        <T extends AiBrainEvent> Stream<T> streamQueryReversed(AiBrainEventType<T> type, long since);
    }

    interface Memories {
        boolean has(Memory<?> memory);

        <T> MemoryEntry<T> get(Memory<T> memory);
    }
}
