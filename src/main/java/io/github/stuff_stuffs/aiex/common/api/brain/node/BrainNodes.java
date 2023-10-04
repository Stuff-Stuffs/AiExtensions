package io.github.stuff_stuffs.aiex.common.api.brain.node;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;

import java.util.Optional;
import java.util.function.BiFunction;

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
            public void deinit() {

            }
        };
    }

    public static <C, R0, R1, FC> BrainNode<C, Optional<R1>, FC> mapPresent(final BrainNode<C, Optional<R0>, FC> source, final BrainNode<C, R1, R0> present) {
        return mapPresent(source, present, empty());
    }

    public static <C, R0, R1, FC> BrainNode<C, Optional<R1>, FC> mapPresent(final BrainNode<C, Optional<R0>, FC> source, final BrainNode<C, R1, R0> present, final BrainNode<C, Unit, Unit> absent) {
        return present(source, present.adaptResult(Optional::of), absent.adaptResult(r -> Optional.empty()));
    }

    public static <C, R0, R1, FC> BrainNode<C, R1, FC> present(final BrainNode<C, Optional<R0>, FC> source, final BrainNode<C, R1, R0> present, final BrainNode<C, R1, Unit> absent) {
        return source.ifThen(Optional::isPresent, present.adaptArg(Optional::get), absent.adaptArg(p -> Unit.INSTANCE));
    }

    private BrainNodes() {
    }
}
