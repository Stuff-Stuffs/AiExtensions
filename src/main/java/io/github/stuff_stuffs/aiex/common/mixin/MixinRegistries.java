package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.api.util.AfterRegistryFreezeEvent;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Registries.class)
public class MixinRegistries {
    @Inject(method = "freezeRegistries", at = @At("RETURN"))
    private static void registryFreezeHook(final CallbackInfo ci) {
        AfterRegistryFreezeEvent.EVENT.invoker().afterRegistryFreeze();
    }
}
