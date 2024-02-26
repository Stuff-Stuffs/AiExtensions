package io.github.stuff_stuffs.aiex.common.impl.brain.behavior;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.Behavior;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.BehaviorDecider;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.BehaviorHandler;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.BehaviorHandlerMap;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BehaviorDeciderImpl<E> implements BehaviorDecider<E> {
    private final Object2ReferenceMap<String, List<Penalty>> penalties;
    private final BehaviorHandlerMap handlerMap;
    private Frame<E, ?, ?> frame;
    private long time;

    public BehaviorDeciderImpl(final BehaviorHandlerMap map) {
        handlerMap = map;
        penalties = new Object2ReferenceOpenHashMap<>();
    }

    @Override
    public void submit(final Behavior<Unit, Boolean> behavior, final BrainContext<E> context) {
        frame = walkDown(behavior, Unit.INSTANCE, new BehaviorHandler.PenaltyInfo("", 0, 0), context);
    }

    @Override
    public boolean tick(final BrainContext<E> context, final SpannedLogger logger) {
        boolean continued = false;
        if (frame != null) {
            continued = frame.tick(context, logger) instanceof Behavior.Continue<?>;
        }
        clearExpiredPenalties();
        time++;
        return continued;
    }

    private double penalty(final String id) {
        final List<Penalty> list = penalties.get(id);
        if (list == null || list.isEmpty()) {
            return 1;
        }
        double acc = 0;
        for (final Penalty penalty : list) {
            acc = acc + penalty.scale * Math.exp(-(time - penalty.startTime()) / penalty.timeScale);
        }
        return 1 + acc;
    }

    private void clearExpiredPenalties() {
        final var iterator = Object2ReferenceMaps.fastIterable(penalties).iterator();
        while (iterator.hasNext()) {
            final var entry = iterator.next();
            final List<Penalty> list = entry.getValue();
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i).expiryTime < time) {
                    list.remove(i);
                } else {
                    break;
                }
            }
            if (list.isEmpty()) {
                iterator.remove();
            }
        }
    }

    private void penalize(final BehaviorHandler.PenaltyInfo info) {
        if (info.scale() < 0.0001 || info.timeScale() < 0.0001 || info.id().isEmpty()) {
            return;
        }
        final List<Penalty> list = penalties.computeIfAbsent(info.id(), k -> new ArrayList<>());
        final long expiry = time + ceil(-info.timeScale() * Math.log(1 / info.scale()));
        final Penalty penalty = new Penalty(time, info.scale(), info.timeScale(), expiry);
        int insertIndex = Collections.binarySearch(list, penalty, Penalty.COMPARATOR);
        if (insertIndex < 0) {
            insertIndex = -insertIndex - 1;
        }
        list.add(insertIndex, penalty);
    }

    private <A, R> @Nullable Frame<E, A, R> walkDown(final Behavior<A, R> behavior, final A arg, final BehaviorHandler.PenaltyInfo info, final BrainContext<E> context) {
        if (behavior instanceof final Behavior.Primitive<A, R> primitive) {
            return new PrimitiveFrame<>(primitive.primitive(context), info, arg);
        }
        final Pair<Double, Frame<E, A, R>> framePair = walkDown0(behavior, arg, info, context, 0, Double.POSITIVE_INFINITY);
        if (framePair == null) {
            return null;
        }
        return framePair.getSecond();
    }

    private <A, R> @Nullable Pair<Double, Frame<E, A, R>> walkDown0(final Behavior<A, R> behavior, final A arg, final BehaviorHandler.PenaltyInfo info, final BrainContext<E> context, final double baseDistance, final double bestSoFar) {
        if (behavior instanceof final Behavior.Primitive<A, R> primitive) {
            return Pair.of(baseDistance + 1, new PrimitiveFrame<>(primitive.primitive(context), info, arg));
        }
        final Behavior.Compound<A, R> compound = (Behavior.Compound<A, R>) behavior;
        double best = Double.POSITIVE_INFINITY;
        Frame<E, A, ?> bestFrame = null;
        BehaviorHandler.PenaltyInfo bestPenalty = null;
        BehaviorHandler.BehaviorList.Node<A, ?> bestNode = null;
        for (final BehaviorHandler<A, R, ?> handler : handlerMap.get(compound.type())) {
            final BehaviorHandler.BehaviorList<A, R> list = cast(handler, compound);
            if (list.size() == 0) {
                continue;
            }
            final BehaviorHandler.BehaviorList.Node<A, ?> first = list.first();
            final BehaviorHandler.PenaltyInfo penaltyInfo = handler.penaltyInfo();
            final double penalty = penalty(penaltyInfo.id());
            final double distance = baseDistance + penalty + 1;
            if (distance >= bestSoFar) {
                continue;
            }
            final Pair<Double, ? extends Frame<E, A, ?>> distancePair = walkDown0(first.behavior(), arg, penaltyInfo, context, distance, best);
            if (distancePair == null) {
                continue;
            }
            if (bestFrame == null) {
                bestFrame = distancePair.getSecond();
                best = distancePair.getFirst();
                bestPenalty = penaltyInfo;
                bestNode = first;
            } else if (distancePair.getFirst() < best) {
                bestFrame = distancePair.getSecond();
                best = distancePair.getFirst();
                bestPenalty = penaltyInfo;
                bestNode = first;
            }
        }
        if (bestFrame == null) {
            return null;
        }
        final CompoundFrame<A, R> compoundFrame = new CompoundFrame<>(bestPenalty, bestFrame, bestNode);
        return Pair.of(best, compoundFrame);
    }

    private <A, R, T extends Behavior.Compound<A, R>> BehaviorHandler.BehaviorList<A, R> cast(final BehaviorHandler<A, R, T> handler, final Behavior.Compound<A, R> compound) {
        //noinspection unchecked
        return handler.handle((T) compound);
    }

    private static long ceil(final double value) {
        final long i = (long) value;
        return value > (double) i ? i + 1 : i;
    }

    private sealed interface Frame<E, A, R> {
        Behavior.Result<R> tick(BrainContext<E> context, SpannedLogger logger);
    }

    private final class CompoundFrame<A, R> implements Frame<E, A, R> {
        private final BehaviorHandler.PenaltyInfo penalty;
        private Frame<E, ?, ?> cursorFrame;
        private BehaviorHandler.BehaviorList.Node<?, ?> cursor;
        private boolean done = false;

        private CompoundFrame(final BehaviorHandler.PenaltyInfo penalty, final Frame<E, ?, ?> cursorFrame, final BehaviorHandler.BehaviorList.Node<?, ?> cursor) {
            this.penalty = penalty;
            this.cursorFrame = cursorFrame;
            this.cursor = cursor;
        }

        @Override
        public Behavior.Result<R> tick(final BrainContext<E> context, final SpannedLogger logger) {
            if (done) {
                throw new RuntimeException();
            }
            return tick0(context, logger, cursorFrame);
        }

        private <A0, R0> Behavior.Result<R> tick0(final BrainContext<E> context, final SpannedLogger logger, final Frame<E, A0, R0> frame) {
            final Behavior.Result<R0> result = frame.tick(context, logger);
            if (result instanceof final Behavior.Continue<R0> cont) {
                return cont.cast();
            } else if (result instanceof final Behavior.Failed<R0> fail) {
                return fail.cast();
            }
            final Behavior.Done<R0> done = (Behavior.Done<R0>) result;
            if (cursor.last()) {
                this.done = true;
                //noinspection unchecked
                return (Behavior.Result<R>) done;
            }
            //noinspection unchecked
            final BehaviorHandler.BehaviorList.Node<A0, R0> cursor = (BehaviorHandler.BehaviorList.Node<A0, R0>) this.cursor;
            final Behavior<R0, ?> next = cursor.next();
            cursorFrame = walkDown(next, done.result(), penalty, context);
            if (cursorFrame == null) {
                return Behavior.Failed.INSTANCE.cast();
            }
            this.cursor = cursor;
            return Behavior.Continue.INSTANCE.cast();
        }
    }

    private final class PrimitiveFrame<A, R> implements Frame<E, A, R> {
        private final BrainNode<E, Behavior.Result<R>, A> node;
        private final BehaviorHandler.PenaltyInfo penalty;
        private final A arg;

        private PrimitiveFrame(final BrainNode<E, Behavior.Result<R>, A> node, final BehaviorHandler.PenaltyInfo penalty, final A arg) {
            this.node = node;
            this.penalty = penalty;
            this.arg = arg;
        }

        @Override
        public Behavior.Result<R> tick(final BrainContext<E> context, final SpannedLogger logger) {
            final Behavior.Result<R> result = node.tick(context, arg, logger);
            if (result instanceof Behavior.Failed<R>) {
                penalize(penalty);
            }
            return result;
        }
    }

    private record Penalty(long startTime, double scale, double timeScale, long expiryTime) {
        private static final Comparator<Penalty> COMPARATOR = Comparator.comparingLong(penalty -> -penalty.expiryTime);
    }
}
