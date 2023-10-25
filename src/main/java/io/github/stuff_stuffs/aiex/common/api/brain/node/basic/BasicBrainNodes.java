package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.IfBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.util.InventorySlot;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public final class BasicBrainNodes {
    public static <T extends MobEntity> BrainNode<T, MineBlockBrainNode.Result, MineParameters> mine() {
        return new TaskBrainNode<>(BasicTasks.SelectToolTask.KEY, (BiFunction<MineParameters, BrainContext<T>, BasicTasks.SelectToolTask.Parameters>) (repository, context) -> {
            final BlockState state = context.world().getBlockState(repository.pos);
            return () -> state;
        }, (repository, context) -> repository.repository).fallthroughChain(new BrainNode<T, Optional<MineParameters>, Pair<TaskBrainNode.Result<BasicTasks.SelectToolTask.Result>, MineParameters>>() {
            @Override
            public void init(final BrainContext<T> context, final SpannedLogger logger) {

            }

            @Override
            public Optional<MineParameters> tick(final BrainContext<T> context, final Pair<TaskBrainNode.Result<BasicTasks.SelectToolTask.Result>, MineParameters> arg, final SpannedLogger logger) {
                final TaskBrainNode.Result<BasicTasks.SelectToolTask.Result> opt = arg.getFirst();
                if (opt instanceof TaskBrainNode.Failure<BasicTasks.SelectToolTask.Result>) {
                    return Optional.empty();
                }
                final BasicTasks.SelectToolTask.Result result = ((TaskBrainNode.Success<BasicTasks.SelectToolTask.Result>) opt).value();
                if (result instanceof BasicTasks.SelectToolTask.NoneAvailableError) {
                    return Optional.empty();
                }
                final BasicTasks.SelectToolTask.Success success = (BasicTasks.SelectToolTask.Success) result;
                boolean tool = false;
                for (final InventorySlot either : success.bestToWorst()) {
                    final Optional<EquipmentSlot> left = either.equipmentSlot();
                    if (left.isPresent()) {
                        if (left.get() == EquipmentSlot.MAINHAND) {
                            tool = true;
                            break;
                        }
                    }
                    final Optional<BrainNode<T, BasicTasks.SwapStack.Result, BrainResourceRepository>> task = context.createTask(BasicTasks.SwapStack.KEY, new BasicTasks.SwapStack.Parameters() {
                        @Override
                        public InventorySlot source() {
                            return either;
                        }

                        @Override
                        public InventorySlot destination() {
                            return new InventorySlot(EquipmentSlot.MAINHAND);
                        }
                    }, logger);
                    if (task.isEmpty()) {
                        return Optional.empty();
                    }
                    final BrainNode<T, BasicTasks.SwapStack.Result, BrainResourceRepository> node = task.get();
                    node.init(context, logger);
                    final BasicTasks.SwapStack.Result tick = node.tick(context, arg.getSecond().repository(), logger);
                    node.deinit(context, logger);
                    if (tick == BasicTasks.SwapStack.Result.OK) {
                        tool = true;
                        break;
                    }
                }
                if (tool) {
                    return Optional.of(arg.getSecond());
                }
                return Optional.empty();
            }

            @Override
            public void deinit(final BrainContext<T> context, final SpannedLogger logger) {

            }
        }, Pair::of).chainedCache(new IfBrainNode<>(new MineBlockBrainNode<T>().adaptArg(Optional::get), BrainNodes.constant(new MineBlockBrainNode.Error(MineBlockBrainNode.ErrorType.RESOURCE_ACQUISITION)), (context, parameters) -> parameters.isPresent())).resetOnContext(new BiPredicate<>() {
            private BlockPos lastPos = BlockPos.ORIGIN;

            @Override
            public boolean test(final BrainContext<T> context, final MineParameters parameters) {
                if (lastPos.equals(parameters.pos)) {
                    return false;
                }
                lastPos = parameters.pos;
                return true;
            }
        });
    }

    public record MineParameters(BrainResourceRepository repository,
                                 BlockPos pos) implements MineBlockBrainNode.Arguments {
    }


    private BasicBrainNodes() {
    }
}
