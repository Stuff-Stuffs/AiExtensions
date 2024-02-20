package io.github.stuff_stuffs.aiex.common.api.brain.task.node.flow;

import com.mojang.datafixers.util.Pair;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.task.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public final class SelectorBrainNode<C, R, FC> implements BrainNode<C, R, FC> {
    private final List<Pair<BiPredicate<BrainContext<C>, FC>, BrainNode<C, R, FC>>> entries;
    private int lastIndex = -1;

    private SelectorBrainNode(final List<Pair<BiPredicate<BrainContext<C>, FC>, BrainNode<C, R, FC>>> entries) {
        this.entries = entries;
    }

    @Override
    public void init(final BrainContext<C> context, final SpannedLogger logger) {

    }

    @Override
    public R tick(final BrainContext<C> context, final FC arg, final SpannedLogger logger) {
        try (final var child = logger.open("Dispatch")) {
            int selected = -1;
            int i = 0;
            for (final Pair<BiPredicate<BrainContext<C>, FC>, BrainNode<C, R, FC>> entry : entries) {
                if (entry.getFirst().test(context, arg)) {
                    selected = i;
                    break;
                }
                i++;
            }
            if (selected == -1) {
                throw new IllegalStateException();
            }
            if (lastIndex != selected && lastIndex != -1) {
                child.debug("De-initialising child: " + lastIndex);
                entries.get(lastIndex).getSecond().deinit(context, child);
            }
            if (lastIndex != selected) {
                child.debug("Initialising child: " + selected);
                entries.get(selected).getSecond().init(context, child);
            }
            lastIndex = selected;
            return entries.get(lastIndex).getSecond().tick(context, arg, child);
        }
    }

    @Override
    public void deinit(final BrainContext<C> context, final SpannedLogger logger) {
        if (lastIndex != -1) {
            try (final var child = logger.open("Dispatch")) {
                child.debug("De-initialising child: " + lastIndex);
                entries.get(lastIndex).getSecond().deinit(context, child);
                lastIndex = -1;
            }
        }
    }

    public static <C, R, FC> Builder<C, R, FC> builder() {
        return new Builder<>();
    }

    public static final class Builder<C, R, FC> {
        private final List<Pair<BiPredicate<BrainContext<C>, FC>, BrainNode<C, R, FC>>> entries = new ArrayList<>();

        private Builder() {
        }

        public Builder<C, R, FC> add(final BiPredicate<BrainContext<C>, FC> predicate, final BrainNode<C, R, FC> entry) {
            entries.add(Pair.of(predicate, entry));
            return this;
        }

        public BrainNode<C, R, FC> build() {
            return new SelectorBrainNode<>(List.copyOf(entries));
        }
    }
}
