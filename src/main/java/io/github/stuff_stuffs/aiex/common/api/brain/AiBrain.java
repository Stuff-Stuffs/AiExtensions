package io.github.stuff_stuffs.aiex.common.api.brain;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import io.github.stuff_stuffs.aiex.common.impl.brain.AiBrainImpl;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public interface AiBrain extends AiBrainView {
    ServerPlayerEntity fakePlayerDelegate();

    boolean hasFakePlayerDelegate();

    void tick();

    void writeNbt(NbtCompound nbt);

    void readNbt(NbtCompound nbt);

    static <T extends Entity> AiBrain create(final T entity, final BrainNode<T, Unit, Unit> root, final BrainConfig config, final MemoryConfig memoryConfig, final TaskConfig<T> taskConfig, SpannedLogger logger) {
        return new AiBrainImpl<>(entity, root, config, memoryConfig, taskConfig, entity.getEntityWorld().random.nextLong(), logger);
    }
}
