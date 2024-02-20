package io.github.stuff_stuffs.aiex.common.api.brain.behavior;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import net.minecraft.entity.Entity;

public interface Behavior {
    BehaviorType<?> type();

    boolean isPrimitive();

    <E extends Entity> BrainNode<E, Boolean, Unit> primitive(BehaviorState<E> state);
}
