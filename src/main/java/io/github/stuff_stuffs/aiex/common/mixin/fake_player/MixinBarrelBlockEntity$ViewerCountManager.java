package io.github.stuff_stuffs.aiex.common.mixin.fake_player;

import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

@Mixin(targets = "net.minecraft.block.entity.BarrelBlockEntity$1")
public class MixinBarrelBlockEntity$ViewerCountManager {
    @Shadow
    @Final
    BarrelBlockEntity field_27208;

    @Inject(method = "isPlayerViewing", at = @At("HEAD"), cancellable = true)
    private void hook(final PlayerEntity player, final CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof AiFakePlayer fake) {
            final WeakReference<Inventory> reference = fake.openInventory;
            if (reference != null && reference.refersTo(field_27208)) {
                cir.setReturnValue(true);
            }
        }
    }
}
