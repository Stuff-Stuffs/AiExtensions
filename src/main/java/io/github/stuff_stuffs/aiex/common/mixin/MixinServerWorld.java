package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import io.github.stuff_stuffs.aiex.common.internal.InternalServerExtensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements AiWorldExtensions {
    @Shadow
    public abstract MinecraftServer getServer();

    @Override
    public @Nullable EntityReference aiex$getEntityReference(final UUID uuid) {
        return ((InternalServerExtensions) getServer()).aiex$entityRefContainer().get(uuid);
    }
}
