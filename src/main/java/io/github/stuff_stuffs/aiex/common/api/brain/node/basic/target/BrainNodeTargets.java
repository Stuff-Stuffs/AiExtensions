package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.target;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventType;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import net.minecraft.entity.Entity;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public final class BrainNodeTargets {
    public static <C extends Entity, E extends Entity, FC> BrainNode<C, Optional<E>, FC> entityTarget(final TypeFilter<Entity, E> typeFilter, final EntityFilter<C, E, FC> filter, final boolean dynamic, final int range) {
        return new AbstractSingleTargetingBrainNode<>(dynamic) {
            @Override
            protected Optional<E> query(final BrainContext<C> context, final FC arg) {
                final List<E> entities = new ArrayList<>(1);
                context.world().collectEntitiesByType(typeFilter, Box.of(context.entity().getPos(), range * 2, range * 2, range * 2), e -> filter.test(context, arg, e), entities, 1);
                if (entities.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(entities.get(0));
            }
        };
    }

    public static <C, FC, E extends AiBrainEvent> BrainNode<C, Optional<E>, FC> eventTarget(final AiBrainEventType<E> type, final long buffer, final boolean oldest, final boolean dynamic) {
        return eventTarget(type, (context, arg, events) -> events.findFirst(), buffer, oldest, dynamic);
    }

    public static <C, R, FC, E extends AiBrainEvent> BrainNode<C, Optional<R>, FC> eventTarget(final AiBrainEventType<E> type, final EventExtractor<C, R, FC, E> extractor, final long bufferTime, final boolean oldest, final boolean dynamic) {
        return new AbstractSingleTargetingBrainNode<>(dynamic) {
            @Override
            protected Optional<R> query(final BrainContext<C> context, final FC arg) {
                final AiBrainView.Events events = context.brain().events();
                final Stream<E> query = events.streamQuery(type, context.brain().age() - bufferTime, !oldest);
                return extractor.extract(context, arg, query);
            }
        };
    }

    public interface EntityFilter<C, E extends Entity, FC> {
        boolean test(BrainContext<C> context, FC arg, E entity);
    }

    public interface EventExtractor<C, R, FC, E extends AiBrainEvent> {
        Optional<R> extract(BrainContext<C> context, FC arg, Stream<E> events);
    }

    private BrainNodeTargets() {
    }
}
