package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryName;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class NamedMemoryLoadBrainNode<C, R, FC> implements BrainNode<C, Optional<Memory<R>>, FC> {
    private final BiFunction<BrainContext<C>, FC, MemoryName<R>> memoryExtractor;

    public NamedMemoryLoadBrainNode(final MemoryName<R> name) {
        this((context, arg) -> name);
    }

    public NamedMemoryLoadBrainNode(final Function<FC, MemoryName<R>> memoryExtractor) {
        this((context, arg) -> memoryExtractor.apply(arg));
    }

    public NamedMemoryLoadBrainNode(final BiFunction<BrainContext<C>, FC, MemoryName<R>> memoryExtractor) {
        this.memoryExtractor = memoryExtractor;
    }


    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public Optional<Memory<R>> tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        final MemoryName<R> loc = memoryExtractor.apply(context, arg);
        final AiBrainView.Memories memories = context.brain().memories();
        return memories.get(loc);
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {

    }
}
