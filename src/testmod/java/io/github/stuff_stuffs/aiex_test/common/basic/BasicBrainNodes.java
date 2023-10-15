package io.github.stuff_stuffs.aiex_test.common.basic;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskTerminalBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import io.github.stuff_stuffs.aiex.common.api.util.avoidance.ProjectileAvoidance;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.FuzzyPositions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.math.random.Xoroshiro128PlusPlusRandom;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

public final class BasicBrainNodes {
    public static <C extends Entity> BrainNode<C, Optional<Vec3d>, Unit> nearestPlayer() {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context) {

            }

            @Override
            public Optional<Vec3d> tick(final BrainContext<C> context, final Unit arg) {
                final PlayerEntity player = context.entity().getEntityWorld().getClosestPlayer(context.entity(), 128.0);
                if (player != null) {
                    return Optional.of(player.getPos());
                }
                return Optional.empty();
            }

            @Override
            public void deinit(final BrainContext<C> context) {

            }
        };
    }

    public static <C> BrainNode<C, TaskTerminalBrainNode.Result<BasicTasks.Walk.Result>, Vec3d> walk(final TaskKey<BasicTasks.Walk.Result, BasicTasks.Walk.Parameters> key, final double maxError) {
        return walk(key, vec3d -> new BasicTasks.Walk.Parameters() {
            @Override
            public Vec3d target() {
                return vec3d;
            }

            @Override
            public double maxError() {
                return maxError;
            }
        });
    }

    public static <C, FC> BrainNode<C, TaskTerminalBrainNode.Result<BasicTasks.Walk.Result>, FC> walk(final TaskKey<BasicTasks.Walk.Result, BasicTasks.Walk.Parameters> key, final Function<FC, BasicTasks.Walk.Parameters> parameterFactory) {
        return new TaskTerminalBrainNode<>(key, (ctx, context) -> parameterFactory.apply(ctx));
    }

    private static <C extends LivingEntity> Optional<Pair<DodgeKey, ToDoubleFunction<Vec3d>>> create(final List<ProjectileAvoidance.DangerScorer> scorers, final BrainContext<C> context, final Set<UUID> previous) {
        if (scorers.size() != previous.size()) {
            return Optional.empty();
        }
        final Set<UUID> ids = new ObjectOpenHashSet<>();
        for (final ProjectileAvoidance.DangerScorer scorer : scorers) {
            ids.add(scorer.id);
        }
        final ToDoubleFunction<Vec3d> total = value -> {
            double s = 0.0;
            for (final ProjectileAvoidance.DangerScorer scorer : scorers) {
                s = s + scorer.score(value);
            }
            return s;
        };
        return Optional.of(Pair.of(new DodgeKey(ids, total.applyAsDouble(context.entity().getPos()), total, context.brain().age()), total));
    }

    private static <T extends Entity> @Nullable Data target(final List<ProjectileAvoidance.DangerScorer> avoidances, final BrainContext<T> context) {
        final ToDoubleFunction<Vec3d> scorer;
        if (avoidances.size() == 1) {
            scorer = p -> avoidances.get(0).score(p);
        } else {
            scorer = pos -> {
                double s = 0;
                for (final ProjectileAvoidance.DangerScorer function : avoidances) {
                    s = s + function.score(pos);
                }
                return s;
            };
        }
        final Random random = new Xoroshiro128PlusPlusRandom(context.randomSeed());
        final int baseX = context.entity().getBlockX();
        final int baseY = context.entity().getBlockY();
        final int baseZ = context.entity().getBlockZ();
        final Vec3d best = FuzzyPositions.guessBest(() -> {
            final int x = baseX + random.nextInt(9) - 4;
            final int y = baseY + random.nextInt(5) - 2;
            final int z = baseZ + random.nextInt(9) - 4;
            final World world = context.entity().getEntityWorld();
            return FuzzyPositions.upWhile(new BlockPos(x, y, z), baseY + 8, pos -> world.getBlockState(pos).isSolidBlock(world, pos));
        }, value -> scorer.applyAsDouble(Vec3d.of(value)));
        if (best == null) {
            return null;
        }
        return new Data(best, scorer, scorer.applyAsDouble(best), avoidances.size());
    }

    private record DangerEntry(ProjectileAvoidance.DangerScorer scorer, UUID id) {
    }

    private record Data(Vec3d target, ToDoubleFunction<Vec3d> scorer, double score, int projectileCount) {
    }

    private record DodgeKey(Set<UUID> uuids, double score, ToDoubleFunction<Vec3d> scorer, long timestamp) {
    }


    private BasicBrainNodes() {
    }
}
