package io.github.stuff_stuffs.aiex.common.api.brain.memory;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestReference;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.registry.Registry;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class BasicMemoryTypes {
    public static final MemoryType<Integer> INT_MEMORY_TYPE = () -> Codec.INT;
    public static final MemoryType<BlockPos> BLOCK_POS_MEMORY_TYPE = () -> BlockPos.CODEC;
    public static final MemoryType<AreaOfInterestReference<?>> GENERIC_AREA_OF_INTEREST_MEMORY_TYPE = () -> AreaOfInterestReference.CODEC;
    public static final MemoryType<UUID> UUID_MEMORY_TYPE = () -> Uuids.STRING_CODEC;

    public static <T extends AreaOfInterest> MemoryType<AreaOfInterestReference<T>> areaOfInterest(final AreaOfInterestType<T> type) {
        final Codec<AreaOfInterestReference<T>> codec = AreaOfInterestReference.typeSpecificCodec(type);
        return () -> codec;
    }

    public static <T> MemoryType<MemoryReference<T>> erasingPointer(final MemoryType<T> type) {
        final Codec<MemoryReference<T>> codec = MemoryReference.codec(type);
        return new MemoryType<>() {
            @Override
            public Codec<MemoryReference<T>> codec() {
                return codec;
            }

            @Override
            public Collection<? extends MemoryReference<?>> insideOf(final MemoryReference<T> value) {
                return Collections.singleton(value);
            }

            @Override
            public Optional<MemoryReference<T>> forgetContained(final MemoryReference<?> other, final MemoryReference<T> currentValue) {
                return Optional.empty();
            }
        };
    }

    public static <T> MemoryType<Set<MemoryReference<T>>> evictingReferenceSet(final MemoryType<T> type) {
        final Codec<Set<MemoryReference<T>>> codec = MemoryReference.codec(type).listOf().xmap(Set::copyOf, List::copyOf);
        return new MemoryType<>() {
            @Override
            public Codec<Set<MemoryReference<T>>> codec() {
                return codec;
            }

            @Override
            public Collection<? extends MemoryReference<?>> insideOf(final Set<MemoryReference<T>> value) {
                return value;
            }

            @Override
            public Optional<Set<MemoryReference<T>>> forgetContained(final MemoryReference<?> other, final Set<MemoryReference<T>> currentValue) {
                if (currentValue.size() == 1 || currentValue.isEmpty()) {
                    return Optional.empty();
                }
                final List<MemoryReference<T>> references = new ArrayList<>(currentValue.size() - 1);
                for (final MemoryReference<T> reference : currentValue) {
                    if (reference != other) {
                        references.add(reference);
                    }
                }
                return Optional.of(Set.copyOf(references));
            }
        };
    }

    public static void init() {
        Registry.register(MemoryType.REGISTRY, AiExCommon.id("int"), INT_MEMORY_TYPE);
        Registry.register(MemoryType.REGISTRY, AiExCommon.id("block_pos"), BLOCK_POS_MEMORY_TYPE);
        Registry.register(MemoryType.REGISTRY, AiExCommon.id("generic_aoi"), GENERIC_AREA_OF_INTEREST_MEMORY_TYPE);
        Registry.register(MemoryType.REGISTRY, AiExCommon.id("uuid"), UUID_MEMORY_TYPE);
    }

    private BasicMemoryTypes() {
    }
}
