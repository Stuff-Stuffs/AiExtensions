package io.github.stuff_stuffs.aiex.common.api.debug;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommands;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.debug.PathDebugInfo;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
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
        public void writeToBuffer(final PathDebugInfo val, final RegistryByteBuf buf) {
            val.writeToBuffer(buf);
        }

        @Override
        public PathDebugInfo readFromBuffer(final RegistryByteBuf buf) {
            return PathDebugInfo.read(buf);
        }

        @Override
        public void apply(final PathDebugInfo message) {
            AiExCommands.CLIENT_PATH_DEBUG_APPLICATOR.accept(message);
        }
    };
    public static final CustomPayload.Id<DebugPayload<?>> DEBUG_PAYLOAD_ID = new CustomPayload.Id<>(AiExCommon.id("debug_payload"));

    public static <T> void send(final DebugFlag<T> flag, final T val, final ServerWorld world) {
        for (final ServerPlayerEntity player : world.getPlayers()) {
            if (flag.shouldSendTo(player, val)) {
                ServerPlayNetworking.send(player, new DebugPayload<>(flag, val));
            }
        }
    }

    public static void init() {
        Registry.register(REGISTRY, AiExCommon.id("path"), PATH_FLAG);
        PayloadTypeRegistry.playS2C().register(DEBUG_PAYLOAD_ID, new PacketCodec<>() {
            @Override
            public DebugPayload<?> decode(final RegistryByteBuf buf) {
                final DebugFlag<?> flag = REGISTRY.get(buf.readIdentifier());
                return decode0(buf, flag);
            }

            private <T> DebugPayload<T> decode0(final RegistryByteBuf buf, final DebugFlag<T> flag) {
                return new DebugPayload<>(flag, flag.readFromBuffer(buf));
            }

            @Override
            public void encode(final RegistryByteBuf buf, final DebugPayload<?> value) {
                buf.writeIdentifier(REGISTRY.getId(value.flag));
                encode0(buf, value);
            }

            private <T> void encode0(final RegistryByteBuf buf, final DebugPayload<T> payload) {
                payload.flag.writeToBuffer(payload.value, buf);
            }
        });
    }

    public record DebugPayload<T>(DebugFlag<T> flag, T value) implements CustomPayload {
        @Override
        public Id<? extends CustomPayload> getId() {
            return DEBUG_PAYLOAD_ID;
        }
    }


    private AiExDebugFlags() {
    }
}
