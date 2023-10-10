package io.github.stuff_stuffs.aiex_test.common;

import io.github.stuff_stuffs.aiex_test.common.entity.AiExTestEntities;
import net.fabricmc.api.ModInitializer;

public class AiExTestCommon implements ModInitializer {
    @Override
    public void onInitialize() {
        AiExTestEntities.init();
    }
}
