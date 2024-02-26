package io.github.stuff_stuffs.aiex.common.api.brain;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.Behavior;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.BehaviorDecider;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import io.github.stuff_stuffs.aiex.common.impl.brain.AiBrainClientImpl;
import io.github.stuff_stuffs.aiex.common.impl.brain.AiBrainImpl;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public interface AiBrain extends AiBrainView {
    ServerPlayerEntity fakePlayerDelegate();

    boolean hasFakePlayerDelegate();

    boolean tick();

    void unload();

    void writeNbt(NbtCompound nbt);

    void readNbt(NbtCompound nbt);

    void submit(Behavior<Unit, Boolean> behavior);

    static <T extends Entity> AiBrain create(final T entity, final BehaviorDecider<T> decider, final BrainConfig config, final TaskConfig<T> taskConfig, final SpannedLogger logger) {
        if (entity.getEntityWorld() instanceof ServerWorld) {
            return new AiBrainImpl<>(entity, decider, config, taskConfig, entity.getEntityWorld().random.nextLong(), logger);
        } else {
            return new AiBrainClientImpl();
        }
    }

    static AiBrain createClient() {
        return new AiBrainClientImpl();
    }
}
