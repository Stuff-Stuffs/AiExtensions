package io.github.stuff_stuffs.aiex.common.api;

import io.github.stuff_stuffs.aiex.common.api.entity.AiEntity;
import net.minecraft.entity.Entity;

public interface EntityInteractionContext<C extends Entity & AiEntity> {
    C entity();
}
