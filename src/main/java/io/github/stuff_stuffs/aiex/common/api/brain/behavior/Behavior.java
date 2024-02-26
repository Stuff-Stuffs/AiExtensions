package io.github.stuff_stuffs.aiex.common.api.brain.behavior;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;

public sealed interface Behavior<A, R> {
    non-sealed interface Compound<A, R> extends Behavior<A, R> {
        BehaviorType<A, R, ?> type();
    }

    non-sealed interface Primitive<A, R> extends Behavior<A, R> {
        <E> BrainNode<E, Result<R>, A> primitive(BrainContext<E> context);
    }

    sealed interface Result<R> {
    }

    record Failed<R>() implements Result<R> {
        public static final Failed<Object> INSTANCE = new Failed<>();

        public <R0> Failed<R0> cast() {
            //noinspection unchecked
            return (Failed<R0>) this;
        }
    }

    record Continue<R>() implements Result<R> {
        public static final Continue<Object> INSTANCE = new Continue<>();

        public <R0> Continue<R0> cast() {
            //noinspection unchecked
            return (Continue<R0>) this;
        }
    }

    record Done<R>(R result) implements Result<R> {
    }
}
