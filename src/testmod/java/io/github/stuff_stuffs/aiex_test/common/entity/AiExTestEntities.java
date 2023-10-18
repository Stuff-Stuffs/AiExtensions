package io.github.stuff_stuffs.aiex_test.common.entity;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.entity.EntityPather;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.function.BiFunction;

public final class AiExTestEntities {
    public static final EntityType<TestEntity> TEST_ENTITY = FabricEntityTypeBuilder.createLiving().spawnGroup(SpawnGroup.MISC).entityFactory(TestEntity::new).dimensions(EntityDimensions.fixed(0.6F, 1.8F)).defaultAttributes(MobEntity::createMobAttributes).build();

    public static void init() {
        Registry.register(Registries.ENTITY_TYPE, AiExCommon.id("test"), TEST_ENTITY);
        AiExApi.ENTITY_NAVIGATOR.registerForType((entity, unused) -> entity.getNavigator(), TEST_ENTITY);
    }

    public record BasicEntityNode(
            int x,
            int y,
            int z,
            BasicEntityNode previous,
            double cost,
            boolean onGround
    ) implements EntityPather.EntityNode<BasicEntityNode> {
    }

    private AiExTestEntities() {
    }
}
