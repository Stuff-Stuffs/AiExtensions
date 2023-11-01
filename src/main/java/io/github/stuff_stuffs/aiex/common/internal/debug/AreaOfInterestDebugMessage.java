package io.github.stuff_stuffs.aiex.common.internal.debug;

import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestBounds;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestEntry;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.api.debug.DebugFlag;
import io.github.stuff_stuffs.aiex.common.impl.aoi.AreaOfInterestReferenceImpl;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommands;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public sealed interface AreaOfInterestDebugMessage {
    DebugFlag<AreaOfInterestDebugMessage> FLAG = new DebugFlag<>() {
        @Override
        public boolean shouldSendTo(final ServerPlayerEntity entity, final AreaOfInterestDebugMessage val) {
            final AreaOfInterestType<?> type = AiExCommands.AOI_WATCHING.get(entity.getUuid());
            if (val instanceof Clear clear) {
                return true;
            }
            if (val instanceof Remove remove && remove.type == type) {
                return true;
            }
            if (val instanceof Add<?> add && add.type == type) {
                return true;
            }
            return false;
        }

        @Override
        public void writeToBuffer(final AreaOfInterestDebugMessage val, final PacketByteBuf buf, final ServerPlayerEntity player) {
            if (val instanceof Add<?> add) {
                buf.writeInt(0);
                add.writeToBuffer(buf);
            } else if (val instanceof Remove remove) {
                buf.writeInt(1);
                buf.writeRegistryValue(AreaOfInterestType.REGISTRY, remove.type);
                buf.writeLong(remove.id);
            } else if (val instanceof Clear clear) {
                buf.writeInt(2);
                buf.writeRegistryValue(AreaOfInterestType.REGISTRY, clear.type);
            }
        }

        @Override
        public void readAndApply(final PacketByteBuf buf) {
            final int messageType = buf.readInt();
            if (messageType == 0) {
                final AreaOfInterestType<?> type = buf.readRegistryValue(AreaOfInterestType.REGISTRY);
                AiExCommands.CLIENT_AOI_DEBUG_APPLICATOR.accept(read(buf, type));
            } else if (messageType == 1) {
                final AreaOfInterestType<?> type = buf.readRegistryValue(AreaOfInterestType.REGISTRY);
                final long id = buf.readLong();
                AiExCommands.CLIENT_AOI_DEBUG_APPLICATOR.accept(new Remove(type, id));
            } else if (messageType == 2) {
                final AreaOfInterestType<?> type = buf.readRegistryValue(AreaOfInterestType.REGISTRY);
                AiExCommands.CLIENT_AOI_DEBUG_APPLICATOR.accept(new Clear(type));
            }
        }

        private static <T extends AreaOfInterest> Add<T> read(final PacketByteBuf buf, final AreaOfInterestType<T> type) {
            final T value = buf.decodeAsJson(type.codec());
            final AreaOfInterestBounds bounds = buf.decodeAsJson(AreaOfInterestBounds.CODEC);
            final long id = buf.readLong();
            return new Add<>(type, value, bounds, id);
        }
    };

    record Add<T extends AreaOfInterest>(AreaOfInterestType<T> type, T value, AreaOfInterestBounds bounds,
                                         long id) implements AreaOfInterestDebugMessage {
        public void writeToBuffer(final PacketByteBuf buf) {
            buf.writeRegistryValue(AreaOfInterestType.REGISTRY, type);
            buf.encodeAsJson(type.codec(), value);
            buf.encodeAsJson(AreaOfInterestBounds.CODEC, bounds);
            buf.writeLong(id);
        }

        public static <T extends AreaOfInterest> Add<T> from(final AreaOfInterestEntry<T> entry) {
            //noinspection unchecked
            return new Add<>((AreaOfInterestType<T>) entry.value().type(), entry.value(), entry.bounds(), ((AreaOfInterestReferenceImpl<T>) entry.reference()).id());
        }
    }

    record Remove(AreaOfInterestType<?> type, long id) implements AreaOfInterestDebugMessage {
    }

    record Clear(AreaOfInterestType<?> type) implements AreaOfInterestDebugMessage {
    }
}
