package io.github.stuff_stuffs.aiex.common.api.brain.node;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.node.flow.TaskBrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BrainNodes {
    public static <C, FC> BrainNode<C, Unit, FC> empty() {
        return constant(Unit.INSTANCE);
    }

    public static <C, R, FC> BrainNode<C, R, FC> constant(final R constant) {
        return terminal((context, fc) -> constant);
    }

    public static <C, R, FC> BrainNode<C, R, FC> terminal(final BiFunction<BrainContext<C>, FC, R> func) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context, final SpannedLogger logger) {

            }

            @Override
            public R tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
                return func.apply(context, arg);
            }

            @Override
            public void deinit(final BrainContext<C> context, final SpannedLogger logger) {

            }
        };
    }

    public static <C, R, FC> BrainNode<C, R, FC> expectResult(final BrainNode<C, TaskBrainNode.Result<R>, FC> node, final Supplier<? extends RuntimeException> errorFactory) {
        return node.adaptResult(res -> {
            if (res instanceof TaskBrainNode.Failure<R>) {
                throw errorFactory.get();
            }
            return ((TaskBrainNode.Success<R>) res).value();
        });
    }

    public static <C, R, FC0, FC1> BrainNode<C, Optional<R>, FC0> flatMap(final BrainNode<C, Optional<FC1>, FC0> start, final BrainNode<C, Optional<R>, FC1> map) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context, final SpannedLogger logger) {
                try (final var child = logger.open("flatmapped")) {
                    try (final var log = child.open("from")) {
                        start.init(context, log);
                    }
                    try (final var log = child.open("to")) {
                        map.init(context, log);
                    }
                }
            }

            @Override
            public Optional<R> tick(final BrainContext<C> context, final FC0 arg, final SpannedLogger logger) {
                try (final var child = logger.open("flatmapped")) {
                    final Optional<FC1> opt;
                    try (final var log = child.open("from")) {
                        opt = start.tick(context, arg, log);
                    }
                    if (opt.isEmpty()) {
                        logger.debug("Empty!");
                        return Optional.empty();
                    }
                    try (final var log = child.open("to")) {
                        return map.tick(context, opt.get(), log);
                    }
                }
            }

            @Override
            public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
                try (final var child = logger.open("flatmapped")) {
                    try (final var log = child.open("from")) {
                        start.deinit(context, log);
                    }
                    try (final var log = child.open("to")) {
                        map.deinit(context, log);
                    }
                }
            }
        };
    }

    public static <C, R, FC> BrainNode<C, R, FC> supplyIfAbsent(final BrainNode<C, Optional<R>, FC> main, final BrainNode<C, R, FC> fallback) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context, final SpannedLogger logger) {
                try (final var child = logger.open("supplied")) {
                    try (final var log = child.open("main")) {
                        main.init(context, log);
                    }
                    try (final var log = child.open("fallback")) {
                        fallback.init(context, log);
                    }
                }
            }

            @Override
            public R tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
                try (final var child = logger.open("supplied")) {
                    final Optional<R> tick = main.tick(context, arg, child);
                    if (tick.isPresent()) {
                        child.debug("Result present");
                        return tick.get();
                    }
                    child.debug("Result absent, using fallback");
                    return fallback.tick(context, arg, child);
                }
            }

            @Override
            public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
                try (final var child = logger.open("supplied")) {
                    try (final var log = child.open("main")) {
                        main.deinit(context, log);
                    }
                    try (final var log = child.open("fallback")) {
                        fallback.deinit(context, log);
                    }
                }
            }
        };
    }

    public static <C, R, FC> BrainNode<C, Optional<R>, FC> or(final BrainNode<C, Optional<R>, FC> main, final BrainNode<C, Optional<R>, FC> fallback) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context, final SpannedLogger logger) {
                try (final var child = logger.open("or")) {
                    try (final var log = child.open("main")) {
                        main.init(context, log);
                    }
                    try (final var log = child.open("fallback")) {
                        fallback.init(context, log);
                    }
                }
            }

            @Override
            public Optional<R> tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
                try (final var l = logger.open("or")) {
                    final Optional<R> tick = main.tick(context, arg, l);
                    if (tick.isPresent()) {
                        l.debug("Result present");
                        return tick;
                    }
                    l.debug("Result absent, using fallback");
                    return fallback.tick(context, arg, logger);
                }
            }

            @Override
            public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
                try (final var child = logger.open("or")) {
                    try (final var log = child.open("main")) {
                        main.deinit(context, log);
                    }
                    try (final var log = child.open("fallback")) {
                        fallback.deinit(context, log);
                    }
                }
            }
        };
    }

    public static <C, R, FC> BrainNode<C, R, FC> orElse(final BrainNode<C, Optional<R>, FC> main, final BrainNode<C, R, FC> fallback) {
        return new BrainNode<>() {
            @Override
            public void init(final BrainContext<C> context, final SpannedLogger logger) {
                try (final var child = logger.open("orElse")) {
                    try (final var log = child.open("main")) {
                        main.init(context, log);
                    }
                    try (final var log = child.open("fallback")) {
                        fallback.init(context, log);
                    }
                }
            }

            @Override
            public R tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
                try (final var l = logger.open("orElse")) {
                    final Optional<R> tick = main.tick(context, arg, l);
                    if (tick.isPresent()) {
                        l.debug("Result present");
                        return tick.get();
                    }
                    l.debug("Result absent, using fallback");
                    return fallback.tick(context, arg, logger);
                }
            }

            @Override
            public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
                try (final var child = logger.open("orElse")) {
                    try (final var log = child.open("main")) {
                        main.deinit(context, log);
                    }
                    try (final var log = child.open("fallback")) {
                        fallback.deinit(context, log);
                    }
                }
            }
        };
    }

    public static <C, FC> BrainNode<C, FC, FC> forwardArg(final BrainNode<C, Unit, FC> node) {
        return node.contextCapture((arg, ret) -> arg);
    }

    private BrainNodes() {
    }
}
