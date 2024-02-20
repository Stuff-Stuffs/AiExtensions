package io.github.stuff_stuffs.aiex.common.api.debug;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public interface DebugFlag<T> {
    boolean shouldSendTo(ServerPlayerEntity entity, T val);

    void writeToBuffer(T val, RegistryByteBuf buf);

    T readFromBuffer(RegistryByteBuf buf);

    void apply(T message);
}
