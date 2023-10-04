package io.github.stuff_stuffs.aiex.common.api.brain.task;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;

public interface Task<R, C> {
    R run(BrainContext<? extends C> context);
}
