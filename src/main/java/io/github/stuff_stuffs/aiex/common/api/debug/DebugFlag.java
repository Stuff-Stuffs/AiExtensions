package io.github.stuff_stuffs.aiex.common.api.debug;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public interface DebugFlag<T> {
    boolean shouldSendTo(ServerPlayerEntity entity, T val);

    void writeToBuffer(T val, PacketByteBuf buf, ServerPlayerEntity player);

    void readAndApply(PacketByteBuf buf);
}
