package io.github.stuff_stuffs.aiex_test.common;

import io.github.stuff_stuffs.aiex_test.common.basic.AiExApi;
import io.github.stuff_stuffs.aiex_test.common.basic.TaskKeys;
import io.github.stuff_stuffs.aiex_test.common.entity.AiExTestEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.mob.MobEntity;

public class AiExTestCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        AiExApi.ENTITY_NAVIGATOR.registerFallback((entity, context) -> {
            if (entity instanceof MobEntity mob) {
                return (pos, error) -> {
                    mob.getNavigation().startMovingTo(pos.x, pos.y, pos.z, mob.getMovementSpeed());
                    return mob.getNavigation().isIdle();
                };
            }
            return null;
        });
        AiExTestEntities.init();
        TaskKeys.init();
    }
}
