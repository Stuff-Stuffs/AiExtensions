package io.github.stuff_stuffs.aiex.common.api.brain.task.node.basic;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow.TaskBrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.AiExFunctionUtil;
import io.github.stuff_stuffs.aiex.common.api.util.InventorySlot;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public final class BasicBrainNodes {
    public static <C extends LivingEntity> BrainNode<C, Optional<BasicTasks.MineBlock.Result>, RepositoryArgument<BlockPos>> mine() {
        return BasicBrainNodes.<C>dynamicToolSelect().ifThenFallthrough(
                (context, pair) -> pair.getFirst() instanceof EquipFirstBrainNode.Success,
                BrainNodes.expectResult(
                        new TaskBrainNode<>(
                                BasicTasks.MineBlock.KEY,
                                (BiFunction<RepositoryArgument<BlockPos>, BrainContext<C>, BasicTasks.MineBlock.Parameters>)
                                        (argument, context) -> argument::arg,
                                (argument, context) -> argument.repository()
                        ).adaptArg(
                                (BiFunction<BrainContext<C>, Pair<EquipFirstBrainNode.Result, RepositoryArgument<BlockPos>>, RepositoryArgument<BlockPos>>)
                                        (context, pair) -> pair.getSecond()
                        ),
                        RuntimeException::new
                ).adaptResult(Optional::of),
                BrainNodes.constant(Optional.empty()),
                Pair::of
        );
    }

    public static <C extends LivingEntity> BrainNode<C, BasicTasks.MineBlock.Result, RepositoryArgument<BlockPos>> forceMine() {
        return BasicBrainNodes.<C>dynamicToolSelect().fallthroughChain(
                BrainNodes.expectResult(
                        new TaskBrainNode<>(
                                BasicTasks.MineBlock.KEY,
                                (argument, context) -> argument::arg,
                                (argument, context) -> argument.repository()
                        ),
                        RuntimeException::new
                ),
                (first, second) -> second
        );
    }

    public static <C extends LivingEntity> BrainNode<C, EquipFirstBrainNode.Result, RepositoryArgument<BlockPos>> dynamicToolSelect() {
        return BrainNodes.expectResult(new TaskBrainNode<>(BasicTasks.SelectTool.KEY,
                (argument, context) -> argument::getFirst,
                (BiFunction<Pair<BlockState, BrainResourceRepository>, BrainContext<C>, BrainResourceRepository>) (pair, context) -> pair.getSecond()
        ), RuntimeException::new).fallthroughChain(
                new EquipFirstBrainNode<>(new InventorySlot(EquipmentSlot.MAINHAND)),
                (result, pair) -> {
                    final List<InventorySlot> bestToWorst;
                    if (result instanceof BasicTasks.SelectTool.NoneAvailableError) {
                        bestToWorst = List.of();
                    } else if (result instanceof final BasicTasks.SelectTool.Success success) {
                        bestToWorst = success.bestToWorst();
                    } else {
                        throw new AssertionError();
                    }
                    final BrainResourceRepository repository = pair.getSecond();
                    return new EquipFirstBrainNode.Arguments() {
                        @Override
                        public List<InventorySlot> bestToWorst() {
                            return bestToWorst;
                        }

                        @Override
                        public BrainResourceRepository repository() {
                            return repository;
                        }
                    };
                }
        ).cache().resetOnContext(
                AiExFunctionUtil.watcher((context, pair) -> pair.getFirst(), false)
        ).adaptArg(
                (context, argument) -> Pair.of(context.world().getBlockState(argument.arg()), argument.repository())
        );
    }

    public interface RepositoryArgument<T> {
        T arg();

        BrainResourceRepository repository();
    }

    private BasicBrainNodes() {
    }
}
