package io.github.stuff_stuffs.aiex.common.api.brain.node.basic.target;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class SinglePoiTargetingBrainNode<C, FC> extends AbstractSingleTargetingBrainNode<C, BlockPos, FC> {
    private final Predicate<RegistryEntry<PointOfInterestType>> predicate;
    private final BiFunction<BrainContext<C>, FC, Predicate<PointOfInterest>> filter;
    private final Function<C, BlockPos> posGetter;
    private final int range;

    public SinglePoiTargetingBrainNode(final Predicate<RegistryEntry<PointOfInterestType>> predicate, final BiFunction<BrainContext<C>, FC, Predicate<PointOfInterest>> filter, final Function<C, BlockPos> posGetter, final int range, final boolean dynamic) {
        super(dynamic);
        this.predicate = predicate;
        this.filter = filter;
        this.posGetter = posGetter;
        this.range = range;
    }

    @Override
    protected Optional<BlockPos> query(final BrainContext<C> context, final FC arg) {
        return context.world().getPointOfInterestStorage().getInCircle(predicate, posGetter.apply(context.entity()), range, PointOfInterestStorage.OccupationStatus.ANY).filter(filter.apply(context, arg)).findFirst().map(PointOfInterest::getPos);
    }
}
