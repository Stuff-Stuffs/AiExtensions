package io.github.stuff_stuffs.aiex_test.client;

import io.github.stuff_stuffs.aiex_test.client.render.entity.TestEntityRenderer;
import io.github.stuff_stuffs.aiex_test.common.entity.AiExTestEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class AiExTestClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(AiExTestEntities.TEST_ENTITY, TestEntityRenderer::createBasic);
    }
}
