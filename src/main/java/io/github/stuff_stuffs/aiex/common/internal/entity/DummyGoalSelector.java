package io.github.stuff_stuffs.aiex.common.internal.entity;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.util.profiler.Profiler;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class DummyGoalSelector extends GoalSelector {
    public DummyGoalSelector(final Supplier<Profiler> profiler) {
        super(profiler);
    }

    @Override
    public void add(final int priority, final Goal goal) {
    }

    @Override
    public void clear(final Predicate<Goal> predicate) {
    }

    @Override
    public void remove(final Goal goal) {
    }

    @Override
    public void tick() {
    }

    @Override
    public void tickGoals(final boolean tickAll) {
    }

    @Override
    public Set<PrioritizedGoal> getGoals() {
        return Collections.emptySet();
    }

    @Override
    public Stream<PrioritizedGoal> getRunningGoals() {
        return Stream.empty();
    }

    @Override
    public void setTimeInterval(final int timeInterval) {
    }

    @Override
    public void disableControl(final Goal.Control control) {
    }

    @Override
    public void enableControl(final Goal.Control control) {
    }

    @Override
    public void setControlEnabled(final Goal.Control control, final boolean enabled) {
    }
}
