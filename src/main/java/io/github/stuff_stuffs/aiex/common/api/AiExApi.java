package io.github.stuff_stuffs.aiex.common.api;

import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfigurator;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.inventory.NpcInventory;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.EntityPather;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.InternalServerExtensions;
import io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls.*;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;

public final class AiExApi {
    public static final EntityApiLookup<EntityPather, Void> ENTITY_NAVIGATOR = EntityApiLookup.get(AiExCommon.id("basic_navigator"), EntityPather.class, Void.class);
    public static final EntityApiLookup<ItemCooldownManager, Void> COOLDOWN_MANAGER = EntityApiLookup.get(AiExCommon.id("basic_cooldown"), ItemCooldownManager.class, Void.class);
    public static final EntityApiLookup<NpcInventory, Void> NPC_INVENTORY = EntityApiLookup.get(AiExCommon.id("npc_inventory"), NpcInventory.class, Void.class);

    public static final TagKey<EntityType<?>> PROJECTILE_ENTITY_TAG = TagKey.of(RegistryKeys.ENTITY_TYPE, AiExCommon.id("projectile"));
    public static final TagKey<Block> MINEABLE_ORE_TAG = TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_ore_mineable"));

    public static void submitTask(final Job runnable, final ServerWorld world) {
        ((InternalServerExtensions) world.getServer()).aiex$submitTask(runnable);
    }

    public static void init() {
        COOLDOWN_MANAGER.registerFallback((entity, context) -> {
            if (entity instanceof AbstractNpcEntity npc) {
                return npc.getItemCooldownManager();
            }
            return null;
        });
        NPC_INVENTORY.registerFallback((entity, context) -> {
            if (entity instanceof AbstractNpcEntity npc) {
                return npc.getNpcInventory();
            }
            return null;
        });
        TaskConfigurator.add(Entity.class, (entity, builder, accessor) -> {
            if (builder.hasFactory(BasicTasks.Look.KEY)) {
                TaskConfig.Factory<Entity, BasicTasks.Look.Result, BasicTasks.Look.EntityParameters, BrainResourceRepository> basic = parameters -> new DefaultEntityLookTask<>(parameters.entity(), parameters.type(), parameters.lookSpeed());
                if (builder.hasFactory(BasicTasks.Look.ENTITY_KEY)) {
                    final TaskConfig.Factory<Entity, BasicTasks.Look.Result, BasicTasks.Look.EntityParameters, BrainResourceRepository> current = accessor.get(BasicTasks.Look.ENTITY_KEY);
                    basic = current.fallbackTo(basic);
                }
                accessor.put(BasicTasks.Look.ENTITY_KEY, basic);
            }
            if (builder.hasFactory(BasicTasks.Look.ENTITY_KEY)) {
                TaskConfig.Factory<Entity, BasicTasks.Look.Result, BasicTasks.Look.EntityParameters, BrainResourceRepository> basic = DefaultEntityLookTask::dynamic;
                if (accessor.has(BasicTasks.Look.ENTITY_DYNAMIC_KEY)) {
                    final TaskConfig.Factory<Entity, BasicTasks.Look.Result, BasicTasks.Look.EntityParameters, BrainResourceRepository> current = accessor.get(BasicTasks.Look.ENTITY_DYNAMIC_KEY);
                    basic = current.fallbackTo(basic);
                }
                accessor.put(BasicTasks.Look.ENTITY_DYNAMIC_KEY, basic);
            }
        });
        TaskConfigurator.add(LivingEntity.class, (entity, builder, accessor) -> {
            final EntityPather navigator = AiExApi.ENTITY_NAVIGATOR.find(entity, null);
            if (navigator != null) {
                TaskConfig.Factory<LivingEntity, BasicTasks.Walk.Result, BasicTasks.Walk.Parameters, BrainResourceRepository> basic = parameters -> new DefaultWalkTask<>(parameters.target(), parameters.maxError(), parameters.urgency(), parameters.maxPathLength(), parameters.partial());
                if (accessor.has(BasicTasks.Walk.KEY)) {
                    final TaskConfig.Factory<LivingEntity, BasicTasks.Walk.Result, BasicTasks.Walk.Parameters, BrainResourceRepository> current = accessor.get(BasicTasks.Walk.KEY);
                    basic = current.fallbackTo(basic);
                }
                accessor.put(BasicTasks.Walk.KEY, basic);
            }
        });
        TaskConfigurator.add(LivingEntity.class, (entity, builder, accessor) -> {
            TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters, BrainResourceRepository> basic = parameters -> new DefaultLookTask<>(parameters.lookDir(), parameters.lookSpeed());
            if (accessor.has(BasicTasks.Look.KEY)) {
                final TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters, BrainResourceRepository> current = accessor.get(BasicTasks.Look.KEY);
                basic = current.fallbackTo(basic);
            }
            accessor.put(BasicTasks.Look.KEY, basic);
        });
        TaskConfigurator.add(LivingEntity.class, (entity, builder, accessor) -> {
            TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters, BrainResourceRepository> basic = DefaultLookTask::dynamic;
            if (builder.hasFactory(BasicTasks.Look.DYNAMIC_KEY)) {
                final TaskConfig.Factory<LivingEntity, BasicTasks.Look.Result, BasicTasks.Look.Parameters, BrainResourceRepository> current = accessor.get(BasicTasks.Look.DYNAMIC_KEY);
                basic = current.fallbackTo(basic);
            }
            accessor.put(BasicTasks.Look.DYNAMIC_KEY, basic);
        });
        TaskConfigurator.add(LivingEntity.class, (entity, builder, accessor) -> {
            TaskConfig.Factory<LivingEntity, BasicTasks.UseItem.Result, BasicTasks.UseItem.Parameters, BrainResourceRepository> basic = DefaultUseItemTask::new;
            if (builder.hasFactory(BasicTasks.UseItem.KEY)) {
                final TaskConfig.Factory<LivingEntity, BasicTasks.UseItem.Result, BasicTasks.UseItem.Parameters, BrainResourceRepository> current = accessor.get(BasicTasks.UseItem.KEY);
                basic = current.fallbackTo(basic);
            }
            accessor.put(BasicTasks.UseItem.KEY, basic);
        });
        TaskConfigurator.add(Entity.class, (entity, builder, accessor) -> {
            if (NPC_INVENTORY.find(entity, null) != null) {
                {
                    TaskConfig.Factory<Entity, BasicTasks.SwapStack.Result, BasicTasks.SwapStack.Parameters, BrainResourceRepository> basic = DefaultSwapStackTask::new;
                    if (accessor.has(BasicTasks.SwapStack.KEY)) {
                        final TaskConfig.Factory<Entity, BasicTasks.SwapStack.Result, BasicTasks.SwapStack.Parameters, BrainResourceRepository> current = accessor.get(BasicTasks.SwapStack.KEY);
                        basic = current.fallbackTo(basic);
                    }
                    accessor.put(BasicTasks.SwapStack.KEY, basic);
                }
                {
                    TaskConfig.Factory<Entity, BasicTasks.SelectTool.Result, BasicTasks.SelectTool.Parameters, BrainResourceRepository> basic = parameters -> new DefaultSelectToolTask<>(parameters.state(), 3.0);
                    if (accessor.has(BasicTasks.SelectTool.KEY)) {
                        final TaskConfig.Factory<Entity, BasicTasks.SelectTool.Result, BasicTasks.SelectTool.Parameters, BrainResourceRepository> current = accessor.get(BasicTasks.SelectTool.KEY);
                        basic = current.fallbackTo(basic);
                    }
                    accessor.put(BasicTasks.SelectTool.KEY, basic);
                }
                {
                    TaskConfig.Factory<Entity, BasicTasks.MoveItemsToContainer.Result, BasicTasks.MoveItemsToContainer.Parameters, BrainResourceRepository> basic = parameters -> new DefaultMoveItemsToContainerTask<>(parameters.container(), parameters.filter(), parameters.side(), parameters.speed());
                    if (accessor.has(BasicTasks.MoveItemsToContainer.KEY)) {
                        final TaskConfig.Factory<Entity, BasicTasks.MoveItemsToContainer.Result, BasicTasks.MoveItemsToContainer.Parameters, BrainResourceRepository> current = accessor.get(BasicTasks.MoveItemsToContainer.KEY);
                        basic = current.fallbackTo(basic);
                    }
                    accessor.put(BasicTasks.MoveItemsToContainer.KEY, basic);
                }
            }
        });
        TaskConfigurator.add(LivingEntity.class, (entity, builder, accessor) -> {
            TaskConfig.Factory<LivingEntity, BasicTasks.PlaceBlock.Result, BasicTasks.PlaceBlock.Parameters, BrainResourceRepository> basic = DefaultPlaceBlockTask::new;
            if (builder.hasFactory(BasicTasks.UseItem.KEY)) {
                final TaskConfig.Factory<LivingEntity, BasicTasks.PlaceBlock.Result, BasicTasks.PlaceBlock.Parameters, BrainResourceRepository> current = accessor.get(BasicTasks.PlaceBlock.KEY);
                basic = current.fallbackTo(basic);
            }
            accessor.put(BasicTasks.PlaceBlock.KEY, basic);
        });
        TaskConfigurator.add(MobEntity.class, (entity, builder, accessor) -> {
            TaskConfig.Factory<MobEntity, BasicTasks.MineBlock.Result, BasicTasks.MineBlock.Parameters, BrainResourceRepository> basic = parameters -> new DefaultMineBlockTask<>(parameters.pos());
            if (builder.hasFactory(BasicTasks.UseItem.KEY)) {
                final TaskConfig.Factory<MobEntity, BasicTasks.MineBlock.Result, BasicTasks.MineBlock.Parameters, BrainResourceRepository> current = accessor.get(BasicTasks.MineBlock.KEY);
                basic = current.fallbackTo(basic);
            }
            accessor.put(BasicTasks.MineBlock.KEY, basic);
        });
    }

    public interface Job {
        void run();

        void preRun();
    }

    private AiExApi() {
    }
}
