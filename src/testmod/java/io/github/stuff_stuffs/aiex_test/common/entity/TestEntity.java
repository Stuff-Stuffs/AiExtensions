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
import io.github.stuff_stuffs.aiex.common.api.brain.memory.BasicMemories;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.basic.BasicBrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.basic.memory.NamedForgettingBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.basic.memory.NamedMemoryLoadBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.basic.memory.NamedRememberingBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.basic.target.BrainNodeTargets;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow.IfBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow.SelectorBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow.TaskBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicNpcEntityPather;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicPathingUniverse;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.EntityPather;
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

import java.util.*;
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
            final BrainNode<TestEntity, Optional<AreaOfInterestReference<BasicAreaOfInterest>>, Unit> findHome = BrainNodes.or(
                    new NamedMemoryLoadBrainNode<TestEntity, AreaOfInterestReference<BasicAreaOfInterest>, Unit>(AiExTestCommon.HOME_MEMORY_NAME).adaptResult(
                            opt -> opt.map(
                                    Memory::get
                            )
                    ),
                    BrainNodeTargets.<TestEntity, BasicAreaOfInterest, Unit>findReachable(
                            BasicMemories.BASIC_UNREACHABLE_AREA_NAME,
                            AiExTestCommon.BASIC_AOI_TYPE,
                            (context, arg, entry) -> entry.value().isClaimedBy(context.entity().getUuid())
                                    || entry.value().claimedBy().isEmpty(),
                            64,
                            true
                    ).adaptResult(opt -> opt.map(AreaOfInterestEntry::reference))
            );
            final var remembering = new NamedRememberingBrainNode<TestEntity, AreaOfInterestReference<BasicAreaOfInterest>, AreaOfInterestReference<BasicAreaOfInterest>>(
                    AiExTestCommon.HOME_MEMORY_NAME,
                    (context, arg) -> arg
            );
            final var forgetting = new NamedForgettingBrainNode<TestEntity, AreaOfInterestReference<BasicAreaOfInterest>, Unit>(AiExTestCommon.HOME_MEMORY_NAME);
            final var tryRemember = new IfBrainNode<>(
                    remembering.<Optional<AreaOfInterestReference<BasicAreaOfInterest>>>adaptArg(
                            Optional::get
                    ).discardResult(),
                    forgetting.discardResult().adaptArg(opt -> Unit.INSTANCE),
                    (context, reference) -> reference.isPresent()
            );
            final var findHomeAndRemember = findHome.chain(
                    tryRemember.contextCapture(
                            (arg, ret) -> arg
                    )
            );
            final var rememberUnreachable = BasicBrainNodes.<TestEntity>rememberUnreachable(BasicMemories.BASIC_UNREACHABLE_AREA_NAME);
            final var dispatch = SelectorBrainNode.<TestEntity, Res, Pair<AreaOfInterestEntry<BasicAreaOfInterest>, BasicTasks.Walk.Result>>builder().add(
                    (context, pair) -> pair.getSecond() == BasicTasks.Walk.Result.DONE,
                    BrainNodes.terminal(
                            (context, pair) -> pair.getFirst().value().visit(context.world(), context.entity()) ? Res.DONE : Res.RESET
                    )
            ).add(
                    (context, pair) -> pair.getSecond() == BasicTasks.Walk.Result.CANNOT_REACH,
                    rememberUnreachable.adaptResult(unit -> Res.RESET).adaptArg(pair -> pair.getFirst().reference())
            ).add(
                    (context, pair) -> true,
                    BrainNodes.terminal((context, pair) -> pair.getSecond() == BasicTasks.Walk.Result.CONTINUE ? Res.WALKING_TO : Res.RESET)
            ).build();
            final var walkToHomeAndClaim = TaskBrainNode.expectedTask(
                    BasicTasks.Walk.KEY,
                    (BiFunction<AreaOfInterestEntry<BasicAreaOfInterest>, BrainContext<TestEntity>, BasicTasks.Walk.Parameters>) (reference, context) -> {
                        final Vec3d center = Vec3d.ofBottomCenter(reference.bounds().center());
                        return new BasicTasks.Walk.Parameters() {
                            @Override
                            public EntityPather.Target target() {
                                return new EntityPather.SingleTarget(center);
                            }

                            @Override
                            public double maxError() {
                                return 2;
                            }
                        };
                    },
                    (reference, context) -> null
            ).contextCapture(Pair::of).chain(dispatch);
            final var node = findHomeAndRemember.adaptResult(
                    (context, reference) -> reference.flatMap(ref -> ((AiWorldExtensions) context.world()).aiex$getAoiWorld().get(ref))
            ).ifThen(
                    (context, reference) -> reference.isPresent(),
                    walkToHomeAndClaim.adaptArg(Optional::get),
                    BrainNodes.constant(Res.RESET)
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
    protected void initDataTracker(final DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(SLIM_DATA, false);
    }

    @Override
    public void onDataTrackerUpdate(final List<DataTracker.SerializedEntry<?>> entries) {
        super.onDataTrackerUpdate(entries);
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
