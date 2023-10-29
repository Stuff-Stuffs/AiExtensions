package io.github.stuff_stuffs.aiex.common.api.debug;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommands;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class PathDebugInfo {
    public final Identifier[] idToName;
    public final BlockPos[] positions;
    public final int[] nodeTypeIds;
    private final Entity entity;

    public PathDebugInfo(final Identifier[] name, final BlockPos[] positions, final int[] ids, final Entity entity) {
        idToName = name;
        this.positions = positions;
        nodeTypeIds = ids;
        for (final int id : ids) {
            if (id < 0 || id >= name.length) {
                throw new RuntimeException();
            }
        }
        if (positions.length != ids.length) {
            throw new RuntimeException();
        }
        this.entity = entity;
    }

    public boolean shouldSendTo(final ServerPlayerEntity player) {
        if (entity == null) {
            return false;
        }
        final Predicate<Entity> predicate = AiExCommands.PATH_WATCHING.get(player.getUuid());
        if (predicate != null) {
            return predicate.test(entity);
        }
        return false;
    }

    public void writeToBuffer(final PacketByteBuf buf) {
        buf.writeVarInt(idToName.length);
        for (final Identifier identifier : idToName) {
            buf.writeIdentifier(identifier);
        }
        final int length = positions.length;
        buf.writeVarInt(length);
        for (int i = 0; i < length; i++) {
            buf.writeBlockPos(positions[i]);
            buf.writeInt(nodeTypeIds[i]);
        }
    }

    public static PathDebugInfo read(final PacketByteBuf buf) {
        final int names = buf.readVarInt();
        final Identifier[] idToName = new Identifier[names];
        for (int i = 0; i < names; i++) {
            idToName[i] = buf.readIdentifier();
        }
        final int positionCount = buf.readVarInt();
        final BlockPos[] positions = new BlockPos[positionCount];
        final int[] nodeTypeIds = new int[positionCount];
        for (int i = 0; i < positionCount; i++) {
            positions[i] = buf.readBlockPos();
            nodeTypeIds[i] = buf.readInt();
        }
        return new PathDebugInfo(idToName, positions, nodeTypeIds, null);
    }
}
