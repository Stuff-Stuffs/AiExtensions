package io.github.stuff_stuffs.aiex_test.common.entity;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrain;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicNpcEntityPather;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicPathingUniverse;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.PathingNpcEntity;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex_test.common.basic.BasicBrainNodes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class TestEntity extends AbstractNpcEntity implements PathingNpcEntity {
    private static final TrackedData<Boolean> SLIM_DATA = DataTracker.registerData(TestEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final AiBrain brain;
    private final BasicNpcEntityPather navigator;

    protected TestEntity(final EntityType<? extends MobEntity> entityType, final World world) {
        super(entityType, world);
        navigator = new BasicNpcEntityPather(this);
        final BrainNode<TestEntity, BasicTasks.Walk.Result, Vec3d> walk = BrainNodes.expectResult(new TaskBrainNode<>(BasicTasks.Walk.KEY, (BiFunction<Vec3d, BrainContext<TestEntity>, BasicTasks.Walk.Parameters>) (vec3d, context) -> new BasicTasks.Walk.Parameters() {
            @Override
            public Vec3d target() {
                return vec3d;
            }

            @Override
            public double maxError() {
                return 1.0;
            }
        }, (vec3d, context) -> BrainResourceRepository.buildEmpty(context.brain().resources())), RuntimeException::new).resetOnContext(new BiPredicate<>() {
            private Vec3d last = new Vec3d(0, -1000000, 0);

            @Override
            public boolean test(final BrainContext<TestEntity> context, final Vec3d vec3d) {
                if (vec3d.squaredDistanceTo(last) > 1) {
                    last = vec3d;
                    return true;
                }
                return false;
            }
        });
        final BrainNode<TestEntity, Unit, Unit> root = BasicBrainNodes.<TestEntity>nearestPlayer().ifThen((context, d) -> d.isPresent(), walk.discardResult().adaptArg(Optional::get), BrainNodes.empty());
        brain = AiBrain.create(this, root, BrainConfig.builder().build(), MemoryConfig.builder().build(this), TaskConfig.<TestEntity>builder().build(this), AiExCommon.createForEntity(this));
        updateSlim();
    }

    public BasicNpcEntityPather getNavigator() {
        return navigator;
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

    @Override
    public void tick() {
        super.tick();
    }

    public void slim(final boolean slim) {
        dataTracker.set(SLIM_DATA, slim);
    }

    public boolean slim() {
        return dataTracker.get(SLIM_DATA);
    }

    @Override
    public AiBrain aiex$getBrain() {
        return brain;
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public int ensuredPathingRadius() {
        return 3;
    }

    @Override
    public int pathingCachePollRate() {
        return 40;
    }

    @Override
    public Collection<LocationClassifier<?>> ensuredLocationClassifiers() {
        return Collections.singleton(BasicPathingUniverse.CLASSIFIER);
    }
}
