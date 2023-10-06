package io.github.stuff_stuffs.aiex.common.impl.brain;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrain;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventType;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryEntry;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.Task;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import io.github.stuff_stuffs.aiex.common.api.entity.AbstractNpcEntity;
import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import io.github.stuff_stuffs.aiex.common.impl.brain.memory.MemoryEntryImpl;
import io.github.stuff_stuffs.aiex.common.impl.brain.resource.AbstractBrainResourcesImpl;
import io.github.stuff_stuffs.aiex.common.impl.brain.resource.BrainResourcesImpl;
import io.github.stuff_stuffs.aiex.common.impl.brain.resource.DebugBrainResourcesImpl;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.stream.Stream;

public class AiBrainImpl<T extends Entity> implements AiBrain<T>, AiBrainView.Events {
    private final BrainNode<T, Unit, Unit> rootNode;
    private final BrainConfig config;
    private final EventHandler handler;
    private final MemoriesImpl memories;
    private final TaskConfig<T> taskConfig;
    private final AbstractBrainResourcesImpl resources;
    private AiFakePlayer fakePlayer;
    private long age;
    private long randomifier;
    private boolean init = false;

    public AiBrainImpl(final BrainNode<T, Unit, Unit> node, final BrainConfig config, final MemoryConfig memoryConfig, final TaskConfig<T> taskConfig) {
        rootNode = node;
        this.config = config;
        handler = new EventHandler();
        memories = new MemoriesImpl(memoryConfig, this);
        this.taskConfig = taskConfig;
        resources = (FabricLoader.getInstance().isDevelopmentEnvironment() ? new DebugBrainResourcesImpl() : new BrainResourcesImpl());
    }

    @Override
    public long age() {
        return age;
    }

    @Override
    public long randomSeed() {
        return HashCommon.murmurHash3(HashCommon.murmurHash3(age) + randomifier++);
    }

    @Override
    public BrainConfig config() {
        return config;
    }

    @Override
    public Events events() {
        return this;
    }

    @Override
    public Memories memories() {
        return memories;
    }

    @Override
    public BrainResources resources() {
        return resources;
    }

    @Override
    public void tick(final T entity) {
        if (!(entity.getEntityWorld() instanceof ServerWorld)) {
            throw new IllegalStateException("Tried ticking Ai on client!");
        }
        age++;
        randomifier = 0;
        handler.tick(age);
        if (memories.has(io.github.stuff_stuffs.aiex.common.api.brain.memory.Memories.ITEM_ATTACK_COOLDOWN)) {
            final MemoryEntry<Integer> entry = memories.get(io.github.stuff_stuffs.aiex.common.api.brain.memory.Memories.ITEM_ATTACK_COOLDOWN);
            if (entry.get() > 0) {
                entry.set(entry.get() - 1);
            }
        }
        if (memories.has(io.github.stuff_stuffs.aiex.common.api.brain.memory.Memories.ITEM_USE_COOLDOWN)) {
            final MemoryEntry<Integer> entry = memories.get(io.github.stuff_stuffs.aiex.common.api.brain.memory.Memories.ITEM_USE_COOLDOWN);
            if (entry.get() > 0) {
                entry.set(entry.get() - 1);
            }
        }
        final BrainContext<T> brainContext = new BrainContext<>() {
            @Override
            public T entity() {
                return entity;
            }

            @Override
            public ServerWorld world() {
                return (ServerWorld) entity.getEntityWorld();
            }

            @Override
            public AiBrainView brain() {
                return AiBrainImpl.this;
            }

            @Override
            public <TR, P> Optional<Task<TR, T>> createTask(final TaskKey<TR, P> key, final P parameters) {
                if (!taskConfig.hasFactory(key)) {
                    return Optional.empty();
                }
                final Task<TR, T> task = taskConfig.getFactory(key).create(parameters);
                return Optional.ofNullable(task);
            }

            @Override
            public AiFakePlayer playerDelegate() {
                if (fakePlayer == null) {
                    throw new NullPointerException();
                }
                return fakePlayer;
            }

            @Override
            public boolean hasPlayerDelegate() {
                return fakePlayer != null;
            }
        };
        if (!init) {
            if (entity instanceof AbstractNpcEntity e) {
                fakePlayer = new AiFakePlayer((ServerWorld) entity.getEntityWorld(), e);
            }
            rootNode.init(brainContext);
            init = true;
        }
        rootNode.tick(brainContext, Unit.INSTANCE);
    }

    @Override
    public void writeNbt(final NbtCompound nbt) {
        nbt.putLong("age", age);
        final NbtList list = new NbtList();
        for (final EventEntry event : handler.allEvents) {
            final Optional<NbtElement> result = AiBrainEvent.CODEC.encodeStart(NbtOps.INSTANCE, event.event).result();
            if (result.isPresent()) {
                final NbtCompound wrapper = new NbtCompound();
                wrapper.putLong("timestamp", event.timestamp);
                wrapper.putLong("expiration", event.expiration);
                wrapper.put("data", result.get());
                list.add(wrapper);
            }
        }
        nbt.put("events", list);
        final NbtList memoryList = new NbtList();
        for (final MemoryEntryImpl<?> value : memories.map.values()) {
            final NbtCompound compound = new NbtCompound();
            value.writeNbt(compound);
            memoryList.add(compound);
        }
        nbt.put("memories", memoryList);
    }

    @Override
    public void readNbt(final NbtCompound nbt) {
        if (init) {
            rootNode.deinit(this);
            init = false;
        }
        age = nbt.getLong("age");
        final NbtList list = nbt.getList("events", NbtElement.COMPOUND_TYPE);
        handler.expirationQueue.clear();
        handler.allEvents.clear();
        handler.eventsByType.clear();
        for (final NbtElement element : list) {
            final NbtCompound wrapper = (NbtCompound) element;
            final Optional<AiBrainEvent> event = AiBrainEvent.CODEC.parse(NbtOps.INSTANCE, wrapper.get("data")).result();
            if (event.isPresent()) {
                handler.submit(new EventEntry(event.get(), wrapper.getLong("timestamp"), wrapper.getLong("expiration")));
            }
        }
        final NbtList memoriesList = nbt.getList("memories", NbtElement.COMPOUND_TYPE);
        for (final NbtElement element : memoriesList) {
            final Optional<? extends MemoryEntryImpl<?>> read = MemoryEntryImpl.read((NbtCompound) element, this);
            if (read.isPresent()) {
                final MemoryEntryImpl<?> entry = read.get();
                if (memories.map.containsKey(entry.type())) {
                    memories.map.put(entry.type(), entry);
                }
            }
        }
    }

    @Override
    public void remember(final AiBrainEvent event) {
        handler.submit(event, age);
    }

    @Override
    public boolean forget(final AiBrainEvent event) {
        return handler.forget(event);
    }

    @Override
    public List<AiBrainEvent> query(final long since) {
        final SortedSet<EventEntry> query = handler.query(since);
        if (query.isEmpty()) {
            return Collections.emptyList();
        }
        final List<AiBrainEvent> list = new ArrayList<>(query.size());
        for (final EventEntry entry : query) {
            list.add(entry.event);
        }
        return list;
    }

    @Override
    public Stream<AiBrainEvent> streamQuery(final long since) {
        return handler.query(since).stream().map(entry -> entry.event);
    }

    @Override
    public List<AiBrainEvent> queryReversed(final long since) {
        final SortedSet<EventEntry> query = handler.queryReversed(since);
        if (query.isEmpty()) {
            return Collections.emptyList();
        }
        final List<AiBrainEvent> list = new ArrayList<>(query.size());
        for (final EventEntry entry : query) {
            list.add(entry.event);
        }
        return list;
    }

    @Override
    public Stream<AiBrainEvent> streamQueryReversed(final long since) {
        return handler.queryReversed(since).stream().map(entry -> entry.event);
    }

    @Override
    public <T0 extends AiBrainEvent> List<T0> query(final AiBrainEventType<T0> type, final long since) {
        final SortedSet<EventEntry> query = handler.query(type, since);
        if (query.isEmpty()) {
            return Collections.emptyList();
        }
        final List<T0> list = new ArrayList<>(query.size());
        for (final EventEntry entry : query) {
            //noinspection unchecked
            list.add((T0) entry.event);
        }
        return list;
    }

    @Override
    public <T0 extends AiBrainEvent> Stream<T0> streamQuery(final AiBrainEventType<T0> type, final long since) {
        //noinspection unchecked
        return handler.query(type, since).stream().map(entry -> (T0) entry.event);
    }

    @Override
    public <T0 extends AiBrainEvent> List<T0> queryReversed(final AiBrainEventType<T0> type, final long since) {
        final SortedSet<EventEntry> query = handler.queryReversed(type, since);
        if (query.isEmpty()) {
            return Collections.emptyList();
        }
        final List<T0> list = new ArrayList<>(query.size());
        for (final EventEntry entry : query) {
            //noinspection unchecked
            list.add((T0) entry.event);
        }
        return list;
    }

    @Override
    public <T0 extends AiBrainEvent> Stream<T0> streamQueryReversed(final AiBrainEventType<T0> type, final long since) {
        //noinspection unchecked
        return handler.query(type, since).stream().map(entry -> (T0) entry.event);
    }

    protected static class EventHandler {
        private final PriorityQueue<EventEntry> expirationQueue;
        private final SortedSet<EventEntry> allEvents;
        private final SortedSet<EventEntry> allEventsReversed;
        private final Map<AiBrainEventType<?>, SortedSet<EventEntry>> eventsByType;
        private final Map<AiBrainEventType<?>, SortedSet<EventEntry>> eventsByTypeReversed;

        private EventHandler() {
            expirationQueue = new ObjectHeapPriorityQueue<>(EventEntry.EXPIRATION_COMPARATOR);
            allEvents = new ObjectAVLTreeSet<>(EventEntry.TIMESTAMP_COMPARATOR);
            allEventsReversed = new ObjectAVLTreeSet<>(EventEntry.TIMESTAMP_COMPARATOR.reversed());
            eventsByType = new Reference2ReferenceOpenHashMap<>();
            eventsByTypeReversed = new Reference2ReferenceOpenHashMap<>();
        }

        public void submit(final AiBrainEvent event, final long timestamp) {
            final EventEntry entry = new EventEntry(event, timestamp, timestamp + event.lifetime());
            submit(entry);
        }

        public void submit(final EventEntry entry) {
            expirationQueue.enqueue(entry);
            allEvents.add(entry);
            allEventsReversed.add(entry);
            eventsByType.computeIfAbsent(entry.event.type(), i -> new ObjectAVLTreeSet<>(EventEntry.TIMESTAMP_COMPARATOR)).add(entry);
            eventsByTypeReversed.computeIfAbsent(entry.event.type(), i -> new ObjectAVLTreeSet<>(EventEntry.TIMESTAMP_COMPARATOR.reversed())).add(entry);
        }

        public void tick(final long timestamp) {
            while (expirationQueue.first().expiration < timestamp) {
                final EventEntry entry = expirationQueue.dequeue();
                allEvents.remove(entry);
                allEventsReversed.remove(entry);
                final AiBrainEventType<?> type = entry.event.type();
                final SortedSet<EventEntry> events = eventsByType.get(type);
                if (events != null) {
                    events.remove(entry);
                    if (events.isEmpty()) {
                        eventsByType.remove(type);
                    }
                }
                final SortedSet<EventEntry> eventsRev = eventsByTypeReversed.get(type);
                if (eventsRev != null) {
                    eventsRev.remove(entry);
                    if (eventsRev.isEmpty()) {
                        eventsByTypeReversed.remove(type);
                    }
                }
            }
        }

        public SortedSet<EventEntry> query(final long since) {
            if (allEvents.isEmpty()) {
                return Collections.emptySortedSet();
            }
            final EventEntry entry = new EventEntry(null, since, 0);
            return allEvents.tailSet(entry);
        }

        public SortedSet<EventEntry> query(final AiBrainEventType<?> type, final long since) {
            final SortedSet<EventEntry> entries = eventsByType.get(type);
            if (entries == null || entries.isEmpty()) {
                return Collections.emptySortedSet();
            }
            final EventEntry entry = new EventEntry(null, since, 0);
            return entries.tailSet(entry);
        }

        public SortedSet<EventEntry> queryReversed(final long since) {
            if (allEventsReversed.isEmpty()) {
                return Collections.emptySortedSet();
            }
            final EventEntry entry = new EventEntry(null, since - 1, 0);
            return allEventsReversed.headSet(entry);
        }

        public SortedSet<EventEntry> queryReversed(final AiBrainEventType<?> type, final long since) {
            final SortedSet<EventEntry> entries = eventsByTypeReversed.get(type);
            if (entries == null || entries.isEmpty()) {
                return Collections.emptySortedSet();
            }
            final EventEntry entry = new EventEntry(null, since - 1, 0);
            return entries.headSet(entry);
        }

        public boolean forget(final AiBrainEvent event) {
            final EventEntry entry = new EventEntry(event, 0, 0);
            if (allEvents.remove(entry)) {
                allEventsReversed.remove(entry);
                eventsByType.getOrDefault(event.type(), Collections.emptySortedSet()).remove(entry);
                eventsByTypeReversed.getOrDefault(event.type(), Collections.emptySortedSet()).remove(entry);
                return true;
            }
            return false;
        }
    }

    private record EventEntry(AiBrainEvent event, long timestamp, long expiration) {
        public static final Comparator<EventEntry> TIMESTAMP_COMPARATOR = Comparator.comparingLong(EventEntry::timestamp);
        public static final Comparator<EventEntry> EXPIRATION_COMPARATOR = Comparator.comparingLong(EventEntry::expiration);

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EventEntry entry)) {
                return false;
            }

            return event.equals(entry.event);
        }

        @Override
        public int hashCode() {
            return event.hashCode();
        }
    }

    private static final class MemoriesImpl implements Memories {
        private final Map<Memory<?>, MemoryEntryImpl<?>> map;
        private final AiBrainImpl<?> brain;

        private MemoriesImpl(final MemoryConfig config, final AiBrainImpl<?> brain) {
            this.brain = brain;
            map = new Reference2ObjectOpenHashMap<>();
            for (final Memory<?> key : config.keys()) {
                setup(key, config);
            }
            for (final Memory<?> memory : map.keySet()) {
                for (final Memory<?> dep : memory.listeningTo()) {
                    map.get(dep).addListener(memory);
                }
                for (final Memory<?> optDep : memory.optionalListeningTo()) {
                    final MemoryEntryImpl<?> entry = map.get(optDep);
                    if (entry != null) {
                        entry.addListener(memory);
                    }
                }
            }
        }

        private <T> void setup(final Memory<T> memory, final MemoryConfig config) {
            final T value = config.defaultValue(memory);
            map.put(memory, new MemoryEntryImpl<>(memory, brain, value));
        }

        @Override
        public boolean has(final Memory<?> memory) {
            return map.containsKey(memory);
        }

        @Override
        public <T> MemoryEntry<T> get(final Memory<T> memory) {
            final MemoryEntry<?> entry = map.get(memory);
            if (entry == null) {
                throw new IllegalArgumentException();
            }
            //noinspection unchecked
            return (MemoryEntry<T>) entry;
        }
    }
}
