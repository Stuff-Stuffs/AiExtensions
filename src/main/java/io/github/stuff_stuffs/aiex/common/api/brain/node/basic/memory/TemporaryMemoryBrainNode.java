package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryReference;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryType;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

public class TemporaryMemoryBrainNode<C, M, FC> implements BrainNode<C, MemoryReference<M>, FC> {
    private final MemoryType<M> type;
    private final BrainNode<C, M, FC> initializer;
    private MemoryReference<M> reference;

    public TemporaryMemoryBrainNode(final MemoryType<M> type, final BrainNode<C, M, FC> initializer) {
        this.type = type;
        this.initializer = initializer;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
    }

    @Override
    public MemoryReference<M> tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        final AiBrainView.Memories memories = context.brain().memories();
        if (reference != null) {
            if (memories.has(reference)) {
                return reference;
            }
        }
        final M memory;
        try (final SpannedLogger cur = logger.open("TemporaryMemory")) {
            initializer.init(context, cur);
            memory = initializer.tick(context, arg, logger);
            initializer.deinit(context, cur);
        }
        return reference = memories.add(type, memory);
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        if (reference != null) {
            context.brain().memories().forget(reference);
            reference = null;
        }
    }
}
