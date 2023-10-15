package io.github.stuff_stuffs.aiex.common.internal.brain.task.default_impls;

import io.github.stuff_stuffs.aiex.common.api.AiExApi;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResource;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.Task;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DefaultUseItemTask<T extends LivingEntity> implements Task<BasicTasks.UseItem.Result, T> {
    private final BasicTasks.UseItem.Parameters parameters;
    private final Hand hand;
    private boolean nextFinish = false;
    private @Nullable BrainResources.Token handToken = null;
    private @Nullable BrainResources.Token offHandToken = null;

    public DefaultUseItemTask(final BasicTasks.UseItem.Parameters parameters) {
        this.parameters = parameters;
        hand = parameters.hand();
    }

    @Override
    public BasicTasks.UseItem.Result run(final BrainContext<T> context) {
        if (handToken == null || !handToken.active()) {
            handToken = context.brain().resources().get(hand == Hand.MAIN_HAND ? BrainResource.MAIN_HAND_CONTROL : BrainResource.OFF_HAND_CONTROL).orElse(null);
            if (handToken == null) {
                return new BasicTasks.UseItem.ResourceAcquisitionError();
            }
        }
        final T entity = context.entity();
        if (entity.getActiveHand() != hand && entity.isUsingItem()) {
            return new BasicTasks.UseItem.UsingOtherHandError();
        } else if (entity.isUsingItem()) {
            if (nextFinish) {
                return new BasicTasks.UseItem.Finished(entity.getStackInHand(hand));
            }
            return new BasicTasks.UseItem.UseTick(entity.getItemUseTime(), entity.getItemUseTimeLeft());
        }
        final int timeLeft = entity.getItemUseTimeLeft();
        if (timeLeft != 0) {
            if (timeLeft == 1) {
                nextFinish = true;
            }
            return new BasicTasks.UseItem.UseTick(entity.getItemUseTime(), timeLeft);
        }
        final ItemStack stackInHand = entity.getStackInHand(hand);
        final ItemCooldownManager cooldownManager = AiExApi.COOLDOWN_MANAGER.find(entity, null);
        if (cooldownManager != null) {
            if (cooldownManager.isCoolingDown(stackInHand.getItem())) {
                return new BasicTasks.UseItem.CooldownWait(cooldownManager.getCooldownProgress(stackInHand.getItem(), 0.0F));
            }
        }
        final UseAction action = stackInHand.getUseAction();
        if (action == UseAction.BLOCK || action == UseAction.BOW || action == UseAction.CROSSBOW || action == UseAction.SPYGLASS) {
            if (offHandToken == null || !offHandToken.active()) {
                offHandToken = context.brain().resources().get(hand != Hand.MAIN_HAND ? BrainResource.MAIN_HAND_CONTROL : BrainResource.OFF_HAND_CONTROL).orElse(null);
                if (offHandToken == null) {
                    return new BasicTasks.UseItem.UsingOtherHandError();
                }
            }
        }
        final double reachDistance = context.brain().config().get(BrainConfig.DEFAULT_REACH_DISTANCE);
        if (parameters instanceof BasicTasks.UseItem.AutoParameters || parameters instanceof BasicTasks.UseItem.EntityParameters) {
            final BasicTasks.UseItem.Result result = tryEntity(context, parameters, hand, reachDistance);
            if (result != null) {
                return result;
            } else if (parameters instanceof BasicTasks.UseItem.EntityParameters) {
                return new BasicTasks.UseItem.Miss();
            }
        }
        if (parameters instanceof BasicTasks.UseItem.AutoParameters || parameters instanceof BasicTasks.UseItem.BlockParameters) {
            final BasicTasks.UseItem.Result result = tryBlock(context, parameters, hand, reachDistance);
            if (result != null) {
                return result;
            } else if (parameters instanceof BasicTasks.UseItem.BlockParameters) {
                return new BasicTasks.UseItem.Miss();
            }
        }
        final TypedActionResult<ItemStack> use = stackInHand.use(context.world(), context.playerDelegate(), hand);
        return new BasicTasks.UseItem.Use(use.getResult(), use.getValue());
    }

    private static <T extends LivingEntity> BasicTasks.UseItem.@Nullable Result tryEntity(final BrainContext<T> context, final BasicTasks.UseItem.Parameters parameters, final Hand hand, final double reachDistance) {
        final T entity = context.entity();
        final EntityHitResult raycast;
        final Vec3d eyePos = entity.getEyePos();
        final Vec3d reachVec = entity.getRotationVec(1.0F).multiply(reachDistance);
        if (parameters instanceof BasicTasks.UseItem.AutoParameters) {
            final Box box = entity.getBoundingBox().stretch(reachVec).expand(1.0);
            final EntityHitResult result = ProjectileUtil.raycast(entity, eyePos, eyePos.add(reachVec), box, EntityPredicates.VALID_LIVING_ENTITY.and(EntityPredicates.EXCEPT_SPECTATOR), reachDistance * reachDistance);
            if (result == null) {
                return null;
            } else {
                raycast = result;
            }
        } else {
            final BasicTasks.UseItem.EntityParameters entityParameters = (BasicTasks.UseItem.EntityParameters) parameters;
            final Box box = entityParameters.entity().getBoundingBox();
            final double distSq = DefaultEntityLookTask.distSqToBox(eyePos, box);
            if (distSq > reachDistance * reachDistance) {
                return null;
            } else {
                final Vec3d otherEye = entityParameters.entity().getEyePos();
                final Vec3d target = DefaultEntityLookTask.computeTarget(eyePos, box, otherEye, context.world(), RaycastContext.ShapeType.OUTLINE, entity, context.randomSeed());
                if (target == null) {
                    return null;
                } else {
                    final Optional<Vec3d> r = box.raycast(eyePos, target.add(target.subtract(eyePos).multiply(0.01)));
                    if (r.isEmpty()) {
                        return null;
                    } else {
                        raycast = new EntityHitResult(entityParameters.entity(), r.get());
                    }
                }
            }
        }
        final ItemStack stackInHand = entity.getStackInHand(hand);
        final ActionResult result = stackInHand.useOnEntity(context.playerDelegate(), (LivingEntity) raycast.getEntity(), hand);
        if (result.isAccepted() || parameters instanceof BasicTasks.UseItem.EntityParameters) {
            return new BasicTasks.UseItem.UseOnEntity(result);
        } else {
            return null;
        }
    }

    private static <T extends LivingEntity> BasicTasks.UseItem.@Nullable Result tryBlock(final BrainContext<T> context, final BasicTasks.UseItem.Parameters parameters, final Hand hand, final double reachDistance) {
        final T entity = context.entity();
        final HitResult raycast;
        if (parameters instanceof BasicTasks.UseItem.AutoParameters) {
            raycast = entity.raycast(reachDistance, 1.0F, false);
        } else {
            final BasicTasks.UseItem.BlockParameters blockParameters = (BasicTasks.UseItem.BlockParameters) parameters;
            final ServerWorld world = context.world();
            final BlockState state = world.getBlockState(blockParameters.pos());
            final VoxelShape shape = state.getOutlineShape(world, blockParameters.pos(), ShapeContext.of(entity));
            if (shape.isEmpty()) {
                raycast = BlockHitResult.createMissed(Vec3d.ofCenter(blockParameters.pos()), Direction.UP, blockParameters.pos());
            } else {
                final Vec3d eyePos = entity.getEyePos();
                raycast = shape.raycast(eyePos, eyePos.add(entity.getRotationVec(1.0F).multiply(reachDistance)), blockParameters.pos());
                assert raycast != null;
            }
        }
        if (raycast.getType() == HitResult.Type.MISS && parameters instanceof BasicTasks.UseItem.BlockParameters) {
            return new BasicTasks.UseItem.Miss();
        } else {
            final ItemStack stackInHand = entity.getStackInHand(hand);
            final ActionResult result = stackInHand.useOnBlock(new ItemUsageContext(context.playerDelegate(), hand, (BlockHitResult) raycast));
            if (result.isAccepted() || parameters instanceof BasicTasks.UseItem.BlockParameters) {
                return new BasicTasks.UseItem.UseOnBlock(result);
            } else {
                return null;
            }
        }
    }

    @Override
    public void stop(final BrainContext<T> context) {
        if (handToken != null && handToken.active()) {
            final T entity = context.entity();
            if (entity.isUsingItem() && entity.getActiveHand() == hand) {
                entity.stopUsingItem();
            }
            context.brain().resources().release(handToken);
            handToken = null;
        }
        if (offHandToken != null && offHandToken.active()) {
            context.brain().resources().release(offHandToken);
            offHandToken = null;
        }
    }
}
