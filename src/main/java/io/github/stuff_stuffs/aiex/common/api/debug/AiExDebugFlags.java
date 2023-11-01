package io.github.stuff_stuffs.aiex.common.api.debug;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommands;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.debug.PathDebugInfo;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class AiExDebugFlags {
    public static final Identifier CHANNEL = AiExCommon.id("debug");
    public static final RegistryKey<Registry<DebugFlag<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("debug_flags"));
    public static final Registry<DebugFlag<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();
    public static final DebugFlag<PathDebugInfo> PATH_FLAG = new DebugFlag<>() {
        @Override
        public boolean shouldSendTo(final ServerPlayerEntity entity, final PathDebugInfo val) {
            return val.shouldSendTo(entity);
        }

        @Override
        public void writeToBuffer(final PathDebugInfo val, final PacketByteBuf buf, final ServerPlayerEntity player) {
            val.writeToBuffer(buf);
        }

        @Override
        public void readAndApply(final PacketByteBuf buf) {
            final PathDebugInfo info = PathDebugInfo.read(buf);
            AiExCommands.CLIENT_PATH_DEBUG_APPLICATOR.accept(info);
        }
    };

    public static <T> void send(final DebugFlag<T> flag, final T val, final ServerWorld world) {
        final Identifier id = REGISTRY.getId(flag);
        for (final ServerPlayerEntity player : world.getPlayers()) {
            if (flag.shouldSendTo(player, val)) {
                final PacketByteBuf buf = PacketByteBufs.create();
                buf.writeIdentifier(id);
                flag.writeToBuffer(val, buf, player);
                ServerPlayNetworking.send(player, CHANNEL, buf);
            }
        }
    }

    public static void init() {
        Registry.register(REGISTRY, AiExCommon.id("path"), PATH_FLAG);
    }


    private AiExDebugFlags() {
    }
}
