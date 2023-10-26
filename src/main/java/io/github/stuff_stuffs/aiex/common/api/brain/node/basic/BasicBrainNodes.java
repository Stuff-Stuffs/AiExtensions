package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.FallthroughChainedBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.IfBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.util.AiExFunctionUtil;
import io.github.stuff_stuffs.aiex.common.api.util.InventorySlot;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public final class BasicBrainNodes {
    public static <T extends MobEntity> BrainNode<T, MineBlockBrainNode.Result, MineParameters> mine() {
        final var node = new TaskBrainNode<>(BasicTasks.SelectToolTask.KEY, (BiFunction<MineParameters, BrainContext<T>, BasicTasks.SelectToolTask.Parameters>) (repository, context) -> {
            final BlockState state = context.world().getBlockState(repository.pos);
            return () -> state;
        }, (repository, context) -> repository.repository).fallthroughChain(new EquipFirstBrainNode<T>(new InventorySlot(EquipmentSlot.MAINHAND)).adaptArg((context, result) -> {
            final TaskBrainNode.Result<BasicTasks.SelectToolTask.Result> res = result.getFirst();
            final BrainResourceRepository repository = result.getSecond().repository();
            if (!(res instanceof TaskBrainNode.Success<BasicTasks.SelectToolTask.Result> success)) {
                return createEmptyEquipArgs(context);
            }
            final BasicTasks.SelectToolTask.Result selectTaskResult = success.value();
            if (!(selectTaskResult instanceof BasicTasks.SelectToolTask.Success s)) {
                return createEmptyEquipArgs(context);
            }
            return new EquipFirstBrainNode.Arguments() {
                @Override
                public List<InventorySlot> bestToWorst() {
                    return s.bestToWorst();
                }

                @Override
                public BrainResourceRepository repository() {
                    return repository;
                }
            };
        }), Pair::of).cache().resetOnContext(AiExFunctionUtil.eagerOr(AiExFunctionUtil.watcher((context, parameters) -> parameters.pos.toImmutable(), false), AiExFunctionUtil.watcher((context, parameters) -> context.world().getBlockState(parameters.pos), false)));
        return new FallthroughChainedBrainNode<>(node, new IfBrainNode<>(new MineBlockBrainNode<T>().adaptArg(Optional::get), BrainNodes.constant(new MineBlockBrainNode.Error(MineBlockBrainNode.ErrorType.RESOURCE_ACQUISITION)), (context, opt) -> opt.isPresent()), (BiFunction<EquipFirstBrainNode.Result, MineParameters, Optional<MineParameters>>) (result, parameters) -> {
            if (result instanceof EquipFirstBrainNode.Error) {
                return Optional.empty();
            }
            return Optional.of(parameters);
        });
    }

    private static EquipFirstBrainNode.Arguments createEmptyEquipArgs(final BrainContext<?> context) {
        final BrainResourceRepository repository = BrainResourceRepository.buildEmpty(context.brain().resources());
        return new EquipFirstBrainNode.Arguments() {
            @Override
            public List<InventorySlot> bestToWorst() {
                return List.of();
            }

            @Override
            public BrainResourceRepository repository() {
                return repository;
            }
        };
    }

    public record MineParameters(BrainResourceRepository repository,
                                 BlockPos pos) implements MineBlockBrainNode.Arguments {
    }


    private BasicBrainNodes() {
    }
}
