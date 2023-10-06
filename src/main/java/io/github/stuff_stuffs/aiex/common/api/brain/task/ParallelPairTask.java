package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import org.apache.commons.lang3.function.TriFunction;

public class ParallelPairTask<R, R0, R1, C> implements Task<R, C> {
    private final Task<R0, C> first;
    private final Task<R1, C> second;
    private final TriFunction<BrainContext<C>, R0, R1, R> combiner;

    public ParallelPairTask(final Task<R0, C> first, final Task<R1, C> second, final TriFunction<BrainContext<C>, R0, R1, R> combiner) {
        this.first = first;
        this.second = second;
        this.combiner = combiner;
    }

    @Override
    public R run(final BrainContext<C> context) {
        return combiner.apply(context, first.run(context), second.run(context));
    }

    @Override
    public void stop(final AiBrainView context) {
        first.stop(context);
        second.stop(context);
    }
}
