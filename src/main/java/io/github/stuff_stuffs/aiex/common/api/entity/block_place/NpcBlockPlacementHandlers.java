package io.github.stuff_stuffs.aiex.common.api.entity.block_place;

import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.util.AfterRegistryFreezeEvent;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.entity.block_place.PillarNpcBlockPlacementHandler;
import io.github.stuff_stuffs.aiex.common.internal.entity.block_place.SimpleNpcBlockPlacementHandler;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;

import java.util.*;
import java.util.function.Predicate;

public final class NpcBlockPlacementHandlers {
    public static final Event<CollectionEvent> EVENT = EventFactory.createArrayBacked(CollectionEvent.class, events -> new CollectionEvent() {
        @Override
        public <T> Collection<NpcBlockPlacementHandler<? super T>> find(final Class<T> clazz) {
            final List<NpcBlockPlacementHandler<? super T>> list = new ArrayList<>();
            for (final CollectionEvent event : events) {
                list.addAll(event.find(clazz));
            }
            return list;
        }
    });
    public static final TagKey<Block> SIMPLE_PLACEABLE_TAG = TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("placeable/simple"));
    private static final Map<Class<?>, NpcBlockPlacementHandler<?>> MAP = new Reference2ReferenceOpenHashMap<>();
    private static boolean registryFrozen = false;

    public static void init() {
        AfterRegistryFreezeEvent.EVENT.register(() -> registryFrozen = true);
        EVENT.register(new CollectionEvent() {
            @Override
            public <T> Collection<NpcBlockPlacementHandler<? super T>> find(final Class<T> clazz) {
                if (LivingEntity.class.isAssignableFrom(clazz)) {
                    final Set<Item> singleState = new ReferenceOpenHashSet<>();
                    final Set<Item> pillars = new ReferenceOpenHashSet<>();
                    for (final Map.Entry<Block, Item> entry : BlockItem.BLOCK_ITEMS.entrySet()) {
                        final Block block = entry.getKey();
                        final Collection<Property<?>> properties = block.getStateManager().getProperties();
                        if (block.getDefaultState().isIn(SIMPLE_PLACEABLE_TAG)) {
                            singleState.add(entry.getValue());
                        }
                        //fallbacks
                        else if ((properties.isEmpty() || (properties.size() == 1 && properties.contains(Properties.WATERLOGGED)))) {
                            singleState.add(entry.getValue());
                        } else if (entry.getKey() instanceof PillarBlock && (properties.size() == 1 && properties.contains(Properties.AXIS))) {
                            pillars.add(entry.getValue());
                        }
                    }
                    return List.of(new SimpleNpcBlockPlacementHandler<>(Collections.unmodifiableSet(singleState)), new PillarNpcBlockPlacementHandler<>(pillars));
                }
                return Collections.emptySet();
            }
        });
    }

    public static <T> NpcBlockPlacementHandler<T> get(final Class<T> clazz) {
        if (!registryFrozen) {
            return new NpcBlockPlacementHandler<>() {
                @Override
                public boolean handle(final Item item, final BrainContext<? extends T> context, final BlockPos pos, final Predicate<BlockState> targetState) {
                    return false;
                }

                @Override
                public Set<Item> handles() {
                    return Collections.emptySet();
                }
            };
        }
        //noinspection unchecked
        return (NpcBlockPlacementHandler<T>) MAP.computeIfAbsent(clazz, NpcBlockPlacementHandlers::compute);
    }

    private static <T> NpcBlockPlacementHandler<?> compute(final Class<T> c) {
        final Collection<NpcBlockPlacementHandler<? super T>> handlers = EVENT.invoker().find(c);
        final Map<Item, List<NpcBlockPlacementHandler<? super T>>> map = new Reference2ReferenceOpenHashMap<>();
        for (final NpcBlockPlacementHandler<? super T> handler : handlers) {
            for (final Item item : handler.handles()) {
                map.computeIfAbsent(item, i -> new ArrayList<>()).add(handler);
            }
        }
        for (final List<NpcBlockPlacementHandler<? super T>> value : map.values()) {
            Collections.reverse(value);
        }
        return new NpcBlockPlacementHandler<T>() {
            @Override
            public boolean handle(final Item item, final BrainContext<? extends T> context, final BlockPos pos, final Predicate<BlockState> targetState) {
                final List<NpcBlockPlacementHandler<? super T>> list = map.get(item);
                for (final NpcBlockPlacementHandler<? super T> handler : list) {
                    final boolean result = handler.handle(item, context, pos, targetState);
                    if (result) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Set<Item> handles() {
                return Collections.unmodifiableSet(map.keySet());
            }
        };
    }

    public interface CollectionEvent {
        <T> Collection<NpcBlockPlacementHandler<? super T>> find(Class<T> clazz);
    }

    private NpcBlockPlacementHandlers() {
    }
}
