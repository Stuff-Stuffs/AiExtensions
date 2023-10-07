package io.github.stuff_stuffs.aiex_test.common.basic;

import io.github.stuff_stuffs.aiex.common.api.entity.EntityNavigator;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;

public final class AiExApi {
    public static final EntityApiLookup<EntityNavigator, Void> ENTITY_NAVIGATOR = EntityApiLookup.get(AiExCommon.id("basic_navigator"), EntityNavigator.class, Void.class);

    private AiExApi() {
    }
}
