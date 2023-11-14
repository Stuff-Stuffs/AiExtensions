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

public final class BasicMemories {
    public static final MemoryType<Integer> INT_MEMORY_TYPE = () -> Codec.INT;
    public static final MemoryType<BlockPos> BLOCK_POS_TYPE = () -> BlockPos.CODEC;
    public static final MemoryType<AreaOfInterestReference<?>> GENERIC_AREA_OF_INTEREST_TYPE = () -> AreaOfInterestReference.CODEC;
    public static final MemoryType<UUID> UUID_MEMORY_TYPE = () -> Uuids.STRING_CODEC;
    public static final MemoryType<UnreachableAreaOfInterestSet> UNREACHABLE_AOI_MEMORY_TYPE = () -> UnreachableAreaOfInterestSet.CODEC;
    public static final MemoryType<TickCountdownMemory> TICK_COUNTDOWN_TYPE = () -> TickCountdownMemory.CODEC;
    public static final MemoryName<UnreachableAreaOfInterestSet> BASIC_UNREACHABLE_AREA_NAME = () -> UNREACHABLE_AOI_MEMORY_TYPE;
    public static final MemoryName<TickCountdownMemory> USE_ITEM_COOLDOWN_TICKS = () -> TICK_COUNTDOWN_TYPE;

    public static <T extends AreaOfInterest> MemoryType<AreaOfInterestReference<T>> areaOfInterest(final AreaOfInterestType<T> type) {
        final Codec<AreaOfInterestReference<T>> codec = AreaOfInterestReference.typeSpecificCodec(type);
        return () -> codec;
    }

    public static <T> MemoryType<MemoryReference<T>> erasingReference(final MemoryType<T> type) {
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
                if (currentValue.equals(other)) {
                    return Optional.empty();
                }
                return Optional.of(currentValue);
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
        Registry.register(MemoryType.REGISTRY, AiExCommon.id("block_pos"), BLOCK_POS_TYPE);
        Registry.register(MemoryType.REGISTRY, AiExCommon.id("generic_aoi"), GENERIC_AREA_OF_INTEREST_TYPE);
        Registry.register(MemoryType.REGISTRY, AiExCommon.id("uuid"), UUID_MEMORY_TYPE);
        Registry.register(MemoryType.REGISTRY, AiExCommon.id("unreachable_aoi"), UNREACHABLE_AOI_MEMORY_TYPE);
        Registry.register(MemoryType.REGISTRY, AiExCommon.id("tick_countdown"), TICK_COUNTDOWN_TYPE);
        Registry.register(MemoryName.REGISTRY, AiExCommon.id("basic_unreachable"), BASIC_UNREACHABLE_AREA_NAME);
        Registry.register(MemoryName.REGISTRY, AiExCommon.id("use_item_cooldown_ticks"), USE_ITEM_COOLDOWN_TICKS);
    }

    private BasicMemories() {
    }
}
