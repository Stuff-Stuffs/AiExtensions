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
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public class TestEntity extends AbstractNpcEntity {
    private static final TrackedData<Boolean> SLIM_DATA = DataTracker.registerData(TestEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final AiBrain<TestEntity> brain;

    protected TestEntity(final EntityType<? extends MobEntity> entityType, final World world) {
        super(entityType, world);
        final BrainNode<TestEntity, TaskTerminalBrainNode.Result<BasicTasks.Look.Result>, Entity> lookAtNode = new TaskTerminalBrainNode<>(BasicTasks.Look.ENTITY_DYNAMIC_KEY, (entity, context) -> new BasicTasks.Look.EntityParameters() {
            @Override
            public Entity entity() {
                return entity;
            }

            @Override
            public RaycastContext.ShapeType type() {
                return RaycastContext.ShapeType.VISUAL;
            }

            @Override
            public double lookSpeed() {
                return 0.2;
            }
        });
        final BrainNode<TestEntity, Optional<Entity>, Unit> target = TargetingBrainNodes.eventTarget(AiBrainEventTypes.DAMAGED, (context, arg, stream) -> stream.map(event -> event.sourceUuid().or(event::attackerUuid).map(uuid -> context.world().getEntity(uuid))).filter(Optional::isPresent).findFirst().flatMap(Function.identity()), AiBrainEvent.SECOND * 20, false, true);
        final BrainNode<TestEntity, Unit, Unit> root = target.ifThen((context, entity) -> entity.isPresent(), lookAtNode.discardResult().adaptArg(Optional::get), BrainNodes.empty());
        brain = AiBrain.create(this, root, BrainConfig.builder().build(), MemoryConfig.builder().build(this), TaskConfig.<TestEntity>builder().build(this));
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
