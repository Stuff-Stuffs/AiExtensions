package io.github.stuff_stuffs.aiex.common.mixin.fake_player;

import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageSources.class)
public class MixinDamageSources {
    @Shadow
    @Final
    public Registry<DamageType> registry;

    @Inject(method = "create(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/damage/DamageSource;", at = @At("HEAD"), cancellable = true)
    private void createHook(RegistryKey<DamageType> key, @Nullable Entity attacker, final CallbackInfoReturnable<DamageSource> cir) {
        if (attacker instanceof AiFakePlayer fake) {
            attacker = fake.getDelegate();
            if (key == DamageTypes.PLAYER_ATTACK) {
                key = DamageTypes.MOB_ATTACK;
            }
            cir.setReturnValue(new DamageSource(registry.entryOf(key), attacker));
        }
    }

    @Inject(method = "create(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/entity/Entity;Lnet/minecraft/entity/Entity;)Lnet/minecraft/entity/damage/DamageSource;", at = @At("HEAD"), cancellable = true)
    private void createHook(final RegistryKey<DamageType> key, @Nullable Entity attacker, @Nullable Entity source, final CallbackInfoReturnable<DamageSource> cir) {
        if (attacker instanceof AiFakePlayer || source instanceof AiFakePlayer) {
            if (attacker instanceof AiFakePlayer fake) {
                attacker = fake.getDelegate();
            }
            if (source instanceof AiFakePlayer fake) {
                source = fake.getDelegate();
            }
            cir.setReturnValue(new DamageSource(registry.entryOf(key), attacker, source));
        }
    }
}
