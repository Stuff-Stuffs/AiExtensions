package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestReference;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryName;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.UnreachableAreaOfInterestSet;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
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
        return node.fallthroughChain(new IfBrainNode<>(new MineBlockBrainNode<T>().adaptArg(Optional::get), BrainNodes.constant(new MineBlockBrainNode.Error(MineBlockBrainNode.ErrorType.RESOURCE_ACQUISITION)), (context, opt) -> opt.isPresent()), (BiFunction<EquipFirstBrainNode.Result, MineParameters, Optional<MineParameters>>) (result, parameters) -> {
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

    public static <C, R, FC> BrainNode<C, R, FC> loadOrCreateMemory(final BiFunction<BrainContext<C>, FC, MemoryName<R>> nameExtractor, final BiFunction<BrainContext<C>, FC, R> creator) {
        return BrainNodes.orElse(
                new LoadMemoryNode<C, R, Pair<FC, MemoryName<R>>>(
                        arg -> Either.right(arg.getSecond())
                ).adaptResult(res -> res.map(Memory::get)),
                new NamedRememberingBrainNode<>(
                        (context, pair) -> pair.getSecond(),
                        (context, pair) -> creator.apply(context, pair.getFirst())
                )
        ).adaptArg(
                (context, arg) -> Pair.of(arg, nameExtractor.apply(context, arg))
        );
    }

    public static <C> BrainNode<C, Unit, AreaOfInterestReference<?>> rememberUnreachable(final MemoryName<UnreachableAreaOfInterestSet> name) {
        return BasicBrainNodes.<C, UnreachableAreaOfInterestSet, AreaOfInterestReference<?>>loadOrCreateMemory(
                (context, reference) -> name,
                (context, ref) -> new UnreachableAreaOfInterestSet(context.brain().config().get(BrainConfig.DEFAULT_UNREACHABLE_TIMEOUT))
        ).contextCapture(
                (BiFunction<AreaOfInterestReference<?>, UnreachableAreaOfInterestSet, Pair<AreaOfInterestReference<?>, UnreachableAreaOfInterestSet>>)
                        Pair::of
        ).chain(
                BrainNodes.terminal(
                        (context, pair) -> {
                            pair.getSecond().tried(pair.getFirst(), context);
                            return Unit.INSTANCE;
                        }
                )
        );
    }

    public static <C, A extends AreaOfInterest> BrainNode<C, Optional<AreaOfInterestReference<A>>, AreaOfInterestReference<A>> checkUnreachable(final MemoryName<UnreachableAreaOfInterestSet> name) {
        return new LoadMemoryNode<C, UnreachableAreaOfInterestSet, AreaOfInterestReference<A>>(name).contextCapture(
                (arg, opt) -> opt.map(memory -> Pair.of(arg, memory.get()))
        ).adaptResult(
                (context, pair) -> {
                    if (pair.isPresent()) {
                        final Pair<AreaOfInterestReference<A>, UnreachableAreaOfInterestSet> p = pair.get();
                        if (!p.getSecond().contains(p.getFirst())) {
                            return Optional.of(p.getFirst());
                        }
                    }
                    return Optional.empty();
                }
        );
    }

    public record MineParameters(BrainResourceRepository repository,
                                 BlockPos pos) implements MineBlockBrainNode.Arguments {
    }


    private BasicBrainNodes() {
    }
}
