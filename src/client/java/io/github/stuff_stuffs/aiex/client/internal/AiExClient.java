package io.github.stuff_stuffs.aiex.client.internal;

import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestBounds;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.debug.AiExDebugFlags;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommands;
import io.github.stuff_stuffs.aiex.common.internal.debug.AreaOfInterestDebugMessage;
import io.github.stuff_stuffs.aiex.common.internal.debug.PathDebugInfo;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

public class AiExClient implements ClientModInitializer {
    private static final Int2ObjectMap<Entry> TIME_OUTS = new Int2ObjectLinkedOpenHashMap<>();
    private static final Long2ObjectMap<AoiEntry> AOI_CACHE = new Long2ObjectOpenHashMap<>();

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AiExDebugFlags.DEBUG_PAYLOAD_ID, new ClientPlayNetworking.PlayPayloadHandler<>() {
            @Override
            public void receive(final AiExDebugFlags.DebugPayload<?> payload, final ClientPlayNetworking.Context context) {
                receive0(payload);
            }

            private <T> void receive0(final AiExDebugFlags.DebugPayload<T> payload) {
                payload.flag().apply(payload.value());
            }
        });
        AiExCommands.CLIENT_PATH_DEBUG_APPLICATOR = info -> {
            final MinecraftClient client = MinecraftClient.getInstance();
            TIME_OUTS.put(info.entityId, new Entry(client.world.getTime() + AiBrainEvent.MINUTE * 3, info));
        };
        AiExCommands.CLIENT_AOI_DEBUG_APPLICATOR = message -> {
            if (message instanceof final AreaOfInterestDebugMessage.Add<?> add) {
                AOI_CACHE.put(add.id(), new AoiEntry(add.type(), add.value(), add.bounds()));
            } else if (message instanceof final AreaOfInterestDebugMessage.Remove remove) {
                AOI_CACHE.remove(remove.id());
            } else if (message instanceof final AreaOfInterestDebugMessage.Clear clear) {
                AOI_CACHE.values().removeIf(entry -> entry.type() == clear.type());
            }
        };
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            final MatrixStack stack = context.matrixStack();
            stack.push();
            final Camera camera = context.camera();
            stack.translate(-camera.getPos().x, -camera.getPos().y, -camera.getPos().z);
            final VertexConsumer buffer = context.consumers().getBuffer(RenderLayer.getLines());
            for (final AoiEntry value : AOI_CACHE.values()) {
                WorldRenderer.drawBox(stack, buffer, value.bounds.box(), 1, 1, 1, 1);
            }
            stack.pop();
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) {
                TIME_OUTS.clear();
                AOI_CACHE.clear();
                return;
            }
            TIME_OUTS.values().removeIf(entry -> entry.timeout < client.world.getTime());
            if (client.world.getTime() % 10 != 0) {
                return;
            }
            for (final Entry entry : TIME_OUTS.values()) {
                final PathDebugInfo info = entry.pathDebugInfo;
                final ParticleEffect[] effects = new ParticleEffect[info.idToName.length];
                int idx = 0;
                for (final Identifier identifier : info.idToName) {
                    final int hash = HashCommon.murmurHash3(identifier.hashCode() + idx + entry.pathDebugInfo.entityId);
                    final int r = hash & 255;
                    final int g = (hash >>> 8) & 255;
                    final int b = (hash >>> 16) & 255;
                    effects[idx++] = new DustParticleEffect(new Vector3f(r / 255.0F, g / 255.0F, b / 255.0F), 0.75F);
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

    private record AoiEntry(AreaOfInterestType<?> type, AreaOfInterest value, AreaOfInterestBounds bounds) {
    }

    private record Entry(long timeout, PathDebugInfo pathDebugInfo) {
    }
}