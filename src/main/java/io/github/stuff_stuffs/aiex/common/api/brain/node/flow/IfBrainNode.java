package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import net.fabricmc.fabric.api.util.TriState;

import java.util.function.BiPredicate;

public class IfBrainNode<C, R, FC> implements BrainNode<C, R, FC> {
    private final BrainNode<C, R, FC> trueBranch;
    private final BrainNode<C, R, FC> falseBranch;
    private final BiPredicate<BrainContext<C>, FC> predicate;
    private final boolean dynamic;
    private TriState prev;

    public IfBrainNode(final BrainNode<C, R, FC> trueBranch, final BrainNode<C, R, FC> falseBranch, final BiPredicate<BrainContext<C>, FC> predicate) {
        this(trueBranch, falseBranch, predicate, true);
    }

    public IfBrainNode(final BrainNode<C, R, FC> trueBranch, final BrainNode<C, R, FC> falseBranch, final BiPredicate<BrainContext<C>, FC> predicate, final boolean dynamic) {
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
        this.predicate = predicate;
        this.dynamic = dynamic;
    }

    @Override
    public void init(final BrainContext<C> context) {
        prev = TriState.DEFAULT;
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg) {
        final boolean res;
        if (dynamic) {
            res = predicate.test(context, arg);
            if (prev != TriState.DEFAULT) {
                if (res ^ prev.get()) {
                    if (res) {
                        falseBranch.deinit(context);
                        trueBranch.init(context);
                    } else {
                        trueBranch.deinit(context);
                        falseBranch.init(context);
                    }
                }
            } else {
                if (res) {
                    trueBranch.init(context);
                } else {
                    falseBranch.init(context);
                }
            }
            prev = TriState.of(res);
        } else {
            if (prev == TriState.DEFAULT) {
                res = predicate.test(context, arg);
                prev = TriState.of(res);
            } else {
                res = prev.get();
            }
        }
        return (res ? trueBranch : falseBranch).tick(context, arg);
    }

    @Override
    public void deinit(BrainContext<C> context) {
        if (prev == TriState.TRUE) {
            trueBranch.deinit(context);
        } else if (prev == TriState.FALSE) {
            falseBranch.deinit(context);
        }
        prev = TriState.DEFAULT;
    }
}
