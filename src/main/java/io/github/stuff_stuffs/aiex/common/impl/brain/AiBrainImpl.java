package io.github.stuff_stuffs.aiex.common.impl.brain;

import com.mojang.datafixers.util.Unit;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrain;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.BrainContext;
import io.github.stuff_stuffs.aiex.common.api.brain.config.BrainConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEvent;
import io.github.stuff_stuffs.aiex.common.api.brain.event.AiBrainEventType;
import io.github.stuff_stuffs.aiex.common.api.brain.node.BrainNode;
import io.github.stuff_stuffs.aiex.common.api.brain.resource.BrainResources;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskConfig;
import io.github.stuff_stuffs.aiex.common.api.brain.task.TaskKey;
import io.github.stuff_stuffs.aiex.common.api.entity.AiFakePlayer;
import io.github.stuff_stuffs.aiex.common.api.util.SpannedLogger;
import io.github.stuff_stuffs.aiex.common.api.util.StringTemplate;
import io.github.stuff_stuffs.aiex.common.impl.brain.memory.MemoriesImpl;
import io.github.stuff_stuffs.aiex.common.impl.brain.resource.AbstractBrainResourcesImpl;
import io.github.stuff_stuffs.aiex.common.impl.brain.resource.BrainResourcesImpl;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.stream.Stream;

public class AiBrainImpl<T extends Entity> implements AiBrain, AiBrainView.Events {
    private static final StringTemplate MISSING_FACTORY_TEMPLATE = StringTemplate.create("Missing task factory for task {}!");
    private final T entity;
    private final BrainNode<T, Unit, Unit> rootNode;
    private final BrainConfig config;
    private final EventHandler handler;
    private final MemoriesImpl memories;
    private final TaskConfig<T> taskConfig;
    private final AbstractBrainResourcesImpl resources;
    private long seed;
    private final SpannedLogger logger;
    private final AiFakePlayer fakePlayer;
    private long age;
    private boolean init = false;

    public AiBrainImpl(final T entity, final BrainNode<T, Unit, Unit> node, final BrainConfig config, final TaskConfig<T> taskConfig, final long seed, final SpannedLogger logger) {
        this.entity = entity;
        rootNode = node;
        this.config = config;
        this.seed = seed;
        this.logger = logger;
        handler = new EventHandler();
        memories = new MemoriesImpl();
        this.taskConfig = taskConfig;
        resources = new BrainResourcesImpl();
        fakePlayer = AiFakePlayer.create(entity, (ServerWorld) entity.getEntityWorld());
    }

    public SpannedLogger logger() {
        return logger;
    }

    @Override
    public long age() {
        return age;
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

    private BrainContext<T> createContext() {
        return new BrainContext<>() {
            private long randomifier = 0;

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
            public <TR, P, FC> Optional<BrainNode<T, TR, FC>> createTask(final TaskKey<TR, P, FC> key, final P parameters, final SpannedLogger logger) {
                if (!taskConfig.hasFactory(key)) {
                    try (final var l = logger.open("BrainTaskFactory")) {
                        l.warning(MISSING_FACTORY_TEMPLATE, TaskKey.REGISTRY.getId(key));
                    }
                    return Optional.empty();
                }
                final BrainNode<T, TR, FC> task = taskConfig.getFactory(key).create(parameters);
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

            @Override
            public long randomSeed() {
                return HashCommon.murmurHash3(seed + age ^ HashCommon.murmurHash3(randomifier++));
            }
        };
    }

    @Override
    public ServerPlayerEntity fakePlayerDelegate() {
        if (fakePlayer == null) {
            throw new NullPointerException();
        }
        return fakePlayer;
    }

    @Override
    public boolean hasFakePlayerDelegate() {
        return fakePlayer != null;
    }

    @Override
    public void tick() {
        if (!(entity.getEntityWorld() instanceof ServerWorld)) {
            throw new IllegalStateException("Tried ticking Ai on client!");
        }
        age++;
        handler.tick(age);
        final BrainContext<T> context = createContext();
        try (final SpannedLogger child = logger.open("root")) {
            if (!init) {
                rootNode.init(context, child);
                init = true;
            }
            rootNode.tick(context, Unit.INSTANCE, child);
        }
        memories.tick(context);
    }

    @Override
    public void unload() {
        if (init) {
            final BrainContext<T> context = createContext();
            init = false;
            try (final SpannedLogger child = logger.open("root")) {
                rootNode.deinit(context, child);
            }
        }
    }

    @Override
    public void writeNbt(final NbtCompound nbt) {
        nbt.putLong("age", age);
        nbt.putLong("seed", seed);
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
        nbt.put("memories", memories.writeNbt());
    }

    @Override
    public void readNbt(final NbtCompound nbt) {
        if (init) {
            try (final SpannedLogger child = logger.open("root")) {
                rootNode.deinit(createContext(), child);
            }
            logger.debug("Reloading brain!");
            init = false;
        }
        age = nbt.getLong("age");
        seed = nbt.getLong("seed");
        final NbtList list = nbt.getList("events", NbtElement.COMPOUND_TYPE);
        resources.clear();
        handler.clear();
        for (final NbtElement element : list) {
            final NbtCompound wrapper = (NbtCompound) element;
            final Optional<AiBrainEvent> event = AiBrainEvent.CODEC.parse(NbtOps.INSTANCE, wrapper.get("data")).result();
            if (event.isPresent()) {
                handler.submit(new EventEntry(event.get(), wrapper.getLong("timestamp"), wrapper.getLong("expiration")));
            }
        }
        memories.readNbt(nbt.getCompound("memories"));
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
    public List<AiBrainEvent> query(final long since, final boolean reversed) {
        final List<EventEntry> entries = (reversed ? handler.queryReversed(since) : handler.query(since));
        final List<AiBrainEvent> events = new ArrayList<>(entries.size());
        for (final EventEntry entry : entries) {
            events.add(entry.event);
        }
        return events;
    }

    @Override
    public Stream<AiBrainEvent> streamQuery(final long since, final boolean reversed) {
        return query(since, reversed).stream();
    }

    @Override
    public <T0 extends AiBrainEvent> List<T0> query(final AiBrainEventType<T0> type, final long since, final boolean reversed) {
        final List<EventEntry> entries = (reversed ? handler.queryReversed(type, since) : handler.query(type, since));
        final List<T0> events = new ArrayList<>(entries.size());
        for (final EventEntry entry : entries) {
            //noinspection unchecked
            events.add((T0) entry.event);
        }
        return events;
    }

    @Override
    public <T0 extends AiBrainEvent> Stream<T0> streamQuery(final AiBrainEventType<T0> type, final long since, final boolean reversed) {
        return query(type, since, reversed).stream();
    }

    protected static class EventHandler {
        private final PriorityQueue<EventEntry> expirationQueue;
        private final List<EventEntry> allEvents;
        private final List<EventEntry> allEventsReversed;
        private final Map<AiBrainEventType<?>, List<EventEntry>> eventsByType;
        private final Map<AiBrainEventType<?>, List<EventEntry>> eventsByTypeReversed;

        private EventHandler() {
            expirationQueue = new ObjectHeapPriorityQueue<>(EventEntry.EXPIRATION_COMPARATOR);
            allEvents = new ArrayList<>();
            allEventsReversed = new ArrayList<>();
            eventsByType = new Reference2ReferenceOpenHashMap<>();
            eventsByTypeReversed = new Reference2ReferenceOpenHashMap<>();
        }

        public void clear() {
            expirationQueue.clear();
            allEvents.clear();
            allEventsReversed.clear();
            eventsByType.clear();
            eventsByTypeReversed.clear();
        }

        public void submit(final AiBrainEvent event, final long timestamp) {
            final EventEntry entry = new EventEntry(event, timestamp, timestamp + event.lifetime());
            submit(entry);
        }

        public void submit(final EventEntry entry) {
            expirationQueue.enqueue(entry);

            int index = Collections.binarySearch(allEvents, entry, EventEntry.TIMESTAMP_COMPARATOR);
            if (index < 0) {
                index = -index - 1;
            }
            allEvents.add(index, entry);

            int revIndex = Collections.binarySearch(allEventsReversed, entry, EventEntry.REVERSED_TIMESTAMP_COMPARATOR);
            if (revIndex < 0) {
                revIndex = -revIndex - 1;
            }
            allEventsReversed.add(revIndex, entry);

            final List<EventEntry> forward = eventsByType.computeIfAbsent(entry.event.type(), i -> new ArrayList<>());
            int byTypeIndex = Collections.binarySearch(forward, entry, EventEntry.TIMESTAMP_COMPARATOR);
            if (byTypeIndex < 0) {
                byTypeIndex = -byTypeIndex - 1;
            }
            forward.add(byTypeIndex, entry);

            final List<EventEntry> backward = eventsByTypeReversed.computeIfAbsent(entry.event.type(), i -> new ArrayList<>());
            int byTypeRevIndex = Collections.binarySearch(backward, entry, EventEntry.REVERSED_TIMESTAMP_COMPARATOR);
            if (byTypeRevIndex < 0) {
                byTypeRevIndex = -byTypeRevIndex - 1;
            }
            backward.add(byTypeRevIndex, entry);
        }

        public void tick(final long timestamp) {
            final Set<EventEntry> toRemove = new ObjectOpenHashSet<>();
            final Set<AiBrainEventType<?>> typesToRemove = new ObjectOpenHashSet<>();
            while (!expirationQueue.isEmpty() && expirationQueue.first().expiration < timestamp) {
                final EventEntry entry = expirationQueue.dequeue();
                toRemove.add(entry);
                typesToRemove.add(entry.event().type());
            }
            allEvents.removeIf(toRemove::contains);
            allEventsReversed.removeIf(toRemove::contains);

            for (final AiBrainEventType<?> type : typesToRemove) {
                final List<EventEntry> forward = eventsByType.get(type);
                forward.removeIf(toRemove::contains);
                if (forward.isEmpty()) {
                    eventsByType.remove(type);
                }
                final List<EventEntry> backward = eventsByTypeReversed.get(type);
                backward.removeIf(toRemove::contains);
                if (backward.isEmpty()) {
                    eventsByTypeReversed.remove(type);
                }
            }
        }

        public List<EventEntry> query(final long since) {
            if (allEvents.isEmpty()) {
                return Collections.emptyList();
            }
            final EventEntry entry = new EventEntry(null, since, 0);
            int index = Collections.binarySearch(allEvents, entry, EventEntry.TIMESTAMP_COMPARATOR);
            if (index < 0) {
                index = -index - 1;
            }
            return allEvents.subList(index, allEvents.size());
        }

        public List<EventEntry> query(final AiBrainEventType<?> type, final long since) {
            final List<EventEntry> entries = eventsByType.get(type);
            if (entries == null || entries.isEmpty()) {
                return Collections.emptyList();
            }
            final EventEntry entry = new EventEntry(null, since, 0);
            int index = Collections.binarySearch(entries, entry, EventEntry.TIMESTAMP_COMPARATOR);
            if (index < 0) {
                index = -index - 1;
            }
            return entries.subList(index, entries.size());
        }

        public List<EventEntry> queryReversed(final long since) {
            if (allEventsReversed.isEmpty()) {
                return Collections.emptyList();
            }
            final EventEntry entry = new EventEntry(null, since, 0);
            int index = Collections.binarySearch(allEventsReversed, entry, EventEntry.REVERSED_TIMESTAMP_COMPARATOR);
            if (index < 0) {
                index = -index - 1;
            }
            return allEventsReversed.subList(0, index);
        }

        public List<EventEntry> queryReversed(final AiBrainEventType<?> type, final long since) {
            final List<EventEntry> entries = eventsByTypeReversed.get(type);
            if (entries == null || entries.isEmpty()) {
                return Collections.emptyList();
            }
            final EventEntry entry = new EventEntry(null, since, 0);
            int index = Collections.binarySearch(entries, entry, EventEntry.REVERSED_TIMESTAMP_COMPARATOR);
            if (index < 0) {
                index = -index - 1;
            }
            return entries.subList(0, index);
        }

        public boolean forget(final AiBrainEvent event) {
            final EventEntry entry = new EventEntry(event, 0, 0);
            final int idx = allEvents.indexOf(entry);
            if (idx < 0) {
                return false;
            }
            final EventEntry eventEntry = allEvents.get(idx);
            allEvents.remove(idx);
            final int revIdx = Collections.binarySearch(allEventsReversed, eventEntry, EventEntry.REVERSED_TIMESTAMP_COMPARATOR);
            allEventsReversed.remove(revIdx);
            final List<EventEntry> byType = eventsByType.getOrDefault(event.type(), Collections.emptyList());
            byType.remove(Collections.binarySearch(byType, eventEntry, EventEntry.TIMESTAMP_COMPARATOR));
            final List<EventEntry> byTypeRev = eventsByTypeReversed.getOrDefault(event.type(), Collections.emptyList());
            byTypeRev.remove(Collections.binarySearch(byTypeRev, eventEntry, EventEntry.REVERSED_TIMESTAMP_COMPARATOR));
            return true;
        }
    }

    private record EventEntry(AiBrainEvent event, long timestamp, long expiration) {
        public static final Comparator<EventEntry> TIMESTAMP_COMPARATOR = Comparator.comparingLong(EventEntry::timestamp);
        public static final Comparator<EventEntry> REVERSED_TIMESTAMP_COMPARATOR = TIMESTAMP_COMPARATOR.reversed();
        public static final Comparator<EventEntry> EXPIRATION_COMPARATOR = Comparator.comparingLong(EventEntry::expiration);

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EventEntry entry)) {
                return false;
            }
            if (event == entry.event()) {
                return true;
            }
            if (event == null) {
                return false;
            }
            return event.equals(entry.event);
        }

        @Override
        public int hashCode() {
            return event == null ? 0 : event.hashCode();
        }
    }
}
