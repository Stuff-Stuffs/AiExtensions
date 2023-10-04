package io.github.stuff_stuffs.aiex_test.common.entity;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrain;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskTerminalBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex_test.common.basic.BasicBrainNodes;
import io.github.stuff_stuffs.aiex_test.common.basic.TaskKeys;
import io.github.stuff_stuffs.aiex_test.common.basic.WalkTask;
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

public class TestEntity extends AbstractNpcEntity {
    private static final TrackedData<Boolean> SLIM_DATA = DataTracker.registerData(TestEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final AiBrain<TestEntity> brain;

    protected TestEntity(final EntityType<? extends MobEntity> entityType, final World world) {
        super(entityType, world);
        final BrainNode<TestEntity, Optional<Vec3d>, Unit> nearestPlayer = BasicBrainNodes.nearestPlayer();
        final BrainNode<TestEntity, TaskTerminalBrainNode.Result<WalkTask.Result>, Vec3d> resettingWalk = BasicBrainNodes.<TestEntity>walk(TaskKeys.WALK_TASK_KEY, 4.0).resetOnResult(TaskTerminalBrainNode.<WalkTask.Result>successInnerPredicate(r -> r == WalkTask.Result.CONTINUE).negate());
        brain = AiBrain.create(nearestPlayer.ifThen(Optional::isPresent, resettingWalk.adaptArg(Optional::get), BrainNodes.constant(new TaskTerminalBrainNode.Failure<>())).discardResult(), BrainConfig.builder().build(), MemoryConfig.builder().build(this), TaskConfig.builder().build(this));
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
