package io.github.stuff_stuffs.aiex.client.internal;

import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.debug.AiExDebugFlags;
import io.github.stuff_stuffs.aiex.common.api.debug.DebugFlag;
import io.github.stuff_stuffs.aiex.common.api.debug.PathDebugInfo;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommands;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

public class AiExClient implements ClientModInitializer {
    private static final Object2LongMap<PathDebugInfo> TIME_OUTS = new Object2LongOpenHashMap<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AiExDebugFlags.CHANNEL, (client, handler, buf, responseSender) -> {
            final Identifier id = buf.readIdentifier();
            final DebugFlag<?> flag = AiExDebugFlags.REGISTRY.get(id);
            if (flag == null) {
                return;
            }
            final PacketByteBuf copy = PacketByteBufs.copy(buf);
            client.execute(() -> flag.readAndApply(copy));
        });
        AiExCommands.CLIENT_PATH_DEBUG_APPLICATOR = info -> {
            final MinecraftClient client = MinecraftClient.getInstance();
            //TODO actually code invalidation
            TIME_OUTS.clear();
            TIME_OUTS.put(info, client.world.getTime() + AiBrainEvent.MINUTE * 3);
        };
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) {
                TIME_OUTS.clear();
                return;
            }
            TIME_OUTS.object2LongEntrySet().removeIf(entry -> entry.getLongValue() < client.world.getTime());
            if (client.world.getTime() % 15 != 0) {
                return;
            }
            for (final PathDebugInfo info : TIME_OUTS.keySet()) {
                final ParticleEffect[] effects = new ParticleEffect[info.idToName.length];
                int idx = 0;
                for (final Identifier identifier : info.idToName) {
                    final int hash = HashCommon.murmurHash3(identifier.hashCode() + idx);
                    final int r = hash & 255;
                    final int g = (hash >>> 8) & 255;
                    final int b = (hash >>> 16) & 255;
                    effects[idx++] = new DustParticleEffect(new Vector3f(r / 255.0F, g / 255.0F, b / 255.0F), 0.5F);
                }
                final int size = info.positions.length;
                for (int i = 0; i < size; i++) {
                    final BlockPos pos = info.positions[i];
                    final ParticleEffect effect = effects[info.nodeTypeIds[i]];
                    client.world.addParticle(effect, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
                }
            }
        });
    }
}