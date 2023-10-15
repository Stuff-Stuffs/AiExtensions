package io.github.stuff_stuffs.aiex.common.api.brain;

import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import net.minecraft.server.world.ServerWorld;

import java.util.Optional;

public interface BrainContext<T> {
    T entity();

    ServerWorld world();

    AiBrainView brain();

    <TR, P, FC> Optional<BrainNode<T, TR, FC>> createTask(TaskKey<TR, P, FC> key, P parameters);

    AiFakePlayer playerDelegate();

    boolean hasPlayerDelegate();

    long randomSeed();
}
