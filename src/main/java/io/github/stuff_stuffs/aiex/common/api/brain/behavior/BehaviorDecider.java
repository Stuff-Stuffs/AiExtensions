package io.github.stuff_stuffs.aiex.common.api.brain.behavior;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import io.github.stuff_stuffs.aiex.common.impl.brain.behavior.BehaviorDeciderImpl;

public interface BehaviorDecider<E> {
    void submit(Behavior<Unit, Boolean> behavior, BrainContext<E> context);

    boolean tick(BrainContext<E> context, SpannedLogger logger);

    static <E> BehaviorDecider<E> create(final BehaviorHandlerMap map) {
        return new BehaviorDeciderImpl<>(map);
    }
}
