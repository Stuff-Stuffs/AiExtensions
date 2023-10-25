package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
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

public class MineBlockBrainNode<C extends MobEntity> implements BrainNode<C, MineBlockBrainNode.Result, MineBlockBrainNode.Arguments> {
    private State state;

    public MineBlockBrainNode() {
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public Result tick(final BrainContext<C> context, final Arguments arg, final SpannedLogger logger) {
        try (final var l = logger.open("MineBlock")) {
            if (state != null && !arg.pos().equals(state.pos)) {
                state.deinit(context);
                state = null;
            }
            if (state == null) {
                state = new State(arg.pos());
            }
            return state.tick(context, arg.repository(), l);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var l = logger.open("MineBlock")) {
            if (state != null) {
                state.deinit(context);
            }
        }
    }

    public sealed interface Result {
    }

    public record BlockSwap(BlockState expected, BlockState got) implements Result {
    }

    public record Error(ErrorType type) implements Result {
    }

    public record Broken() implements Result {
    }

    public record Continue(float blockBreakingDelta) implements Result {
    }

    public enum ErrorType {
        NO_OUTLINE_SHAPE,
        CANT_REACH,
        RESOURCE_ACQUISITION
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

        private <C extends MobEntity> Result tick(final BrainContext<C> context, final BrainResourceRepository arg, final SpannedLogger logger) {
            if (!breaking) {
                final Result r = setup(context, arg);
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
                        return new BlockSwap(this.state, state);
                    }
                    return new Error(ErrorType.CANT_REACH);
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

        private <C extends MobEntity> Result tick(final BrainContext<C> context, final BrainResourceRepository arg) {
            if (armToken == null || !armToken.active()) {
                armToken = arg.get(BrainResource.MAIN_HAND_CONTROL).orElse(null);
                if (armToken == null) {
                    return new Error(ErrorType.RESOURCE_ACQUISITION);
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
                return new Broken();
            } else {
                context.world().setBlockBreakingInfo(context.entity().getId(), pos, (int) (f * 10.0F));
                return new Continue(f);
            }
        }

        @Nullable
        private <C extends Entity> Result setup(final BrainContext<C> context, final BrainResourceRepository repository) {
            if (armToken == null || armToken.active()) {
                if (armToken != null) {
                    context.brain().resources().release(armToken);
                }
                armToken = repository.get(BrainResource.MAIN_HAND_CONTROL).orElse(null);
                if (armToken == null) {
                    return new Error(ErrorType.RESOURCE_ACQUISITION);
                }
            }
            final BlockState state = context.world().getBlockState(pos);
            if (state.isAir()) {
                return new Broken();
            }
            final Vec3d eyePos = context.entity().getEyePos();
            final Vec3d p = eyePos.subtract(pos.getX(), pos.getY(), pos.getZ());
            final VoxelShape outlineShape = state.getOutlineShape(context.world(), pos);
            final Optional<Vec3d> to = outlineShape.getClosestPointTo(p);
            if (to.isEmpty()) {
                return new Error(ErrorType.NO_OUTLINE_SHAPE);
            }
            final Vec3d target = to.get().add(pos.getX(), pos.getY(), pos.getZ());
            final double reach = context.brain().config().get(BrainConfig.DEFAULT_REACH_DISTANCE);
            if (target.squaredDistanceTo(eyePos) > reach * reach) {
                return new Error(ErrorType.CANT_REACH);
            }
            final Vec3d delta = target.subtract(eyePos);
            final Vec3d end = target.add(delta.multiply(0.01));
            final BlockHitResult raycast = outlineShape.raycast(eyePos, end, pos);
            if (raycast == null) {
                return new Error(ErrorType.CANT_REACH);
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

    public interface Arguments {
        BlockPos pos();

        BrainResourceRepository repository();
    }
}
