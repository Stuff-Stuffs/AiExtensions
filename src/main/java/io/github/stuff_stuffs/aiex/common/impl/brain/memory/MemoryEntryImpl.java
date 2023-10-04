package io.github.stuff_stuffs.aiex.common.impl.brain.memory;

import io.github.stuff_stuffs.aiex.common.api.brain.AiBrainView;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.Memory;
import io.github.stuff_stuffs.aiex.common.api.brain.memory.MemoryEntry;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.Set;

public class MemoryEntryImpl<T> implements MemoryEntry<T> {
    private final Memory<T> type;
    private final AiBrainView brain;
    private final Set<Memory<?>> listeners;
    private T val;

    public MemoryEntryImpl(final Memory<T> type, final AiBrainView brain, final T val) {
        this.type = type;
        this.brain = brain;
        this.val = val;
        listeners = new ReferenceOpenHashSet<>();
    }

    public void addListener(final Memory<?> memory) {
        listeners.add(memory);
    }

    @Override
    public Memory<T> type() {
        return type;
    }

    @Override
    public T get() {
        return val;
    }

    @Override
    public void set(final T value) {
        if (!value.equals(val)) {
            final T old = val;
            val = value;
            for (final Memory<?> listener : listeners) {
                listenerUpdate(listener, old, val);
            }
        }
    }

    private <T0> void listenerUpdate(final Memory<T0> memory, final T oldValue, final T newValue) {
        final MemoryEntry<T0> entry = brain.memories().get(memory);
        entry.set(memory.listenerMemoryUpdate(type, oldValue, newValue, entry.get(), brain));
    }

    public static Optional<? extends MemoryEntryImpl<?>> read(final NbtCompound nbt, final AiBrainView brain) {
        final Memory<?> type = Memory.REGISTRY.get(new Identifier(nbt.getString("type")));
        if (type == null) {
            return Optional.empty();
        }
        return read(nbt, type, brain);
    }

    private static <T> Optional<MemoryEntryImpl<T>> read(final NbtCompound nbt, final Memory<T> type, final AiBrainView brain) {
        final Optional<T> dataRes = type.codec().parse(NbtOps.INSTANCE, nbt.get("data")).result();
        if (dataRes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MemoryEntryImpl<>(type, brain, dataRes.get()));
    }

    public void writeNbt(final NbtCompound nbt) {
        final Optional<NbtElement> valRes = type.codec().encodeStart(NbtOps.INSTANCE, val).result();
        if (valRes.isPresent()) {
            nbt.putString("type", Memory.REGISTRY.getId(type).toString());
            nbt.put("data", valRes.get());
        }
    }
}
