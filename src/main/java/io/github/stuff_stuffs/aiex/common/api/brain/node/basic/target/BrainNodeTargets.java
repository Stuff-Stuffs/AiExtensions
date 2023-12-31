package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.target;

import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestBounds;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestEntry;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventType;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryName;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.UnreachableAreaOfInterestSet;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import net.minecraft.entity.Entity;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public static <C extends Entity, R, FC, A extends AreaOfInterest> BrainNode<C, Optional<R>, FC> areaTarget(final AreaOfInterestType<A> type, final EventExtractor<C, R, FC, AreaOfInterestEntry<A>> extractor, final int radius, final boolean dynamic) {
        return new AbstractSingleTargetingBrainNode<>(dynamic) {
            @Override
            protected Optional<R> query(final BrainContext<C> context, final FC arg) {
                final int x = context.entity().getBlockX();
                final int y = context.entity().getBlockY();
                final int z = context.entity().getBlockZ();
                final AreaOfInterestBounds bounds = new AreaOfInterestBounds(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
                return extractor.extract(context, arg, ((AiWorldExtensions) context.world()).aiex$getAoiWorld().intersecting(bounds, type));
            }
        };
    }

    public static <C extends Entity, A extends AreaOfInterest, FC> BrainNode<C, Optional<AreaOfInterestEntry<A>>, FC> findReachable(final MemoryName<UnreachableAreaOfInterestSet> unreachableName, final AreaOfInterestType<A> type, final AreaFilter<C, A, FC> filter, final int radius, final boolean dynamic) {
        return BrainNodeTargets.areaTarget(type, (context, arg, events) -> {
            final var opt = context.brain().memories().get(unreachableName);
            if (opt.isPresent()) {
                final UnreachableAreaOfInterestSet memory = opt.get().get();
                events = events.filter(entry -> !memory.contains(entry.reference()));
            }
            return events.filter(entry -> filter.test(context, arg, entry)).findAny();
        }, radius, dynamic);
    }

    public interface AreaFilter<C, A extends AreaOfInterest, FC> {
        boolean test(BrainContext<C> context, FC arg, AreaOfInterestEntry<A> entry);
    }

    public interface EntityFilter<C, E extends Entity, FC> {
        boolean test(BrainContext<C> context, FC arg, E entity);
    }

    public interface EventExtractor<C, R, FC, E> {
        Optional<R> extract(BrainContext<C> context, FC arg, Stream<E> events);
    }

    private BrainNodeTargets() {
    }
}
