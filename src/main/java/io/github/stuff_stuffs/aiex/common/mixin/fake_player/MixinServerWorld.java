package io.github.stuff_stuffs.aiex.common.mixin.fake_player;

import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld {
    @Shadow
    public abstract void emitGameEvent(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter);

    @Inject(method = "emitGameEvent", at = @At("HEAD"))
    private void gameEventEmitterHook(final RegistryEntry<GameEvent> event, final Vec3d emitterPos, final GameEvent.Emitter emitter, final CallbackInfo ci) {
        if (emitter.sourceEntity() instanceof final AiFakePlayer fakePlayer) {
            emitGameEvent(event, emitterPos, GameEvent.Emitter.of(fakePlayer.getDelegate(), emitter.affectedState()));
        }
    }
}
