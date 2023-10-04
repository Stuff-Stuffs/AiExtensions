package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.api.entity.AbstractAiMobEntity;
import io.github.stuff_stuffs.aiex.common.internal.entity.DummyGoalSelector;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MobEntity.class)
public class MixinMobEntity {
    @Mutable
    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Mutable
    @Shadow
    @Final
    protected GoalSelector targetSelector;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void replaceGoalSelector(final EntityType<? extends MobEntity> entityType, final World world, final CallbackInfo ci) {
        if ((Object) this instanceof AbstractAiMobEntity) {
            goalSelector = new DummyGoalSelector(world.getProfilerSupplier());
            targetSelector = new DummyGoalSelector(world.getProfilerSupplier());
        }
    }
}
