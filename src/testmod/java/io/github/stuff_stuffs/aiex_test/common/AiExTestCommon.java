package io.github.stuff_stuffs.aiex_test.common;

import io.github.stuff_stuffs.aiex_test.common.basic.AiExApi;
import io.github.stuff_stuffs.aiex_test.common.basic.TaskKeys;
import io.github.stuff_stuffs.aiex_test.common.basic.WalkTask;
import io.github.stuff_stuffs.aiex_test.common.entity.AiExTestEntities;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

public class AiExTestCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        AiExApi.ENTITY_NAVIGATOR.registerFallback((entity, context) -> {
            if (entity instanceof MobEntity mob) {
                return new WalkTask.Navigator() {
                    @Override
                    public boolean isPathing() {
                        return mob.getNavigation().isFollowingPath();
                    }

                    @Override
                    public boolean walkTo(final Vec3d pos, final double error) {
                        mob.getNavigation().startMovingTo(pos.x, pos.y, pos.z, 0.25);
                        return mob.getNavigation().isIdle();
                    }
                };
            }
            return null;
        });
        AiExTestEntities.init();
        TaskKeys.init();
    }
}
