package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import io.github.stuff_stuffs.aiex.common.internal.InternalServerExtensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.listener.GameEventDispatchManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements AiWorldExtensions {
    @Shadow
    public abstract MinecraftServer getServer();

    @Shadow
    @Final
    private GameEventDispatchManager gameEventDispatchManager;

    @Override
    public @Nullable EntityReference aiex$getEntityReference(final UUID uuid) {
        return ((InternalServerExtensions) getServer()).aiex$entityRefContainer().get(uuid);
    }

    @Inject(method = "emitGameEvent", at = @At("HEAD"), cancellable = true)
    private void gameEventEmitterHook(final GameEvent event, final Vec3d emitterPos, final GameEvent.Emitter emitter, final CallbackInfo ci) {
        if (emitter.sourceEntity() instanceof AiFakePlayer fakePlayer) {
            gameEventDispatchManager.dispatch(event, emitterPos, GameEvent.Emitter.of(fakePlayer.getDelegate(), emitter.affectedState()));
            ci.cancel();
        }
    }
}
