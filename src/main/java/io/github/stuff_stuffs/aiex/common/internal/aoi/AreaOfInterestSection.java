package io.github.stuff_stuffs.aiex.common.internal.aoi;

import io.github.stuff_stuffs.aiex.common.api.aoi.*;
import io.github.stuff_stuffs.aiex.common.impl.aoi.AreaOfInterestEntryImpl;
import io.github.stuff_stuffs.aiex.common.impl.aoi.AreaOfInterestReferenceImpl;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.aoi.tree.AoiOctTree;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class AreaOfInterestSection {
    public static final long VERSION = 0;
    private final AoiSectionPos pos;
    private final Long2ObjectMap<AreaOfInterestEntry<?>> areas;
    private final Long2ObjectMap<AreaOfInterestEntry<?>> ticking;
    private final AoiOctTree tree;
    private boolean dirty = false;

    public AreaOfInterestSection(final AoiSectionPos pos, final int yStart, final int worldHeight) {
        this.pos = pos;
        areas = new Long2ObjectLinkedOpenHashMap<>();
        ticking = new Long2ObjectLinkedOpenHashMap<>();
        final ChunkPos lower = pos.lower();
        tree = new AoiOctTree(lower.getStartX(), yStart, lower.getStartZ(), worldHeight);
    }

    public AreaOfInterestSection(final AoiSectionPos pos, final NbtCompound nbt, final RegistryKey<World> worldKey, final int yStart, final int worldHeight) {
        this.pos = pos;
        areas = new Long2ObjectLinkedOpenHashMap<>();
        ticking = new Long2ObjectLinkedOpenHashMap<>();
        final ChunkPos lower = pos.lower();
        tree = new AoiOctTree(lower.getStartX(), yStart, lower.getStartZ(), worldHeight);
        if (nbt.getLong("version") != VERSION) {
            AiExCommon.LOGGER.error("Invalid aoi version found!");
            return;
        }
        final NbtCompound data = nbt.getCompound("data");
        for (final String key : data.getKeys()) {
            try {
                final long l = Long.parseUnsignedLong(key, 16);
                final NbtCompound entry = data.getCompound(key);
                final AreaOfInterestType<?> type = AreaOfInterestType.REGISTRY.get(new Identifier(entry.getString("type")));
                if (type == null) {
                    AiExCommon.LOGGER.error("Invalid aoi type found! {}", entry.getString("type"));
                    continue;
                }
                final Optional<AreaOfInterestBounds> bounds = AreaOfInterestBounds.CODEC.parse(NbtOps.INSTANCE, entry.get("bounds")).result();
                if (bounds.isEmpty()) {
                    AiExCommon.LOGGER.error("Could not decode aoi of type {}, due to bounds!", entry.getString("type"));
                    continue;
                }
                final Optional<? extends AreaOfInterest> result = type.codec().parse(NbtOps.INSTANCE, entry.get("data")).result();
                if (result.isEmpty()) {
                    AiExCommon.LOGGER.error("Could not decode aoi of type {}!", entry.getString("type"));
                    continue;
                }

                //noinspection rawtypes,unchecked
                final AreaOfInterestEntryImpl areaOfInterestEntry = new AreaOfInterestEntryImpl<>(result.get(), bounds.get(), new AreaOfInterestReferenceImpl(l, worldKey, result.get().type()));
                areas.put(l, areaOfInterestEntry);
                tree.add(areaOfInterestEntry);
                if (areaOfInterestEntry.value() instanceof TickingAreaOfInterest) {
                    ticking.put(l, areaOfInterestEntry);
                }
                areaOfInterestEntry.value().setRef(areaOfInterestEntry.reference());
            } catch (final NumberFormatException e) {
                AiExCommon.LOGGER.error("Invalid aoi id {}!", key);
            } catch (final InvalidIdentifierException e) {
                AiExCommon.LOGGER.error("Invalid aoi type id!", e);
            }
        }
    }

    public Optional<AreaOfInterestEntry<?>> get(final long id) {
        return Optional.ofNullable(areas.getOrDefault(id, null));
    }

    public boolean dirty() {
        return dirty;
    }

    public void clean() {
        dirty = false;
    }

    public <T extends AreaOfInterest> void forEach(final AreaOfInterestType<T> type, final Consumer<AreaOfInterestEntry<T>> consumer) {
        for (final AreaOfInterestEntry<?> entry : areas.values()) {
            if (entry.value().type() == type) {
                //noinspection unchecked
                consumer.accept((AreaOfInterestEntry<T>) entry);
            }
        }
    }

    public AoiSectionPos pos() {
        return pos;
    }

    public @Nullable NbtCompound write() {
        if (areas.isEmpty()) {
            return null;
        }
        final NbtCompound compound = new NbtCompound();
        final NbtCompound map = new NbtCompound();
        compound.put("data", map);
        compound.putLong("version", VERSION);
        for (final Long2ObjectMap.Entry<AreaOfInterestEntry<?>> entry : areas.long2ObjectEntrySet()) {
            final NbtCompound sub = new NbtCompound();
            sub.putString("type", AreaOfInterestType.REGISTRY.getId(entry.getValue().value().type()).toString());
            final Optional<NbtElement> encodedBounds = AreaOfInterestBounds.CODEC.encodeStart(NbtOps.INSTANCE, entry.getValue().bounds()).result();
            if (encodedBounds.isPresent()) {
                sub.put("bounds", encodedBounds.get());
                final Optional<NbtElement> encode = encode(entry.getValue().value().type(), entry.getValue().value());
                if (encode.isPresent()) {
                    sub.put("data", encode.get());
                    map.put(Long.toUnsignedString(entry.getLongKey(), 16), sub);
                }
            }
        }
        return compound;
    }

    public Stream<AreaOfInterestEntry<?>> stream(final AreaOfInterestBounds bounds) {
        return tree.stream(bounds);
    }

    public <T extends AreaOfInterest> Stream<AreaOfInterestEntry<T>> stream(final AreaOfInterestType<T> type, final AreaOfInterestBounds bounds) {
        return tree.stream(bounds, type);
    }

    private <T extends AreaOfInterest> Optional<NbtElement> encode(final AreaOfInterestType<T> type, final AreaOfInterest value) {
        return type.codec().encodeStart(NbtOps.INSTANCE, (T) value).result();
    }

    public void add(final AreaOfInterestEntryImpl<?> entry) {
        final long id = ((AreaOfInterestReferenceImpl<?>) entry.reference()).id();
        entry.value().setRef(entry.reference());
        areas.put(id, entry);
        tree.add(entry);
        if (entry.value() instanceof TickingAreaOfInterest) {
            ticking.put(id, entry);
        }
        dirty = true;
    }

    public Optional<AreaOfInterestEntry<?>> remove(final long id) {
        final AreaOfInterestEntry<?> entry = areas.remove(id);
        if (entry == null) {
            return Optional.empty();
        }
        tree.remove(id);
        ticking.remove(id);
        dirty = true;
        return Optional.of(entry);
    }

    public void tick(final World world) {
        for (final AreaOfInterestEntry<?> aoi : ticking.values()) {
            ((TickingAreaOfInterest) aoi).tick(world, aoi.bounds(), aoi.reference());
        }
    }

    public void markDirty() {
        dirty = true;
    }
}
