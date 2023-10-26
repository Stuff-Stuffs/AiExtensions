package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.api.entity.AiEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public class MixinEntityType<T extends Entity> {

    @Inject(method = "downcast(Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/Entity;", at = @At("HEAD"), cancellable = true)
    private void hook(final Entity entity, final CallbackInfoReturnable<@Nullable T> cir) {
        if ((Object) this == EntityType.PLAYER) {
            if (entity instanceof AiEntity ai) {
                if (ai.aiex$getBrain().hasFakePlayerDelegate()) {
                    cir.setReturnValue((T) ai.aiex$getBrain().fakePlayerDelegate());
                }
            }
        }
    }
}
