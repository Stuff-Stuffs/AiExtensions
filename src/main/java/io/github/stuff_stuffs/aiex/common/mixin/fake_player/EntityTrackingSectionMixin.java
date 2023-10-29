package io.github.stuff_stuffs.aiex.common.mixin.fake_player;

import io.github.stuff_stuffs.aiex.common.api.entity.AiEntity;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityTrackingSection.class)
public class EntityTrackingSectionMixin<T extends EntityLike> {
    @Shadow @Final private TypeFilterableList<T> collection;

    @Inject(method = "add", at = @At("RETURN"))
    private void addDelegateHook(T entity, CallbackInfo ci) {
        if(entity instanceof AiEntity aiEntity && aiEntity.aiex$getBrain().hasFakePlayerDelegate()) {
            this.collection.add((T) aiEntity.aiex$getBrain().fakePlayerDelegate());
        }
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void removeDelegateHook(T entity, CallbackInfoReturnable<Boolean> cir) {
        if(entity instanceof AiEntity aiEntity && aiEntity.aiex$getBrain().hasFakePlayerDelegate()) {
            this.collection.remove(aiEntity.aiex$getBrain().fakePlayerDelegate());
        }
    }
}
