package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.registry.Registry;

public final class Memories {
    public static final Memory<Integer> ITEM_USE_COOLDOWN = BasicMemory.builder(Codec.intRange(0, Integer.MAX_VALUE)).build();
    public static final Memory<Integer> ITEM_ATTACK_COOLDOWN = BasicMemory.builder(Codec.intRange(0, Integer.MAX_VALUE)).build();

    public static void init() {
        Registry.register(Memory.REGISTRY, AiExCommon.id("item_use_cooldown"), ITEM_USE_COOLDOWN);
        Registry.register(Memory.REGISTRY, AiExCommon.id("item_attack_cooldown"), ITEM_ATTACK_COOLDOWN);
        MemoryConfig.ON_BUILD_EVENT.register(new MemoryConfig.OnBuild() {
            @Override
            public <T> void onBuild(final T entity, final MemoryConfig.Builder builder) {
                if (entity instanceof AbstractNpcEntity) {
                    if (!builder.has(ITEM_USE_COOLDOWN)) {
                        builder.putDefaultValueFactory(ITEM_USE_COOLDOWN, () -> 0);
                    }
                    if (!builder.has(ITEM_ATTACK_COOLDOWN)) {
                        builder.putDefaultValueFactory(ITEM_ATTACK_COOLDOWN, () -> 0);
                    }
                }
            }
        });
    }

    private Memories() {
    }
}
