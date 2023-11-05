package io.github.stuff_stuffs.aiex.common.impl.brain.memory;

import com.google.common.collect.AbstractIterator;
import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

import java.util.*;

public class MemoriesImpl implements AiBrainView.Memories {
    private static final long INVALID_ID = Long.MIN_VALUE;
    private final Object2LongMap<MemoryName<?>> idByName;
    private final Long2ObjectMap<MemoryName<?>> nameById;
    private final Long2ObjectMap<MemoryImpl<?>> byId;
    private final Long2ObjectMap<LongSet> containedBy;
    private final Long2ObjectMap<LongSet> containing;
    private final Long2ObjectLinkedOpenHashMap<TickingMemory> tickable;
    private long nextId = INVALID_ID + 1;

    public MemoriesImpl() {
        idByName = new Object2LongOpenHashMap<>();
        idByName.defaultReturnValue(INVALID_ID);
        nameById = new Long2ObjectOpenHashMap<>();
        byId = new Long2ObjectOpenHashMap<>();
        containedBy = new Long2ObjectOpenHashMap<>();
        containing = new Long2ObjectOpenHashMap<>();
        tickable = new Long2ObjectLinkedOpenHashMap<>();
    }

    @Override
    public boolean has(final MemoryName<?> memory) {
        return idByName.containsKey(memory);
    }

    @Override
    public boolean has(final MemoryReference<?> reference) {
        final MemoryReferenceImpl<?> casted = (MemoryReferenceImpl<?>) reference;
        final MemoryImpl<?> memory = byId.get(casted.id());
        if (memory == null) {
            return false;
        }
        return memory.type() == reference.type();
    }

    @Override
    public <T> Optional<Memory<T>> get(final MemoryName<T> memory) {
        final long id = idByName.getLong(memory);
        if (id == INVALID_ID) {
            return Optional.empty();
        }
        final MemoryImpl<?> impl = byId.get(id);
        if (impl == null || impl.type() != memory.type()) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return Optional.of((MemoryImpl<T>) impl);
    }

    @Override
    public <T> Optional<Memory<T>> get(final MemoryReference<T> memory) {
        final MemoryReferenceImpl<?> casted = (MemoryReferenceImpl<?>) memory;
        final MemoryImpl<?> impl = byId.get(casted.id());
        if (impl == null) {
            return Optional.empty();
        }
        if (impl.type() != casted.type()) {
            throw new RuntimeException();
        }
        //noinspection unchecked
        return Optional.of((MemoryImpl<T>) impl);
    }

    @Override
    public <T> MemoryReference<T> add(final MemoryType<T> type, final T value) {
        final long id = nextId++;
        final MemoryImpl<T> memory = new MemoryImpl<>(type, value, id, this, m -> tickable.put(id, m), () -> tickable.remove(id));
        byId.put(id, memory);
        return memory.reference();
    }

    @Override
    public <T> void put(final MemoryName<T> name, final T value) {
        long id = idByName.getLong(name);
        if (id == INVALID_ID) {
            long curId = nextId++;
            idByName.put(name, curId);
            nameById.put(curId, name);
            final MemoryImpl<T> memory = new MemoryImpl<>(name.type(), value, curId, this, m -> tickable.put(curId, m), () -> tickable.remove(curId));
            byId.put(curId, memory);
        } else {
            final MemoryImpl<?> memory = byId.get(id);
            if (memory.type() != name.type()) {
                throw new RuntimeException();
            }
            final MemoryImpl<T> casted = (MemoryImpl<T>) memory;
            casted.set(value);
        }
    }

    private <T> void changeUpdateNetwork(final MemoryImpl<T> memory) {
        final Collection<? extends MemoryReference<?>> collection = memory.type().insideOf(memory.get());
        final LongSet containing = this.containing.computeIfAbsent(memory.id(), i -> new LongOpenHashSet());
        final LongSet diff = new LongOpenHashSet(containing);
        final LongSet touched = new LongOpenHashSet();
        for (final MemoryReference<?> reference : collection) {
            final long id = ((MemoryReferenceImpl<?>) reference).id();
            if (containing.add(id)) {
                touched.add(id);
            }
            diff.remove(id);
        }
        LongIterator iterator = diff.iterator();
        while (iterator.hasNext()) {
            final long l = iterator.nextLong();
            containing.remove(l);
            this.containing.get(l).remove(memory.id());
        }
        iterator = touched.iterator();
        while (iterator.hasNext()) {
            containedBy.computeIfAbsent(iterator.nextLong(), p -> new LongOpenHashSet()).add(memory.id());
        }
    }

    @Override
    public boolean forget(final MemoryReference<?> memory) {
        final MemoryReferenceImpl<?> casted = (MemoryReferenceImpl<?>) memory;
        return forget(casted.id(), memory.type());
    }

    @Override
    public boolean forget(final MemoryName<?> name) {
        final long id = idByName.getLong(name);
        if (id == INVALID_ID) {
            return false;
        }
        return forget(id, name.type());
    }

    private boolean forget(final long id, final MemoryType<?> type) {
        final MemoryImpl<?> removed = byId.remove(id);
        if (removed == null) {
            return false;
        }
        final MemoryName<?> name = nameById.remove(id);
        if (name != null) {
            idByName.removeLong(name);
        }
        removed.forget();
        containing.remove(id);
        final LongSet set = containedBy.remove(id);
        if (set == null || set.isEmpty()) {
            return true;
        }
        final LongIterator iterator = set.iterator();
        final List<Runnable> changes = new ArrayList<>(set.size());
        final MemoryReferenceImpl<?> reference = new MemoryReferenceImpl<>(type, id);
        while (iterator.hasNext()) {
            final long otherId = iterator.nextLong();
            final MemoryImpl<?> memory = byId.get(otherId);
            if (memory == null) {
                throw new RuntimeException();
            }
            changes.add(forgetUpdate(memory, reference));
        }
        final LongSet longs = containedBy.remove(id);
        if (longs != null) {
            final LongIterator iter = longs.iterator();
            while (iter.hasNext()) {
                containing.get(iter.nextLong()).remove(id);
            }
        }
        for (final Runnable change : changes) {
            change.run();
        }
        return true;
    }

    public <T> void change(final long id, final MemoryImpl<T> cursor) {
        final LongSet set = containedBy.get(id);
        if (set == null || set.isEmpty()) {
            return;
        }
        final LongIterator iterator = set.iterator();
        final List<Runnable> changes = new ArrayList<>(set.size());
        while (iterator.hasNext()) {
            final MemoryImpl<?> memory = byId.get(id);
            if (memory == null) {
                throw new RuntimeException();
            }
            changes.add(update(memory, cursor));
        }
        changeUpdateNetwork(cursor);
        for (final Runnable change : changes) {
            change.run();
        }
    }

    private <T, K> Runnable update(final Memory<K> memory, final Memory<T> cursor) {
        return () -> {
            if (memory.forgotten()) {
                return;
            }
            final Optional<K> optional = memory.type().changeContained(cursor, memory.get());
            if (optional.isEmpty()) {
                forget(memory.reference());
            } else {
                memory.set(optional.get());
            }
        };
    }

    private <K> Runnable forgetUpdate(final MemoryImpl<K> memory, final MemoryReferenceImpl<?> reference) {
        return () -> {
            if (memory.forgotten()) {
                return;
            }
            final LongSet set = containing.get(memory.id());
            set.remove(reference.id());
            final Optional<K> optional = memory.type().forgetContained(reference, memory.get());
            if (optional.isEmpty()) {
                forget(memory.reference());
            } else {
                memory.set(optional.get());
            }
        };
    }

    public Iterator<Memory<?>> containedIn(final long id) {
        final LongSet set = containedBy.get(id);
        if (set == null || set.isEmpty()) {
            return Collections.emptyIterator();
        }
        return new AbstractIterator<>() {
            private final LongIterator iterator = set.iterator();

            @Override
            protected Memory<?> computeNext() {
                while (iterator.hasNext()) {
                    final long l = iterator.nextLong();
                    final MemoryImpl<?> memory = byId.get(l);
                    if (!(memory == null || memory.forgotten())) {
                        return memory;
                    }
                }
                return endOfData();
            }
        };
    }

    public void readNbt(final NbtCompound compound) {
        idByName.clear();
        byId.clear();
        containedBy.clear();
        containing.clear();
        for (final String key : compound.getKeys()) {
            try {
                final NbtCompound nbt = compound.getCompound(key);
                final long id = Long.parseUnsignedLong(key, 16);
                final Optional<MemoryType<?>> optType = MemoryType.CODEC.parse(NbtOps.INSTANCE, nbt.get("type")).result();
                if (optType.isEmpty()) {
                    continue;
                }
                final MemoryType<?> type = optType.get();
                final MemoryImpl<?> data = create(type, nbt.get("data"), id);
                byId.put(id, data);
                if (nbt.contains("name")) {
                    final Optional<MemoryName<?>> name = MemoryName.CODEC.parse(NbtOps.INSTANCE, nbt.get("name")).result();
                    if (name.isPresent()) {
                        idByName.put(name.get(), id);
                    }
                }
            } catch (final NumberFormatException ignored) {
            }
        }
        for (final MemoryImpl<?> value : byId.values()) {
            updateContained(value);
        }
    }

    private <T> void updateContained(final MemoryImpl<T> memory) {
        final LongSet set = containing.computeIfAbsent(memory.id(), i -> new LongOpenHashSet());
        for (final MemoryReference<?> reference : memory.type().insideOf(memory.get())) {
            final MemoryReferenceImpl<?> casted = (MemoryReferenceImpl<?>) reference;
            if (has(casted)) {
                containedBy.computeIfAbsent(casted.id(), i -> new LongOpenHashSet()).add(memory.id());
                set.add(casted.id());
            }
        }
    }

    private <T> MemoryImpl<T> create(final MemoryType<T> type, final NbtElement element, final long id) {
        final T val = type.codec().parse(NbtOps.INSTANCE, element).result().orElseThrow(RuntimeException::new);
        return new MemoryImpl<>(type, val, id, this, m -> tickable.put(id, m), () -> tickable.remove(id));
    }

    public NbtCompound writeNbt() {
        final NbtCompound compound = new NbtCompound();
        final LongSet visited = new LongOpenHashSet();
        for (final Object2LongMap.Entry<MemoryName<?>> entry : idByName.object2LongEntrySet()) {
            visited.add(entry.getLongValue());
            final MemoryImpl<?> memory = byId.get(entry.getLongValue());
            final NbtCompound wrapper = new NbtCompound();
            if (write(memory, wrapper)) {
                final Optional<NbtElement> result = MemoryType.CODEC.encodeStart(NbtOps.INSTANCE, memory.type()).result();
                if (result.isPresent()) {
                    wrapper.put("type", result.get());
                    final Optional<NbtElement> nameResult = MemoryName.CODEC.encodeStart(NbtOps.INSTANCE, entry.getKey()).result();
                    if (nameResult.isPresent()) {
                        wrapper.put("name", nameResult.get());
                    }
                    compound.put(Long.toUnsignedString(entry.getLongValue(), 16), wrapper);
                }
            }
        }
        for (final Long2ObjectMap.Entry<MemoryImpl<?>> entry : byId.long2ObjectEntrySet()) {
            if (visited.contains(entry.getLongKey())) {
                continue;
            }
            final NbtCompound wrapper = new NbtCompound();
            final MemoryImpl<?> memory = entry.getValue();
            if (write(memory, wrapper)) {
                final Optional<NbtElement> result = MemoryType.CODEC.encodeStart(NbtOps.INSTANCE, memory.type()).result();
                if (result.isPresent()) {
                    wrapper.put("type", result.get());
                    compound.put(Long.toUnsignedString(entry.getLongKey(), 16), wrapper);
                }
            }
        }
        return compound;
    }

    private <T> boolean write(final MemoryImpl<T> memory, final NbtCompound compound) {
        final Optional<NbtElement> result = memory.type().codec().encodeStart(NbtOps.INSTANCE, memory.get()).result();
        if (result.isPresent()) {
            compound.put("data", result.get());
            return true;
        }
        return false;
    }
}
