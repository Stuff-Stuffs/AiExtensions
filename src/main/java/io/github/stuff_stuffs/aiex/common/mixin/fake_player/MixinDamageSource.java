package io.github.stuff_stuffs.aiex.common.mixin.fake_player;

import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DamageSource.class)
public class MixinDamageSource {
    @Mutable
    @Shadow
    @Final
    private @Nullable Entity source;

    @Mutable
    @Shadow
    @Final
    private @Nullable Entity attacker;

    @Inject(method = "<init>(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;)V", at = @At("RETURN"))
    private void hook(final RegistryEntry<DamageType> type, final Entity source, final Entity attacker, final Vec3d position, final CallbackInfo ci) {
        if (this.source instanceof AiFakePlayer fake) {
            this.source = fake.getDelegate();
        }
        if (this.attacker instanceof AiFakePlayer fake) {
            this.attacker = fake.getDelegate();
        }
    }
}
