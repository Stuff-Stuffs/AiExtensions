package io.github.stuff_stuffs.aiex.common.api.brain.behavior;

import net.minecraft.entity.Entity;

import java.util.List;

public interface BehaviorHandler<T extends Behavior> {
    <E extends Entity> List<WeightedBehavior> handle(T behaviour, BehaviorState<E> state);

    record WeightedBehavior(Behavior behavior, double weight) {
    }
}
