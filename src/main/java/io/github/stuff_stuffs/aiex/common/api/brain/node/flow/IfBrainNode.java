package io.github.stuff_stuffs.aiex.common.api.brain.node.flow;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
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
    public void init(final BrainContext<C> context, final SpannedLogger logger) {
        prev = TriState.DEFAULT;
        try (final var child = dynamic ? logger.open("DynamicIf") : logger.open("If")) {
        }
    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = dynamic ? logger.open("DynamicIf") : logger.open("If")) {
            final boolean res;
            if (dynamic) {
                res = predicate.test(context, arg);
                if (prev != TriState.DEFAULT && res != prev.get()) {
                    child.debug("Resetting, new value=" + res);
                    if (res ^ prev.get()) {
                        if (res) {
                            child.debug("Setting up true branch");
                            trueBranch.init(context, child);
                            child.debug("Tearing down false branch");
                            falseBranch.deinit(context, child);
                        } else {
                            child.debug("Setting up false branch");
                            falseBranch.init(context, child);
                            child.debug("Tearing down true branch");
                            trueBranch.deinit(context, child);
                        }
                    }
                } else if (prev == TriState.DEFAULT) {
                    if (res) {
                        child.debug("Setting up true branch");
                        trueBranch.init(context, child);
                    } else {
                        child.debug("Setting up false branch");
                        falseBranch.init(context, child);
                    }
                }
                prev = TriState.of(res);
            } else {
                if (prev == TriState.DEFAULT) {
                    res = predicate.test(context, arg);
                    prev = TriState.of(res);
                    child.debug("Setting value-" + res);
                    if (res) {
                        trueBranch.init(context, child);
                    } else {
                        falseBranch.init(context, child);
                    }
                } else {
                    res = prev.get();
                }
            }
            if (res) {
                child.debug("Going true branch!");
                return trueBranch.tick(context, arg, child);
            } else {
                child.debug("Going false branch!");
                return falseBranch.tick(context, arg, child);
            }
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        try (final var child = dynamic ? logger.open("DynamicIf") : logger.open("If")) {
            if (prev == TriState.TRUE) {
                try (final var tLogger = child.open("trueBranch")) {
                    trueBranch.deinit(context, tLogger);
                }
            } else if (prev == TriState.FALSE) {
                try (final var fLogger = child.open("falseBranch")) {
                    falseBranch.deinit(context, fLogger);
                }
            }
            prev = TriState.DEFAULT;
        }
    }
}
