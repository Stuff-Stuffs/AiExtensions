package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DefaultMineBlockTask<C extends MobEntity> implements BrainNode<C, BasicTasks.MineBlock.Result, BrainResourceRepository> {
    private final BlockPos pos;
    private State state;

    public DefaultMineBlockTask(final BlockPos pos) {
        this.pos = pos.toImmutable();
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public BasicTasks.MineBlock.Result tick(final BrainContext<C> context, final BrainResourceRepository arg, final SpannedLogger logger) {
        try (final var l = logger.open("MineBlock")) {
            if (state == null) {
                state = new State(pos);
            }
            return state.tick(context, arg, l);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var l = logger.open("MineBlock")) {
            if (state != null) {
                state.deinit(context);
                state = null;
            }
        }
    }

    private static final class State {
        private final BlockPos pos;
        private boolean breaking = false;
        private boolean started = false;
        private long startedMiningAge;
        private BlockState state = null;
        private BrainResources.Token armToken = null;

        private State(final BlockPos pos) {
            this.pos = pos.toImmutable();
        }

        private <C extends MobEntity> BasicTasks.MineBlock.Result tick(final BrainContext<C> context, final BrainResourceRepository arg, final SpannedLogger logger) {
            if (!breaking) {
                final BasicTasks.MineBlock.Result r = setup(context, arg);
                if (r != null) {
                    return r;
                }
            } else {
                if (!check(context)) {
                    if (started) {
                        context.world().setBlockBreakingInfo(context.entity().getId(), pos, -1);
                    }
                    breaking = false;
                    started = false;
                    final BlockState state = context.world().getBlockState(pos);
                    if (state != this.state) {
                        return new BasicTasks.MineBlock.BlockSwap(this.state, state);
                    }
                    return new BasicTasks.MineBlock.Error(BasicTasks.MineBlock.ErrorType.CANT_REACH);
                }
            }
            return tick(context, arg);
        }

        private <C extends MobEntity> boolean check(final BrainContext<C> context) {
            final BlockState state = context.world().getBlockState(pos);
            if (state.isAir() || state != this.state) {
                return false;
            }
            final Vec3d eyePos = context.entity().getEyePos();
            final Vec3d p = eyePos.subtract(pos.getX(), pos.getY(), pos.getZ());
            final VoxelShape outlineShape = state.getOutlineShape(context.world(), pos);
            final Optional<Vec3d> to = outlineShape.getClosestPointTo(p);
            if (to.isEmpty()) {
                return false;
            }
            final Vec3d target = to.get().add(pos.getX(), pos.getY(), pos.getZ());
            final double reach = context.brain().config().get(BrainConfig.DEFAULT_REACH_DISTANCE);
            if (target.squaredDistanceTo(eyePos) > reach * reach) {
                return false;
            }
            final Vec3d delta = target.subtract(eyePos);
            final Vec3d end = target.add(delta.multiply(0.01));
            final BlockHitResult raycast = outlineShape.raycast(eyePos, end, pos);
            return raycast != null;
        }

        private <C extends MobEntity> BasicTasks.MineBlock.Result tick(final BrainContext<C> context, final BrainResourceRepository arg) {
            if (armToken == null || !armToken.active()) {
                armToken = arg.get(BrainResource.ACTIVE_MAIN_HAND_CONTROL).orElse(null);
                if (armToken == null) {
                    return new BasicTasks.MineBlock.Error(BasicTasks.MineBlock.ErrorType.RESOURCE_ACQUISITION);
                }
            }
            if (!started) {
                startedMiningAge = context.entity().age;
                state.onBlockBreakStart(context.world(), pos, context.playerDelegate());
                started = true;
            }
            final float f = state.calcBlockBreakingDelta(context.playerDelegate(), context.world(), pos) * (context.entity().age - startedMiningAge + 1);
            if (f >= 1.0F) {
                finishMining(context);
                started = false;
                breaking = false;
                state = null;
                context.world().setBlockBreakingInfo(context.entity().getId(), pos, -1);
                context.brain().resources().release(armToken);
                return new BasicTasks.MineBlock.Broken();
            } else {
                context.world().setBlockBreakingInfo(context.entity().getId(), pos, (int) (f * 10.0F));
                return new BasicTasks.MineBlock.Continue(f);
            }
        }

        @Nullable
        private <C extends Entity> BasicTasks.MineBlock.Result setup(final BrainContext<C> context, final BrainResourceRepository repository) {
            if (armToken == null || armToken.active()) {
                if (armToken != null) {
                    context.brain().resources().release(armToken);
                }
                armToken = repository.get(BrainResource.ACTIVE_MAIN_HAND_CONTROL).orElse(null);
                if (armToken == null) {
                    return new BasicTasks.MineBlock.Error(BasicTasks.MineBlock.ErrorType.RESOURCE_ACQUISITION);
                }
            }
            final BlockState state = context.world().getBlockState(pos);
            if (state.isAir()) {
                return new BasicTasks.MineBlock.Broken();
            }
            final Vec3d eyePos = context.entity().getEyePos();
            final Vec3d p = eyePos.subtract(pos.getX(), pos.getY(), pos.getZ());
            final VoxelShape outlineShape = state.getOutlineShape(context.world(), pos);
            final Optional<Vec3d> to = outlineShape.getClosestPointTo(p);
            if (to.isEmpty()) {
                return new BasicTasks.MineBlock.Error(BasicTasks.MineBlock.ErrorType.NO_OUTLINE_SHAPE);
            }
            final Vec3d target = to.get().add(pos.getX(), pos.getY(), pos.getZ());
            final double reach = context.brain().config().get(BrainConfig.DEFAULT_REACH_DISTANCE);
            if (target.squaredDistanceTo(eyePos) > reach * reach) {
                return new BasicTasks.MineBlock.Error(BasicTasks.MineBlock.ErrorType.CANT_REACH);
            }
            final Vec3d delta = target.subtract(eyePos);
            final Vec3d end = target.add(delta.multiply(0.01));
            final BlockHitResult raycast = outlineShape.raycast(eyePos, end, pos);
            if (raycast == null) {
                return new BasicTasks.MineBlock.Error(BasicTasks.MineBlock.ErrorType.CANT_REACH);
            }
            this.state = state;
            breaking = true;
            return null;
        }

        private <C extends MobEntity> void finishMining(final BrainContext<C> context) {
            context.world().setBlockBreakingInfo(context.entity().getId(), pos, -1);
            final BlockEntity blockEntity = context.world().getBlockEntity(pos);
            final Block block = state.getBlock();
            block.onBreak(context.world(), pos, state, context.playerDelegate());
            final boolean removed = context.world().removeBlock(pos, false);
            if (removed) {
                block.onBroken(context.world(), pos, state);
            }
            final ItemStack itemStack = context.entity().getMainHandStack();
            final ItemStack copy = itemStack.copy();
            final boolean harvest = context.playerDelegate().canHarvest(state);
            itemStack.postMine(context.world(), state, pos, context.playerDelegate());
            if (removed && harvest) {
                block.afterBreak(context.world(), context.playerDelegate(), pos, state, blockEntity, copy);
            }
        }

        private <C extends Entity> void deinit(final BrainContext<C> context) {
            if (armToken != null && armToken.active()) {
                context.brain().resources().release(armToken);
                armToken = null;
            }
            breaking = false;
            state = null;
            if (started) {
                context.world().setBlockBreakingInfo(context.entity().getId(), pos, -1);
                started = false;
            }
        }
    }
}
