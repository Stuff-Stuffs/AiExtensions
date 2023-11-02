package io.github.stuff_stuffs.aiex.common.impl.aoi;

import io.github.stuff_stuffs.aiex.common.api.aoi.*;
import io.github.stuff_stuffs.aiex.common.api.debug.AiExDebugFlags;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommands;
import io.github.stuff_stuffs.aiex.common.internal.aoi.AoiDatabaseSection;
import io.github.stuff_stuffs.aiex.common.internal.aoi.AoiSectionPos;
import io.github.stuff_stuffs.aiex.common.internal.aoi.AreaOfInterestSection;
import io.github.stuff_stuffs.aiex.common.internal.aoi.AreaOfInterestStorage;
import io.github.stuff_stuffs.aiex.common.internal.debug.AreaOfInterestDebugMessage;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class AreaOfInterestWorldImpl implements AreaOfInterestWorld, AutoCloseable {
    private final List<AreaOfInterestDebugMessage> debugMessages;
    private final RegistryKey<World> worldKey;
    private final AreaOfInterestStorage storage;
    private boolean init = false;
    private long nextId = 0;

    public AreaOfInterestWorldImpl(final Path worldDir, final RegistryKey<World> worldKey, final int bottomY, final int worldHeight) {
        debugMessages = new ArrayList<>();
        this.worldKey = worldKey;
        storage = new AreaOfInterestStorage(worldDir.resolve("aiex_aoi"), worldKey, bottomY, worldHeight);
    }

    private void checkInit() {
        if (!init) {
            init = true;
            nextId = storage.loadNextId();
        }
    }

    public void tick(final ServerWorld world) {
        checkInit();
        storage.tick(world);
        for (final AreaOfInterestDebugMessage message : debugMessages) {
            AiExDebugFlags.send(AreaOfInterestDebugMessage.FLAG, message, world);
        }
        debugMessages.clear();
    }

    @Override
    public Stream<AreaOfInterestEntry<?>> intersecting(final AreaOfInterestBounds bounds) {
        return Arrays.stream(AoiSectionPos.from(bounds)).flatMap(pos -> storage.get(pos).stream(bounds));
    }

    @Override
    public <T extends AreaOfInterest> Stream<AreaOfInterestEntry<T>> intersecting(final AreaOfInterestBounds bounds, final AreaOfInterestType<T> type) {
        return Arrays.stream(AoiSectionPos.from(bounds)).flatMap(pos -> storage.get(pos).stream(type, bounds));
    }

    @Override
    public <T extends AreaOfInterest> Optional<AreaOfInterestEntry<T>> get(final AreaOfInterestReference<T> ref) {
        final AreaOfInterestReferenceImpl<T> casted = (AreaOfInterestReferenceImpl<T>) ref;
        if (!casted.world().equals(worldKey)) {
            return Optional.empty();
        }
        final Optional<AoiSectionPos> location = storage.getRefLocation(casted.id());
        if (location.isEmpty()) {
            return Optional.empty();
        }
        final AreaOfInterestSection section = storage.get(location.get());
        final Optional<AreaOfInterestEntry<?>> opt = section.get(casted.id());
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        final AreaOfInterestEntry<?> entry = opt.get();
        if (entry.value().type() != ref.type()) {
            return Optional.empty();
        }
        //noinspection unchecked
        return Optional.of((AreaOfInterestEntry<T>) entry);
    }

    @Override
    public <T extends AreaOfInterest> AreaOfInterestEntry<T> add(final T value, final AreaOfInterestBounds bounds) {
        checkInit();
        if (!bounds.checkValidSize()) {
            throw new IllegalArgumentException();
        }
        final AoiSectionPos pos = AoiSectionPos.from(new ChunkPos(bounds.minX() / 16, bounds.minZ() / 16));
        //noinspection unchecked
        final AreaOfInterestReferenceImpl<T> reference = new AreaOfInterestReferenceImpl<>(nextId++, worldKey, (AreaOfInterestType<T>) value.type());
        final AreaOfInterestEntryImpl<T> entry = new AreaOfInterestEntryImpl<>(value, bounds, reference);
        storage.get(pos).add(entry);
        storage.getDatabase(reference.id()).set(reference.id(), pos);
        addDebugMessage(value.type(), value, bounds, reference.id());
        return entry;
    }

    @Override
    public boolean updateBounds(final AreaOfInterestReference<?> ref, final AreaOfInterestBounds newBounds) {
        if (!newBounds.checkValidSize()) {
            throw new IllegalArgumentException();
        }
        final AreaOfInterestReferenceImpl<?> casted = (AreaOfInterestReferenceImpl<?>) ref;
        if (!casted.world().equals(worldKey)) {
            return false;
        }
        final Optional<AoiSectionPos> location = storage.getRefLocation(casted.id());
        if (location.isEmpty()) {
            return false;
        }
        final AreaOfInterestSection section = storage.get(location.get());
        final Optional<AreaOfInterestEntry<?>> opt = section.remove(casted.id());
        if (opt.isEmpty()) {
            return false;
        }
        //noinspection rawtypes,unchecked
        final AreaOfInterestEntryImpl<?> newEntry = new AreaOfInterestEntryImpl<>(opt.get().value(), newBounds, (AreaOfInterestReferenceImpl) opt.get().reference());
        final AoiSectionPos newPos = AoiSectionPos.from(new ChunkPos(newBounds.minX() / 16, newBounds.minZ() / 16));
        storage.get(newPos).add(newEntry);
        storage.getDatabase(casted.id()).set(casted.id(), newPos);
        if (AiExCommands.ENABLE_AOI_DEBUGGING) {
            debugMessages.add(new AreaOfInterestDebugMessage.Remove(casted.type(), casted.id()));
            addDebugMessage(newEntry.value().type(), newEntry.value(), newBounds, casted.id());
        }
        return true;
    }

    private <T extends AreaOfInterest> void addDebugMessage(final AreaOfInterestType<T> type, final AreaOfInterest value, final AreaOfInterestBounds bounds, final long id) {
        //noinspection unchecked
        debugMessages.add(new AreaOfInterestDebugMessage.Add<>(type, (T) value, bounds, id));
    }

    @Override
    public boolean remove(final AreaOfInterestReference<?> ref) {
        final AreaOfInterestReferenceImpl<?> casted = (AreaOfInterestReferenceImpl<?>) ref;
        if (!casted.world().equals(worldKey)) {
            return false;
        }
        final Optional<AoiSectionPos> location = storage.getRefLocation(casted.id());
        if (location.isEmpty()) {
            return false;
        }
        final AreaOfInterestSection section = storage.get(location.get());
        final Optional<AreaOfInterestEntry<?>> opt = section.remove(casted.id());
        if (opt.isEmpty()) {
            return false;
        }
        storage.getDatabase(casted.id()).set(casted.id(), AoiDatabaseSection.INVALID_POS);
        if (AiExCommands.ENABLE_AOI_DEBUGGING) {
            debugMessages.add(new AreaOfInterestDebugMessage.Remove(casted.type(), casted.id()));
        }
        return true;
    }

    @Override
    public <T extends AreaOfInterest> boolean update(final AreaOfInterestReference<T> ref, final T value) {
        final AreaOfInterestReferenceImpl<?> casted = (AreaOfInterestReferenceImpl<?>) ref;
        if (!casted.world().equals(worldKey)) {
            return false;
        }
        final Optional<AoiSectionPos> location = storage.getRefLocation(casted.id());
        if (location.isEmpty()) {
            return false;
        }
        final AreaOfInterestSection section = storage.get(location.get());
        final Optional<AreaOfInterestEntry<?>> opt = section.remove(casted.id());
        if (opt.isEmpty()) {
            return false;
        }
        //noinspection rawtypes,unchecked
        final AreaOfInterestEntryImpl<?> newEntry = new AreaOfInterestEntryImpl<>(value, opt.get().bounds(), (AreaOfInterestReferenceImpl) opt.get().reference());
        section.add(newEntry);
        if (AiExCommands.ENABLE_AOI_DEBUGGING) {
            debugMessages.add(new AreaOfInterestDebugMessage.Remove(casted.type(), casted.id()));
            addDebugMessage(newEntry.value().type(), newEntry.value(), newEntry.bounds(), casted.id());
        }
        return true;
    }

    @Override
    public boolean markDirty(final AreaOfInterestReference<?> ref) {
        final AreaOfInterestReferenceImpl<?> casted = (AreaOfInterestReferenceImpl<?>) ref;
        if (!casted.world().equals(worldKey)) {
            return false;
        }
        final Optional<AoiSectionPos> location = storage.getRefLocation(casted.id());
        if (location.isEmpty()) {
            return false;
        }
        return storage.markDirty(location.get());
    }

    @Override
    public void close() {
        storage.saveNextId(nextId);
        storage.close();
    }

    public void resync(final ServerWorld world, final AreaOfInterestType<?> type) {
        storage.resync(world, type);
    }
}
