package io.github.stuff_stuffs.aiex.common.api.brain.util.target;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.node.basic.target.BasicSingleTargetingBrainNode;

public interface Targeter<C, R, FC> {
    R find(BrainContext<C> context, FC arg);

    void reset();

    default BrainNode<C, R, FC> asBrainNode() {
        return new BasicSingleTargetingBrainNode<>(this);
    }
}
