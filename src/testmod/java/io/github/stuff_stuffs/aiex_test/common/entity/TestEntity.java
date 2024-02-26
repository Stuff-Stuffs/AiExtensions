package io.github.stuff_stuffs.aiex_test.common.entity;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrain;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.BehaviorDecider;
import io.github.stuff_stuffs.aiex.common.api.brain.behavior.BehaviorHandlerMap;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicNpcEntityPather;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.BasicPathingUniverse;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.PathingNpcEntity;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.entity.DelegatingPlayerInventory;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Arm;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TestEntity extends AbstractNpcEntity implements PathingNpcEntity {
    private static final TrackedData<Boolean> SLIM_DATA = DataTracker.registerData(TestEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private final AiBrain brain;
    private final BasicNpcEntityPather navigator;
    private final DelegatingPlayerInventory inventory;

    protected TestEntity(final EntityType<? extends MobEntity> entityType, final World world) {
        super(entityType, world);
        navigator = new BasicNpcEntityPather(this);
        if (world instanceof ServerWorld) {
            final BehaviorHandlerMap map = BehaviorHandlerMap.create(entityType);
            brain = AiBrain.create(this, BehaviorDecider.create(map), BrainConfig.builder().build(), TaskConfig.<TestEntity>builder().build(this), AiExCommon.createForEntity(this));
            inventory = new DelegatingPlayerInventory(aiex$getBrain().fakePlayerDelegate(), getNpcInventory());
        } else {
            brain = AiBrain.createClient();
            inventory = null;
        }

        updateSlim();

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
