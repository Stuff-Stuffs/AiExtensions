package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;

public class TickCountdownMemory implements TickingMemory {
    public static final Codec<TickCountdownMemory> CODEC = Codec.INT.xmap(TickCountdownMemory::new, TickCountdownMemory::ticksRemaining);
    private int remaining;

    public TickCountdownMemory(final int remaining) {
        this.remaining = Math.max(remaining, 0);
    }

    public void set(final int ticks) {
        remaining = Math.max(ticks, 0);
    }

    public int ticksRemaining() {
        return remaining;
    }

    @Override
    public void tick(final BrainContext<?> context) {
        remaining = Math.max(remaining - 1, 0);
    }
}
