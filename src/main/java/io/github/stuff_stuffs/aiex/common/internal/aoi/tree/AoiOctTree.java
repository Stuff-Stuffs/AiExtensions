package io.github.stuff_stuffs.aiex.common.internal.aoi.tree;

import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestBounds;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestEntry;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.impl.aoi.AreaOfInterestReferenceImpl;
import io.github.stuff_stuffs.aiex.common.internal.aoi.AoiSectionPos;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class AoiOctTree {
    private final Long2ObjectMap<AreaOfInterestBounds> bounds;
    private final int x, y, z;
    private final int worldHeight;
    private @Nullable AoiOctTreeNode root;

    public AoiOctTree(final int x, final int y, final int z, final int worldHeight) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldHeight = worldHeight;
        bounds = new Long2ObjectOpenHashMap<>();
    }

    public void add(final AreaOfInterestEntry<?> entry) {
        final AreaOfInterestReferenceImpl<?> reference = (AreaOfInterestReferenceImpl<?>) entry.reference();
        final AreaOfInterestBounds old = bounds.put(reference.id(), entry.bounds());
        if (old != null && root != null) {
            root.remove(reference.id(), old);
        }
        if (root == null) {
            root = new AoiOctTreeNode(x, y, z, AoiSectionPos.CHUNKS_PER_SECTION * 16, worldHeight, AoiSectionPos.CHUNKS_PER_SECTION * 16);
        }
        root.add(entry);
    }

    public void remove(final long reference) {
        final AreaOfInterestBounds bounds = this.bounds.remove(reference);
        if (bounds != null && root != null) {
            root.remove(reference, bounds);
            if (root.childEntryCount == 0) {
                root = null;
            }
        }
    }

    public Stream<AreaOfInterestEntry<?>> stream(final AreaOfInterestBounds bounds) {
        if (root == null) {
            return Stream.empty();
        }
        return Stream.of(root).mapMulti((node, consumer) -> node.forEachIntersecting(bounds, consumer));
    }

    public <T extends AreaOfInterest> Stream<AreaOfInterestEntry<T>> stream(final AreaOfInterestBounds bounds, final AreaOfInterestType<T> type) {
        if (root == null) {
            return Stream.empty();
        }
        return Stream.of(root).mapMulti((node, consumer) -> node.forEachIntersecting(bounds, consumer, type));
    }
}
