package io.github.stuff_stuffs.aiex.common.mixin.fake_player;

import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventDispatchManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class MixinServerWorld {
    @Shadow
    @Final
    private GameEventDispatchManager gameEventDispatchManager;

    @Inject(method = "emitGameEvent", at = @At("HEAD"), cancellable = true)
    private void gameEventEmitterHook(final GameEvent event, final Vec3d emitterPos, final GameEvent.Emitter emitter, final CallbackInfo ci) {
        if (emitter.sourceEntity() instanceof AiFakePlayer fakePlayer) {
            gameEventDispatchManager.dispatch(event, emitterPos, GameEvent.Emitter.of(fakePlayer.getDelegate(), emitter.affectedState()));
            ci.cancel();
        }
    }
}
