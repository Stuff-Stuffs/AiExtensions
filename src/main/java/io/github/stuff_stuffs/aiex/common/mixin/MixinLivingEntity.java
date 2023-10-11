package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.api.entity.AbstractAiMobEntity;
import io.github.stuff_stuffs.aiex.common.internal.entity.DummyBrain;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @Shadow
    protected Brain<?> brain;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void replaceBrainHook(final EntityType<? extends LivingEntity> entityType, final World world, final CallbackInfo ci) {
        if ((Object) this instanceof AbstractAiMobEntity) {
            brain = DummyBrain.create();
        }
    }
}
