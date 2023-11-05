package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import com.mojang.datafixers.util.Either;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryName;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryReference;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.Optional;
import java.util.function.Function;

public class LoadMemoryNode<C, R, FC> implements BrainNode<C, Optional<Memory<R>>, FC> {
    private final Function<FC, Either<MemoryReference<R>, MemoryName<R>>> memoryExtractor;

    public LoadMemoryNode(final MemoryName<R> name) {
        this(arg -> Either.right(name));
    }

    public LoadMemoryNode(final Function<FC, Either<MemoryReference<R>, MemoryName<R>>> extractor) {
        memoryExtractor = extractor;
    }


    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public Optional<Memory<R>> tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        final Either<MemoryReference<R>, MemoryName<R>> loc = memoryExtractor.apply(arg);
        final AiBrainView.Memories memories = context.brain().memories();
        return loc.map(memories::get, memories::get);
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {

    }
}
