package io.github.stuff_stuffs.aiex.common.internal.entity;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.*;
import net.minecraft.entity.ai.brain.task.Task;
import net.minecraft.server.world.ServerWorld;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class DummyBrain<T extends LivingEntity> extends Brain<T> {
    private DummyBrain(final Supplier<Codec<Brain<T>>> codecSupplier) {
        super(Collections.emptySet(), Collections.emptySet(), ImmutableList.of(), codecSupplier);
    }

    @Override
    public <T1> DataResult<T1> encode(final DynamicOps<T1> ops) {
        return DataResult.success(ops.empty());
    }

    @Override
    public boolean hasMemoryModule(final MemoryModuleType<?> type) {
        return false;
    }

    @Override
    public void forgetAll() {
    }

    @Override
    public <U> void forget(final MemoryModuleType<U> type) {
    }

    @Override
    public <U> void remember(final MemoryModuleType<U> type, @Nullable final U value) {
    }

    @Override
    public <U> void remember(final MemoryModuleType<U> type, final U value, final long expiry) {
    }

    @Override
    public <U> void remember(final MemoryModuleType<U> type, final Optional<? extends U> value) {
    }

    @Override
    public <U> Optional<U> getOptionalRegisteredMemory(final MemoryModuleType<U> type) {
        return Optional.empty();
    }

    @Nullable
    @Override
    public <U> Optional<U> getOptionalMemory(final MemoryModuleType<U> type) {
        return Optional.empty();
    }

    @Override
    public <U> long getMemoryExpiry(final MemoryModuleType<U> type) {
        return 0;
    }

    @Override
    public Map<MemoryModuleType<?>, Optional<? extends Memory<?>>> getMemories() {
        return Collections.emptyMap();
    }

    @Override
    public <U> boolean hasMemoryModuleWithValue(final MemoryModuleType<U> type, final U value) {
        return false;
    }

    @Override
    public boolean isMemoryInState(final MemoryModuleType<?> type, final MemoryModuleState state) {
        return false;
    }

    @Override
    public Schedule getSchedule() {
        return Schedule.EMPTY;
    }

    @Override
    public void setSchedule(final Schedule schedule) {
    }

    @Override
    public void setCoreActivities(final Set<Activity> coreActivities) {
    }

    @Override
    public Set<Activity> getPossibleActivities() {
        return Collections.emptySet();
    }

    @Override
    public List<Task<? super T>> getRunningTasks() {
        return Collections.emptyList();
    }

    @Override
    public void resetPossibleActivities() {
    }

    @Override
    public Optional<Activity> getFirstPossibleNonCoreActivity() {
        return Optional.empty();
    }

    @Override
    public void doExclusively(final Activity activity) {
    }

    @Override
    public void refreshActivities(final long timeOfDay, final long time) {
    }

    @Override
    public void resetPossibleActivities(final List<Activity> activities) {
    }

    @Override
    public void setDefaultActivity(final Activity activity) {
    }

    @Override
    public void setTaskList(final Activity activity, final int begin, final ImmutableList<? extends Task<? super T>> list) {
    }

    @Override
    public void setTaskList(final Activity activity, final int begin, final ImmutableList<? extends Task<? super T>> tasks, final MemoryModuleType<?> memoryType) {
    }

    @Override
    public void setTaskList(final Activity activity, final ImmutableList<? extends Pair<Integer, ? extends Task<? super T>>> indexedTasks) {
    }

    @Override
    public void setTaskList(final Activity activity, final ImmutableList<? extends Pair<Integer, ? extends Task<? super T>>> indexedTasks, final Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories) {
    }

    @Override
    public void setTaskList(final Activity activity, final ImmutableList<? extends Pair<Integer, ? extends Task<? super T>>> indexedTasks, final Set<Pair<MemoryModuleType<?>, MemoryModuleState>> requiredMemories, final Set<MemoryModuleType<?>> forgettingMemories) {
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean hasActivity(final Activity activity) {
        return false;
    }

    @Override
    public Brain<T> copy() {
        return this;
    }

    @Override
    public void tick(final ServerWorld world, final T entity) {
    }

    @Override
    public void stopAllTasks(final ServerWorld world, final T entity) {
    }

    public static <T extends LivingEntity> DummyBrain<T> create() {
        final MutableObject<DummyBrain<T>> brain = new MutableObject<>();
        final Codec<Brain<T>> dummyCodec = Codec.unit(brain::getValue);
        brain.setValue(new DummyBrain<>(() -> dummyCodec));
        return brain.getValue();
    }
}
