package io.github.stuff_stuffs.aiex.common.api.brain.node.basic;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.Set;
import java.util.function.BiFunction;

public class PoiTargetingBrainNode<C,  FC> implements BrainNode<C, Set<PointOfInterest>, FC> {
    private final PointOfInterestType type;

    public PoiTargetingBrainNode(PointOfInterestType type, BiFunction<BrainContext<C>, FC, >) {
        this.type = type;
    }

    @Override
    public void init(BrainContext<C> context) {

    }

    @Override
    public Set<PointOfInterest> tick(BrainContext<C> context, FC arg) {
        context.world().getPointOfInterestStorage().getInSquare()
        return null;
    }

    @Override
    public void deinit() {

    }
}
