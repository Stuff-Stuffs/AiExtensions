package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestEntry;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestReference;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryName;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.UnreachableAreaOfInterestSet;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNodes;
import io.github.stuff_stuffs.aiex.common.api.brain.node.basic.memory.NamedMemoryLoadBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.basic.memory.NamedRememberingBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.ForwardingBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.LoopBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskBrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResourceRepository;
import io.github.stuff_stuffs.aiex.common.api.brain.task.BasicTasks;
import io.github.stuff_stuffs.aiex.common.api.entity.pathing.EntityPather;
import io.github.stuff_stuffs.aiex.common.api.util.AiExFunctionUtil;
import io.github.stuff_stuffs.aiex.common.api.util.InventorySlot;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

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
                    } else if (result instanceof BasicTasks.SelectTool.Success success) {
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

    public static <C, R, FC> BrainNode<C, R, FC> loadOrCreateMemory(final BiFunction<BrainContext<C>, FC, MemoryName<R>> nameExtractor, final BiFunction<BrainContext<C>, FC, R> creator) {
        return BrainNodes.orElse(
                new NamedMemoryLoadBrainNode<C, R, Pair<FC, MemoryName<R>>>(
                        Pair::getSecond
                ).adaptResult(res -> res.map(Memory::get)),
                new NamedRememberingBrainNode<>(
                        Pair::getSecond,
                        (context, pair) -> creator.apply(context, pair.getFirst())
                )
        ).adaptArg(
                (context, arg) -> Pair.of(arg, nameExtractor.apply(context, arg))
        );
    }

    public static <C, T> BrainNode<C, Optional<Pair<BasicTasks.Walk.Result, T>>, RepositoryArgument<Collection<T>>> walkToFirst(final MemoryName<UnreachableAreaOfInterestSet> unreachableName, final Function<T, AreaOfInterestEntry<?>> extractor, final BiFunction<T, EntityPather.Target, BasicTasks.Walk.Parameters> parameterFactory) {
        final var selectFirstNode = LoopBrainNode.<C, T>first(
                BasicBrainNodes.<C>checkUnreachable(
                        unreachableName
                ).adaptResult(
                        Optional::isPresent
                ).adaptArg(
                        arg -> extractor.apply(arg).reference()
                ),
                1000
        ).adaptArg(
                (Function<RepositoryArgument<Collection<T>>, Iterable<T>>) RepositoryArgument::arg
        ).cache((arg, res) -> Pair.of(arg.repository(), res));
        final var walkNode = BrainNodes.expectResult(new TaskBrainNode<>(BasicTasks.Walk.KEY,
                (argument, context) -> {
                    final T val = argument.arg();
                    final EntityPather.Target target = EntityPather.MetricTarget.ofBounds(extractor.apply(val).bounds());
                    return parameterFactory.apply(val, target);
                },
                (BiFunction<RepositoryArgument<T>, BrainContext<C>, BrainResourceRepository>)
                        (argument, context) -> argument.repository()
        ), RuntimeException::new);
        return selectFirstNode.<Optional<Pair<BasicTasks.Walk.Result, T>>>ifThen(
                (context, pair) -> pair.getSecond().isPresent(),
                walkNode.<Pair<BrainResourceRepository, Optional<T>>>adaptArg(
                        pair -> new RepositoryArgument<>() {
                            @Override
                            public T arg() {
                                return pair.getSecond().get();
                            }

                            @Override
                            public BrainResourceRepository repository() {
                                return pair.getFirst();
                            }
                        }
                ).contextCapture((repo, res) -> Pair.of(repo.getSecond().get(), res)).ifThen(
                        (context, pair) -> pair.getSecond() == BasicTasks.Walk.Result.CANNOT_REACH,
                        BasicBrainNodes.<C>rememberUnreachable(unreachableName)
                                .<Pair<T, BasicTasks.Walk.Result>>adaptArg(
                                        pair -> extractor.apply(pair.getFirst()).reference()
                                ).contextCapture(
                                        (arg, unit) -> arg
                                ),
                        new ForwardingBrainNode<>()
                ).adaptResult(
                        res -> Optional.of(res.getSecond())
                ).contextCapture(
                        (pair, opt) -> opt.map(res -> Pair.of(res, pair.getSecond().get()))
                ),
                BrainNodes.constant(Optional.empty())
        ).resetOnResult(
                res -> res.isPresent() && res.get().getFirst() == BasicTasks.Walk.Result.CANNOT_REACH
        ).adaptResult(
                res -> res.isPresent() && res.get().getFirst() == BasicTasks.Walk.Result.CANNOT_REACH ?
                        Optional.of(
                                Pair.of(BasicTasks.Walk.Result.CONTINUE, res.get().getSecond())
                        )
                        : res
        );
    }

    public static <C> BrainNode<C, Unit, AreaOfInterestReference<?>> rememberUnreachable(final MemoryName<UnreachableAreaOfInterestSet> name) {
        return BasicBrainNodes.<C, UnreachableAreaOfInterestSet, AreaOfInterestReference<?>>loadOrCreateMemory(
                (context, reference) -> name,
                (context, ref) -> new UnreachableAreaOfInterestSet(context.brain().config().get(BrainConfig.DEFAULT_UNREACHABLE_TIMEOUT))
        ).fallthroughChain(
                BrainNodes.terminal(
                        (context, pair) -> {
                            pair.getFirst().tried(pair.getSecond(), context);
                            return Unit.INSTANCE;
                        }
                ),
                Pair::of
        );
    }

    public static <C> BrainNode<C, Optional<AreaOfInterestReference<?>>, AreaOfInterestReference<?>> checkUnreachable(final MemoryName<UnreachableAreaOfInterestSet> name) {
        return new NamedMemoryLoadBrainNode<C, UnreachableAreaOfInterestSet, AreaOfInterestReference<?>>(name).contextCapture(
                (arg, opt) -> opt.map(memory -> Pair.<AreaOfInterestReference<?>, UnreachableAreaOfInterestSet>of(arg, memory.get()))
        ).adaptResult(
                (context, pair) -> {
                    if (pair.isPresent()) {
                        final Pair<AreaOfInterestReference<?>, UnreachableAreaOfInterestSet> p = pair.get();
                        if (!p.getSecond().contains(p.getFirst())) {
                            return Optional.of(p.getFirst());
                        }
                    }
                    return Optional.empty();
                }
        );
    }

    public static <C, A extends AreaOfInterest> BrainNode<C, Optional<AreaOfInterestReference<A>>, AreaOfInterestReference<A>> checkUnreachableTyped(final MemoryName<UnreachableAreaOfInterestSet> name) {
        return new NamedMemoryLoadBrainNode<C, UnreachableAreaOfInterestSet, AreaOfInterestReference<A>>(name).contextCapture(
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

    public interface RepositoryArgument<T> {
        T arg();

        BrainResourceRepository repository();
    }

    private BasicBrainNodes() {
    }
}
