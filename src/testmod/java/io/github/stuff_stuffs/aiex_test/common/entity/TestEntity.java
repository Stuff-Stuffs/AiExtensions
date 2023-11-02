package io.github.stuff_stuffs.aiex_test.common.entity;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestEntry;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestReference;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrain;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.basic.LoadMemoryNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.basic.NamedForgettingNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.basic.NamedRememberingBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.basic.target.BrainNodeTargets;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.IfBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicNpcEntityPather;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicPathingUniverse;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.PathingNpcEntity;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.entity.DelegatingPlayerInventory;
import io.github.stuff_stuffs.aiex_test.common.AiExTestCommon;
import io.github.stuff_stuffs.aiex_test.common.aoi.BasicAreaOfInterest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public class TestEntity extends AbstractNpcEntity implements PathingNpcEntity {
    private static final TrackedData<Boolean> SLIM_DATA = DataTracker.registerData(TestEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final AiBrain brain;
    private final BasicNpcEntityPather navigator;
    private final DelegatingPlayerInventory inventory;

    protected TestEntity(final EntityType<? extends MobEntity> entityType, final World world) {
        super(entityType, world);
        navigator = new BasicNpcEntityPather(this);
        if (world instanceof ServerWorld) {
            final BrainNode<TestEntity, Optional<AreaOfInterestReference<BasicAreaOfInterest>>, Unit> findHome = LoadMemoryNode.loadOrElse(
                    AiExTestCommon.HOME_MEMORY_NAME,
                    BrainNodeTargets.<TestEntity, AreaOfInterestReference<BasicAreaOfInterest>, Unit, BasicAreaOfInterest>areaTarget(
                            AiExTestCommon.BASIC_AOI_TYPE,
                            (context, arg, events) -> events.filter(entry -> entry.value().claimedBy().isEmpty()).map(AreaOfInterestEntry::reference).findFirst(),
                            64,
                            true
                    ).adaptResult(
                            opt -> opt.orElse(null)
                    )).adaptResult(Optional::ofNullable);
            final NamedRememberingBrainNode<TestEntity, AreaOfInterestReference<BasicAreaOfInterest>, AreaOfInterestReference<BasicAreaOfInterest>> remembering = new NamedRememberingBrainNode<>(
                    AiExTestCommon.HOME_MEMORY_NAME,
                    (context, arg) -> arg
            );
            final NamedForgettingNode<TestEntity, AreaOfInterestReference<BasicAreaOfInterest>, Unit> forgetting = new NamedForgettingNode<>(AiExTestCommon.HOME_MEMORY_NAME);
            final IfBrainNode<TestEntity, Unit, Optional<AreaOfInterestReference<BasicAreaOfInterest>>> tryRemember = new IfBrainNode<>(
                    remembering.<Optional<AreaOfInterestReference<BasicAreaOfInterest>>>adaptArg(
                            Optional::get
                    ).adaptResult(
                            res -> Unit.INSTANCE
                    ), forgetting.adaptResult(
                    res -> Unit.INSTANCE
            ).adaptArg(
                    res -> Unit.INSTANCE
            ),
                    (context, reference) -> reference.isPresent()
            );
            final BrainNode<TestEntity, Optional<AreaOfInterestReference<BasicAreaOfInterest>>, Unit> findHomeAndRemember = findHome.chain(
                    tryRemember.contextCapture(
                            (arg, ret) -> arg)
            ).cache();
            final BrainNode<TestEntity, Res, AreaOfInterestEntry<BasicAreaOfInterest>> walkToHomeAndClaim = BrainNodes.expectResult(
                    new TaskBrainNode<>(
                            BasicTasks.Walk.KEY,
                            (BiFunction<AreaOfInterestEntry<BasicAreaOfInterest>, BrainContext<TestEntity>, BasicTasks.Walk.Parameters>) (reference, context) -> {
                                final Vec3d center = Vec3d.ofBottomCenter(reference.bounds().center());
                                return new BasicTasks.Walk.Parameters() {
                                    @Override
                                    public Vec3d target() {
                                        return center;
                                    }

                                    @Override
                                    public double maxError() {
                                        return 2;
                                    }
                                };
                            },
                            (reference, context) -> null
                    ),
                    RuntimeException::new
            ).contextCapture(Pair::of).ifThen(
                    (context, pair) -> pair.getSecond() == BasicTasks.Walk.Result.DONE,
                    BrainNodes.terminal(
                            (context, pair) -> pair.getFirst().value().visit(context.world(), context.entity()) ? Res.DONE : Res.RESET
                    ),
                    BrainNodes.constant(Res.RESET)
            );
            final BrainNode<TestEntity, Res, Unit> node = findHomeAndRemember.adaptResult(
                    (context, reference) -> reference.flatMap(ref -> ((AiWorldExtensions) context.world()).aiex$getAoiWorld().get(ref))
            ).ifThen(
                    (context, reference) -> reference.isPresent(),
                    walkToHomeAndClaim.adaptArg(Optional::get),
                    BrainNodes.constant(Res.WALKING_TO)
            ).resetOnResult(
                    res -> res == Res.RESET
            );
            brain = AiBrain.create(this, node.discardResult(), BrainConfig.builder().build(), TaskConfig.<TestEntity>builder().build(this), AiExCommon.createForEntity(this));
            inventory = new DelegatingPlayerInventory(aiex$getBrain().fakePlayerDelegate(), getNpcInventory());
        } else {
            brain = AiBrain.createClient();
            inventory = null;
        }

        updateSlim();

    }

    private enum Res {
        WALKING_TO,
        DONE,
        RESET
    }


    @Override
    public PlayerInventory getInventory() {
        return inventory;
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
    public Collection<LocationClassifier<?>> ensuredLocationClassifiers() {
        return Collections.singleton(BasicPathingUniverse.CLASSIFIER);
    }
}
