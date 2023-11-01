package io.github.stuff_stuffs.aiex.common.internal.aoi.tree;

import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestBounds;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestEntry;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.impl.aoi.AreaOfInterestReferenceImpl;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

class AoiOctTreeNode {
    private static final int THRESHOLD = 8;
    private final Long2ObjectMap<AreaOfInterestEntry<?>> entries;
    private final AoiOctTreeNode[] children;
    private final int x, y, z;
    private final int xL, yL, zL;
    private final AreaOfInterestBounds bounds;
    int childEntryCount;

    public AoiOctTreeNode(final int x, final int y, final int z, final int xL, final int yL, final int zL) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.xL = xL;
        this.yL = yL;
        this.zL = zL;
        entries = new Long2ObjectLinkedOpenHashMap<>();
        int childCount = 0;
        if (xL > THRESHOLD) {
            childCount = 2;
        }
        if (yL > THRESHOLD) {
            if (childCount == 0) {
                childCount = 2;
            } else {
                childCount = childCount * 2;
            }
        }
        if (zL > THRESHOLD) {
            if (childCount == 0) {
                childCount = 2;
            } else {
                childCount = childCount * 2;
            }
        }
        children = new AoiOctTreeNode[childCount];
        bounds = new AreaOfInterestBounds(x, y, z, x + xL, y + yL, z + zL);
    }

    public <T extends AreaOfInterest> void forEachIntersecting(final AreaOfInterestBounds bounds, final Consumer<AreaOfInterestEntry<T>> consumer, final AreaOfInterestType<T> type) {
        for (final AreaOfInterestEntry<?> entry : entries.values()) {
            if (entry.bounds().intersects(bounds) && type == entry.value().type()) {
                //noinspection unchecked
                consumer.accept((AreaOfInterestEntry<T>) entry);
            }
        }
        for (final AoiOctTreeNode child : children) {
            if (child != null && child.bounds.intersects(bounds)) {
                child.forEachIntersecting(bounds, consumer, type);
            }
        }
    }

    public void forEachIntersecting(final AreaOfInterestBounds bounds, final Consumer<AreaOfInterestEntry<?>> consumer) {
        for (final AreaOfInterestEntry<?> entry : entries.values()) {
            if (entry.bounds().intersects(bounds)) {
                consumer.accept(entry);
            }
        }
        for (final AoiOctTreeNode child : children) {
            if (child != null && child.bounds.intersects(bounds)) {
                child.forEachIntersecting(bounds, consumer);
            }
        }
    }

    public void add(final AreaOfInterestEntry<?> entry) {
        if (xL <= THRESHOLD && yL <= THRESHOLD && zL <= THRESHOLD) {
            childEntryCount++;
            entries.put(((AreaOfInterestReferenceImpl<?>) entry.reference()).id(), entry);
            return;
        }
        final AreaOfInterestBounds bounds = entry.bounds();
        for (int i = 0; i < children.length; i++) {
            AoiOctTreeNode child = children[i];
            if (child != null && child.bounds.union(bounds).equals(child.bounds)) {
                childEntryCount++;
                child.add(entry);
                return;
            } else {
                final AreaOfInterestBounds childBounds = boundsOfChild(i);
                if (childBounds == null) {
                    throw new IllegalStateException();
                }
                if (childBounds.union(bounds).equals(childBounds)) {
                    childEntryCount++;
                    child = children[i] = new AoiOctTreeNode(childBounds.minX(), childBounds.minY(), childBounds.minZ(), childBounds.length(Direction.Axis.X), childBounds.length(Direction.Axis.Y), childBounds.length(Direction.Axis.Z));
                    child.add(entry);
                    return;
                }
            }
        }
    }

    public @Nullable AreaOfInterestEntry<?> remove(final long id, final AreaOfInterestBounds bounds) {
        final AreaOfInterestEntry<?> removed = entries.remove(id);
        if (removed != null) {
            childEntryCount--;
            return removed;
        }
        for (int i = 0; i < children.length; i++) {
            final AoiOctTreeNode child = children[i];
            if (child != null && child.bounds.union(bounds).equals(child.bounds)) {
                final AreaOfInterestEntry<?> r = child.remove(id, bounds);
                if (r != null) {
                    childEntryCount--;
                }
                if (child.childEntryCount == 0) {
                    children[i] = null;
                }
                return r;
            }
        }
        return null;
    }

    private @Nullable AreaOfInterestBounds boundsOfChild(final int index) {
        final int halfX = xL / 2;
        final int halfY = yL / 2;
        final int halfZ = zL / 2;
        final boolean firstCoord = (index & 1) == 1;
        final boolean secondCoord = (index & 2) == 2;
        final boolean thirdCoord = (index & 4) == 4;
        if (xL <= THRESHOLD) {
            if (yL <= THRESHOLD) {
                if (zL <= THRESHOLD) {
                    return null;
                }
                return AreaOfInterestBounds.fromDimensions(x, y, z + (firstCoord ? halfZ : 0), xL, yL, halfZ);
            }
            if (zL <= THRESHOLD) {
                return AreaOfInterestBounds.fromDimensions(x, y + (firstCoord ? halfY : 0), z, xL, halfY, zL);
            }
            return AreaOfInterestBounds.fromDimensions(x, y + (firstCoord ? halfY : 0), z + (secondCoord ? halfZ : 0), xL, halfY, halfZ);
        } else if (yL <= THRESHOLD) {
            if (zL <= THRESHOLD) {
                return AreaOfInterestBounds.fromDimensions(x + (firstCoord ? halfX : 0), y, z, halfX, yL, zL);
            }
            return AreaOfInterestBounds.fromDimensions(x + (firstCoord ? halfX : 0), y, z + (secondCoord ? halfZ : 0), halfX, yL, halfZ);
        } else if (zL < THRESHOLD) {
            return AreaOfInterestBounds.fromDimensions(x + (firstCoord ? halfX : 0), y + (secondCoord ? halfY : 0), z, halfX, halfY, zL);
        }
        return AreaOfInterestBounds.fromDimensions(x + (firstCoord ? halfX : 0), y + (secondCoord ? halfY : 0), z + (thirdCoord ? halfZ : 0), halfX, halfY, halfZ);
    }
}
