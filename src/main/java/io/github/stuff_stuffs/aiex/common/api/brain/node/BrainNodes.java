package io.github.stuff_stuffs.aiex.common.api.brain.node;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryEntry;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BrainNodes {
    public static <C, FC> BrainNode<C, Unit, FC> empty() {
        return constant(Unit.INSTANCE);
    }

    public static <C, R, FC> BrainNode<C, R, FC> constant(final R constant) {
        return terminal((context, fc) -> constant);
    }

    public static <C, R, FC> BrainNode<C, R, FC> terminal(final BiFunction<BrainContext<C>, FC, R> func) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context) {

            }

            @Override
            public R tick(final BrainContext<C> context, final FC arg) {
                return func.apply(context, arg);
            }

            @Override
            public void deinit(BrainContext<C> context) {

            }
        };
    }

    public static <C, R, FC> BrainNode<C, R, FC> expect(final BrainNode<C, Optional<R>, FC> node) {
        return node.adaptResult(Optional::get);
    }

    public static <C, R0, R1, FC> BrainNode<C, Optional<R1>, FC> mapPresent(final BrainNode<C, Optional<R0>, FC> source, final BrainNode<C, R1, R0> present) {
        return mapPresent(source, present, empty());
    }

    public static <C, R0, R1, FC> BrainNode<C, Optional<R1>, FC> mapPresent(final BrainNode<C, Optional<R0>, FC> source, final BrainNode<C, R1, R0> present, final BrainNode<C, Unit, Unit> absent) {
        return present(source, present.adaptResult(Optional::of), absent.adaptResult(r -> Optional.empty()));
    }

    public static <C, R0, R1, FC> BrainNode<C, R1, FC> present(final BrainNode<C, Optional<R0>, FC> source, final BrainNode<C, R1, R0> present, final BrainNode<C, R1, Unit> absent) {
        return source.ifThen((context, r0) -> r0.isPresent(), present.adaptArg(Optional::get), absent.adaptArg(p -> Unit.INSTANCE));
    }

    public static <C, R, FC> BrainNode<C, Optional<R>, FC> remembering(final BiFunction<BrainContext<C>, FC, Memory<R>> memoryFunc, final BiFunction<FC, R, R> combiner) {
        return terminal((context, fc) -> {
            final Memory<R> memory = memoryFunc.apply(context, fc);
            final AiBrainView.Memories memories = context.brain().memories();
            if (!memories.has(memory)) {
                return Optional.empty();
            }
            final MemoryEntry<R> entry = memories.get(memory);
            entry.set(combiner.apply(fc, entry.get()));
            return Optional.of(entry.get());
        });
    }

    public static <C, R, FC> BrainNode<C, R, FC> expect(final BrainNode<C, Optional<R>, FC> node, final Supplier<? extends RuntimeException> errorFactory) {
        return node.adaptResult(res -> res.orElseThrow(errorFactory));
    }

    public static <C, R, FC> BrainNode<C, R, FC> orElse(final BrainNode<C, Optional<R>, FC> node, final R fallback) {
        return node.adaptResult(res -> res.orElse(fallback));
    }

    public static <C, R, FC> BrainNode<C, R, FC> orElseGet(final BrainNode<C, Optional<R>, FC> node, final Function<BrainContext<C>, R> factory) {
        return node.adaptResult((ctx, res) -> res.orElseGet(() -> factory.apply(ctx)));
    }

    public static <C, R, FC> BrainNode<C, Optional<R>, FC> or(final BrainNode<C, Optional<R>, FC> node, final Function<BrainContext<C>, Optional<R>> other) {
        return node.adaptResult((ctx, res) -> res.or(() -> other.apply(ctx)));
    }

    public static <C, R, FC0, FC1> BrainNode<C, Optional<R>, FC0> flatMap(final BrainNode<C, Optional<FC1>, FC0> start, final BrainNode<C, Optional<R>, FC1> map) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context) {
                start.init(context);
                map.init(context);
            }

            @Override
            public Optional<R> tick(final BrainContext<C> context, final FC0 arg) {
                return start.tick(context, arg).flatMap(val -> map.tick(context, val));
            }

            @Override
            public void deinit(BrainContext<C> context) {
                start.deinit(context);
                map.deinit(context);
            }
        };
    }

    private BrainNodes() {
    }
}
