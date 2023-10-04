package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferencable;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReference;
import io.github.stuff_stuffs.aiex.common.internal.InternalServerExtensions;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class MixinEntity implements EntityReferencable {
    @Shadow
    public abstract World getEntityWorld();

    @Shadow
    public abstract UUID getUuid();

    @Override
    public EntityReference aiex$getAndUpdateReference() {
        final MinecraftServer server = getEntityWorld().getServer();
        if (server == null) {
            throw new RuntimeException("Must call on server thread!");
        }
        return ((InternalServerExtensions) server).aiex$entityRefContainer().get(getUuid(), (Entity) (Object) this);
    }
}
