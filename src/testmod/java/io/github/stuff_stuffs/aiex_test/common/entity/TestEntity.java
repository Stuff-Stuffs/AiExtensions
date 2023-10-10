package io.github.stuff_stuffs.aiex_test.common.entity;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrain;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventTypes;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.basic.target.TargetingBrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskTerminalBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class TestEntity extends AbstractNpcEntity {
    private static final TrackedData<Boolean> SLIM_DATA = DataTracker.registerData(TestEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final AiBrain<TestEntity> brain;

    protected TestEntity(final EntityType<? extends MobEntity> entityType, final World world) {
        super(entityType, world);
        final BrainNode<TestEntity, TaskTerminalBrainNode.Result<BasicTasks.Walk.Result>, Entity> followNode = new TaskTerminalBrainNode<>(BasicTasks.Walk.DYNAMIC_KEY, (entity, context) -> new BasicTasks.Walk.DynamicParameters() {
            @Override
            public boolean shouldStop() {
                return !entity.isAlive();
            }

            @Override
            public Vec3d target() {
                return entity.getPos();
            }

            @Override
            public double maxError() {
                return 2.5;
            }
        });
        final BrainNode<TestEntity, TaskTerminalBrainNode.Result<BasicTasks.Look.Result>, Entity> lookAtNode = new TaskTerminalBrainNode<>(BasicTasks.Look.DYNAMIC_KEY, (entity, context) -> new BasicTasks.Look.Parameters() {
            @Override
            public Vec3d lookDir() {
                final TestEntity npc = context.entity();
                return entity.getPos().add(0, entity.getEyeHeight(entity.getPose()), 0).subtract(npc.getPos().add(0, npc.getEyeHeight(npc.getPose()), 0));
            }

            @Override
            public double lookSpeed() {
                return 0.05;
            }
        });
        final BrainNode<TestEntity, Optional<Entity>, Unit> target = TargetingBrainNodes.eventTarget(AiBrainEventTypes.DAMAGED, (context, arg, stream) -> stream.map(event -> event.sourceUuid().or(event::attackerUuid).map(uuid -> context.world().getEntity(uuid))).filter(Optional::isPresent).findFirst().flatMap(Function.identity()), AiBrainEvent.SECOND * 10, false, true);
        final BrainNode<TestEntity, Unit, Unit> root = target.ifThen((context, entity) -> entity.isPresent(), followNode.parallel(lookAtNode, (r0, r1) -> Unit.INSTANCE).adaptArg(Optional::get), BrainNodes.empty());
        brain = AiBrain.create(root, BrainConfig.builder().build(), MemoryConfig.builder().build(this), TaskConfig.<TestEntity>builder().build(this));
        updateSlim();
    }

    @Override
    protected void deserializeBrain(final NbtCompound brainNbt) {
        brain.readNbt(brainNbt);
    }

    @Override
    public void setUuid(final UUID uuid) {
        super.setUuid(uuid);
        updateSlim();
    }

    private void updateSlim() {
        slim((getUuid().getLeastSignificantBits() & 1) == 1);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(SLIM_DATA, false);
    }

    public void slim(final boolean slim) {
        dataTracker.set(SLIM_DATA, slim);
    }

    public boolean slim() {
        return dataTracker.get(SLIM_DATA);
    }

    @Override
    public AiBrain<? extends TestEntity> aiex$getBrain() {
        return brain;
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }
}
