package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.internal.EntityReferenceContainer;
import io.github.stuff_stuffs.aiex.common.internal.InternalServerExtensions;
import io.github.stuff_stuffs.aiex.common.internal.ThreadedTaskExecutor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MixinServer implements InternalServerExtensions {
    @Shadow
    @Final
    protected LevelStorage.Session session;
    private final EntityReferenceContainer aiex$entityRefContainer = new EntityReferenceContainer();
    @Unique
    private final ThreadedTaskExecutor aiex$executor = new ThreadedTaskExecutor();

    @Override
    public EntityReferenceContainer aiex$entityRefContainer() {
        return aiex$entityRefContainer;
    }

    @Inject(method = "save", at = @At("HEAD"))
    private void saveHook(final boolean suppressLogs, final boolean flush, final boolean force, final CallbackInfoReturnable<Boolean> cir) {
        aiex$entityRefContainer.save(session);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tickHook(final BooleanSupplier shouldKeepTicking, final CallbackInfo ci) {
        aiex$executor.tick();
    }

    @Inject(method = "loadWorld", at = @At("RETURN"))
    private void loadHook(final CallbackInfo ci) {
        aiex$entityRefContainer.load(session);
    }

    @Override
    public LevelStorage.Session aiex$session() {
        return session;
    }

    @Override
    public void aiex$submitTask(final AiExApi.Job task) {
        aiex$executor.submit(task);
    }
}
