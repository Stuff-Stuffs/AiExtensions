package io.github.stuff_stuffs.aiex.common.mixin.fake_player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @Redirect(method = {"getBlockBreakingSpeed", "dropInventory", "giveItemStack"}, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerEntity;inventory:Lnet/minecraft/entity/player/PlayerInventory;", opcode = Opcodes.GETFIELD))
    private PlayerInventory hook(final PlayerEntity instance) {
        return instance.getInventory();
    }
}
