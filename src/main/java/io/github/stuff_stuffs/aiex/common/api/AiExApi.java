package io.github.stuff_stuffs.aiex.common.api;

import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfigurator;
import io.github.stuff_stuffs.aiex.common.api.brain.task.basic.BasicEntityLookTask;
import io.github.stuff_stuffs.aiex.common.api.brain.task.basic.BasicLookTask;
import io.github.stuff_stuffs.aiex.common.api.brain.task.basic.BasicUseItemTask;
import io.github.stuff_stuffs.aiex.common.api.brain.task.basic.BasicWalkTask;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.EntityNavigator;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.ItemCooldownManager;

public final class AiExApi {
    public static final EntityApiLookup<EntityNavigator, Void> ENTITY_NAVIGATOR = EntityApiLookup.get(AiExCommon.id("basic_navigator"), EntityNavigator.class, Void.class);
    public static EntityApiLookup<ItemCooldownManager, Void> COOLDOWN_MANAGER = EntityApiLookup.get(AiExCommon.id("basic_cooldown"), ItemCooldownManager.class, Void.class);

    public static void init() {
        ENTITY_NAVIGATOR.registerFallback((entity, context) -> {
            if (entity instanceof MobEntity mob) {
                return (pos, error) -> {
                    final EntityNavigation navigation = mob.getNavigation();
                    final Path path = navigation.findPathTo(pos.x, pos.y, pos.z, (int) Math.floor(error));
                    if (path == null) {
                        return true;
                    }
                    navigation.startMovingAlong(path, 0.5);
                    return navigation.isIdle();
                };
            } else {
                return null;
            }
        });
        COOLDOWN_MANAGER.registerFallback((entity, context) -> {
            if (entity instanceof AbstractNpcEntity npc) {
                return npc.getCooldownManager();
            }
            return null;
        });
        new TaskConfigurator().add(Entity.class, (entity, builder, accessor) -> {
            final EntityNavigator navigator = AiExApi.ENTITY_NAVIGATOR.find(entity, null);
            if (navigator != null) {
                TaskConfig.Factory<Entity, BasicTasks.Walk.Result, BasicTasks.Walk.Parameters> basic = parameters -> new BasicWalkTask(parameters.target(), parameters.maxError());
                if (accessor.has(BasicTasks.Walk.KEY)) {
                    final TaskConfig.Factory<Entity, BasicTasks.Walk.Result, BasicTasks.Walk.Parameters> current = accessor.get(BasicTasks.Walk.KEY);
                    basic = current.fallbackTo(basic);
                }
                accessor.put(BasicTasks.Walk.KEY, basic);
            }
            if (builder.hasFactory(BasicTasks.Walk.KEY)) {
                TaskConfig.Factory<Entity, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters> basic = BasicWalkTask::dynamic;
                if (accessor.has(BasicTasks.Walk.DYNAMIC_KEY)) {
                    final TaskConfig.Factory<Entity, BasicTasks.Walk.Result, BasicTasks.Walk.DynamicParameters> current = accessor.get(BasicTasks.Walk.DYNAMIC_KEY);
                    basic = current.fallbackTo(basic);
                }
                accessor.put(BasicTasks.Walk.DYNAMIC_KEY, basic);
            }
            if (builder.hasFactory(BasicTasks.Look.KEY)) {
                TaskConfig.Factory<Entity, BasicTasks.Look.Result, BasicTasks.Look.EntityParameters> basic = parameters -> new BasicEntityLookTask<>(parameters.entity(), parameters.type(), parameters.lookSpeed());
                if (builder.hasFactory(BasicTasks.Look.ENTITY_KEY)) {
                    final TaskConfig.Factory<Entity, BasicTasks.Look.Result, BasicTasks.Look.EntityParameters> current = accessor.get(BasicTasks.Look.ENTITY_KEY);
                    basic = current.fallbackTo(basic);
                }
                accessor.put(BasicTasks.Look.ENTITY_KEY, basic);
            }
            if (builder.hasFactory(BasicTasks.Look.ENTITY_KEY)) {
                TaskConfig.Factory<Entity, BasicTasks.Look.Result, BasicTasks.Look.EntityParameters> basic = BasicEntityLookTask::dynamic;
                if (accessor.has(BasicTasks.Look.ENTITY_DYNAMIC_KEY)) {
                    final TaskConfig.Factory<Entity, BasicTasks.Look.Result, BasicTasks.Look.EntityParameters> current = accessor.get(BasicTasks.Look.ENTITY_DYNAMIC_KEY);
                    basic = current.fallbackTo(basic);
                }
                accessor.put(BasicTasks.Look.ENTITY_DYNAMIC_KEY, basic);
            }
        }).add(LivingEntity.class, (entity, builder, accessor) -> {
            TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters> basic = parameters -> new BasicLookTask(parameters.lookDir(), parameters.lookSpeed());
            if (accessor.has(BasicTasks.Look.KEY)) {
                final TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters> current = accessor.get(BasicTasks.Look.KEY);
                basic = current.fallbackTo(basic);
            }
            accessor.put(BasicTasks.Look.KEY, basic);
        }).add(LivingEntity.class, (entity, builder, accessor) -> {
            TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters> basic = BasicLookTask::dynamic;
            if (builder.hasFactory(BasicTasks.Look.DYNAMIC_KEY)) {
                final TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters> current = accessor.get(BasicTasks.Look.DYNAMIC_KEY);
                basic = current.fallbackTo(basic);
            }
            accessor.put(BasicTasks.Look.DYNAMIC_KEY, basic);
        }).add(LivingEntity.class, (entity, builder, accessor) -> {
            TaskConfig.Factory<LivingEntity, BasicTasks.UseItem.Result, BasicTasks.UseItem.Parameters> basic = BasicUseItemTask::new;
            if (builder.hasFactory(BasicTasks.UseItem.KEY)) {
                final TaskConfig.Factory<LivingEntity, BasicTasks.UseItem.Result, BasicTasks.UseItem.Parameters> current = accessor.get(BasicTasks.UseItem.KEY);
                basic = current.fallbackTo(basic);
            }
            accessor.put(BasicTasks.UseItem.KEY, basic);
        }).build();
    }

    private AiExApi() {
    }
}
