package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestReference;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongPredicate;

public class UnreachableAreaOfInterestSet implements TickingMemory {
    public static final Codec<UnreachableAreaOfInterestSet> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("timeout").forGetter(set -> set.timeout),
            Codec.pair(AreaOfInterestReference.CODEC, Codec.LONG).listOf().xmap(pairs -> {
                Object2LongLinkedOpenHashMap<AreaOfInterestReference<?>> map = new Object2LongLinkedOpenHashMap<>(pairs.size(), Hash.DEFAULT_LOAD_FACTOR);
                for (Pair<AreaOfInterestReference<?>, Long> pair : pairs) {
                    map.put(pair.getFirst(), (long) pair.getSecond());
                }
                return map;
            }, map -> {
                List<Pair<AreaOfInterestReference<?>, Long>> list = new ArrayList<>(map.size());
                for (Object2LongMap.Entry<AreaOfInterestReference<?>> entry : map.object2LongEntrySet()) {
                    list.add(Pair.of(entry.getKey(), entry.getLongValue()));
                }
                return list;
            }).fieldOf("lastTried").forGetter(set -> set.lastTriedTick)
    ).apply(instance, UnreachableAreaOfInterestSet::new));
    private final long timeout;
    private final Object2LongLinkedOpenHashMap<AreaOfInterestReference<?>> lastTriedTick;

    public UnreachableAreaOfInterestSet(final long timeout) {
        this.timeout = timeout;
        lastTriedTick = new Object2LongLinkedOpenHashMap<>();
    }

    private UnreachableAreaOfInterestSet(final long timeout, final Object2LongLinkedOpenHashMap<AreaOfInterestReference<?>> lastTriedTick) {
        this.timeout = timeout;
        this.lastTriedTick = lastTriedTick;
    }

    public void tried(final AreaOfInterestReference<?> ref, final BrainContext<?> context) {
        lastTriedTick.put(ref, context.brain().age());
    }

    public boolean contains(final AreaOfInterestReference<?> ref) {
        return lastTriedTick.containsKey(ref);
    }

    @Override
    public void tick(final BrainContext<?> context) {
        lastTriedTick.values().removeIf((LongPredicate) value -> value + timeout < context.brain().age());
    }
}
