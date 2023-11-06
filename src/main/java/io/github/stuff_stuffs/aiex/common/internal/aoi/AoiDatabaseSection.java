package io.github.stuff_stuffs.aiex.common.internal.aoi;

import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AoiDatabaseSection {
    public static final AoiSectionPos INVALID_POS = new AoiSectionPos(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private static final long INVALID_KEY = pack(INVALID_POS.x(), INVALID_POS.z());
    public static final long SIZE = 1024;
    private final long offset;
    private final List<AoiSectionPos> data;
    private boolean dirty = false;

    public AoiDatabaseSection(final long offset, final NbtCompound compound) {
        this.offset = offset;
        final long v = compound.getLong("version");
        if (v != AreaOfInterestSection.VERSION) {
            throw new IllegalStateException();
        }
        final long[] data = compound.getLongArray("data");
        final int m = (int) Math.min(data.length, SIZE);
        this.data = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            final long datum = data[i];
            if (datum == INVALID_KEY) {
                this.data.add(INVALID_POS);
            } else {
                this.data.add(new AoiSectionPos(unpackX(datum), unpackZ(datum)));
            }
        }
    }

    public AoiDatabaseSection(final long offset) {
        this.offset = offset;
        data = new ArrayList<>();
    }

    public void set(final long id, final AoiSectionPos pos) {
        final long l = id - offset;
        final int size = data.size();
        if (l > size) {
            throw new IndexOutOfBoundsException();
        }
        if (l == size) {
            data.add(pos);
            dirty = true;
        } else {
            final AoiSectionPos old = data.set((int) id, pos);
            if (!pos.equals(old)) {
                dirty = true;
            }
        }
    }

    public boolean dirty() {
        return dirty;
    }

    public void clean() {
        dirty = false;
    }

    public Optional<AoiSectionPos> get(final long id) {
        final long l = id - offset;
        final int size = data.size();
        if (l < 0 || l >= size) {
            return Optional.empty();
        }
        final AoiSectionPos value = data.get((int) l);
        if (value.equals(INVALID_POS)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public NbtCompound write() {
        final NbtCompound compound = new NbtCompound();
        compound.putLong("version", AreaOfInterestSection.VERSION);
        final int size = data.size();
        final long[] arr = new long[size];
        for (int i = 0; i < size; i++) {
            final AoiSectionPos pos = data.get(i);
            arr[i] = pack(pos.x(), pos.z());
        }
        compound.putLongArray("data", arr);
        return compound;
    }

    public long offset() {
        return offset;
    }

    private static long pack(final int x, final int z) {
        return ((long) x << 32) | (z & 0xFFFF_FFFFL);
    }

    private static int unpackX(final long l) {
        return (int) (l >>> 32L);
    }

    private static int unpackZ(final long l) {
        return (int) (l & 0xFFFF_FFFFL);
    }
}
