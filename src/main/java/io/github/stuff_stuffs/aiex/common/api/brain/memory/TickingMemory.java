package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;

public interface TickingMemory {
    void tick(BrainContext<?> context);
}
