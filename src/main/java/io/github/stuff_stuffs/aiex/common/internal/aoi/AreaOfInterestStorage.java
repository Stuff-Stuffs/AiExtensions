package io.github.stuff_stuffs.aiex.common.internal.aoi;

import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.api.debug.AiExDebugFlags;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.debug.AreaOfInterestDebugMessage;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;

public class AreaOfInterestStorage implements AutoCloseable {
    //5 minutes
    private static final long TIMEOUT = 20 * 60 * 5;
    private final Object2ObjectLinkedOpenHashMap<AoiSectionPos, SectionEntry> sections;
    private final Long2ObjectLinkedOpenHashMap<DatabaseSectionEntry> databaseSections;
    private final AoiStorageIoWorker worker;
    private final RegistryKey<World> worldKey;
    private final int bottomY;
    private final int worldHeight;
    private long tickCount = 0;

    public AreaOfInterestStorage(final Path dir, final RegistryKey<World> worldKey, final int bottomY, final int worldHeight) {
        this.worldKey = worldKey;
        this.bottomY = bottomY;
        this.worldHeight = worldHeight;
        sections = new Object2ObjectLinkedOpenHashMap<>();
        databaseSections = new Long2ObjectLinkedOpenHashMap<>();
        worker = new AoiStorageIoWorker(dir, worldKey);
    }

    public AreaOfInterestSection get(final AoiSectionPos pos) {
        SectionEntry section = sections.getAndMoveToLast(pos);
        if (section != null) {
            section.lastAccessed = tickCount;
            return section.section;
        }
        try {
            final Optional<NbtCompound> load = worker.loadSection(pos).join();
            if (load.isEmpty()) {
                section = new SectionEntry(new AreaOfInterestSection(pos, bottomY, worldHeight), tickCount);
            } else {
                section = new SectionEntry(new AreaOfInterestSection(pos, load.get(), worldKey, bottomY, worldHeight), tickCount);
            }
        } catch (final Exception e) {
            section = new SectionEntry(new AreaOfInterestSection(pos, bottomY, worldHeight), tickCount);
        }
        sections.putAndMoveToLast(pos, section);
        return section.section;
    }

    public AoiDatabaseSection getDatabase(final long id) {
        final long offset = id / AoiDatabaseSection.SIZE;
        DatabaseSectionEntry section = databaseSections.getAndMoveToLast(offset);
        if (section != null) {
            section.lastAccessed = tickCount;
            return section.section;
        }
        try {
            final Optional<NbtCompound> load = worker.loadDatabaseSection(offset).join();
            if (load.isEmpty()) {
                section = new DatabaseSectionEntry(new AoiDatabaseSection(offset * AoiDatabaseSection.SIZE), tickCount);
            } else {
                section = new DatabaseSectionEntry(new AoiDatabaseSection(offset * AoiDatabaseSection.SIZE, load.get()), tickCount);
            }
        } catch (final Exception e) {
            section = new DatabaseSectionEntry(new AoiDatabaseSection(offset * AoiDatabaseSection.SIZE), tickCount);
        }
        databaseSections.putAndMoveToLast(offset, section);
        return section.section;
    }

    public Optional<AoiSectionPos> getRefLocation(final long id) {
        try {
            final long offset = id / AoiDatabaseSection.SIZE;
            DatabaseSectionEntry section = databaseSections.getAndMoveToLast(offset);
            if (section != null) {
                section.lastAccessed = tickCount;
                return section.section.get(id);
            }
            final Optional<NbtCompound> load = worker.loadDatabaseSection(offset).join();
            if (load.isEmpty()) {
                return Optional.empty();
            }
            section = new DatabaseSectionEntry(new AoiDatabaseSection(offset * AoiDatabaseSection.SIZE, load.get()), tickCount);
            databaseSections.putAndMoveToLast(offset, section);
            return section.section.get(id);
        } catch (final Exception e) {
            AiExCommon.LOGGER.error("Exception while loading aoi location ref!", e);
            return Optional.empty();
        }
    }

    public boolean markDirty(final AoiSectionPos pos) {
        final SectionEntry entry = sections.getAndMoveToLast(pos);
        if (entry == null) {
            return false;
        }
        entry.lastAccessed = tickCount;
        entry.section.markDirty();
        return true;
    }

    public void tick(final World world) {
        tickCount++;
        final Iterator<SectionEntry> iterator = sections.values().iterator();
        while (iterator.hasNext()) {
            final SectionEntry next = iterator.next();
            if (next.lastAccessed + TIMEOUT < tickCount) {
                if (next.section.dirty()) {
                    next.section.clean();
                    worker.saveSection(next.section);
                }
                iterator.remove();
            } else {
                next.section.tick(world);
            }
        }
        final ObjectIterator<DatabaseSectionEntry> databaseIterator = databaseSections.values().iterator();
        while (databaseIterator.hasNext()) {
            final DatabaseSectionEntry next = databaseIterator.next();
            if (next.lastAccessed + TIMEOUT < tickCount) {
                if (next.section.dirty()) {
                    next.section.clean();
                    worker.saveDatabaseSection(next.section);
                }
                databaseIterator.remove();
            } else {
                break;
            }
        }
    }

    @Override
    public void close() {
        for (final SectionEntry value : sections.values()) {
            if (value.section.dirty()) {
                worker.saveSection(value.section);
            }
        }
        sections.clear();
        for (final DatabaseSectionEntry value : databaseSections.values()) {
            if (value.section.dirty()) {
                worker.saveDatabaseSection(value.section);
            }
        }
        databaseSections.clear();
    }

    public long loadNextId() {
        try {
            return worker.loadRoot().join().map(compound -> compound.getLong("nextId")).orElse(0L);
        } catch (final Exception e) {
            AiExCommon.LOGGER.error("Error while loading root aoi file!", e);
            throw new RuntimeException(e);
        }
    }

    public void saveNextId(final long nextId) {
        worker.saveRoot(nextId);
    }

    public void resync(final ServerWorld world, final AreaOfInterestType<?> type) {
        for (final SectionEntry value : sections.values()) {
            value.section.forEach(type, entry -> {
                AiExDebugFlags.send(AreaOfInterestDebugMessage.FLAG, AreaOfInterestDebugMessage.Add.from(entry), world);
            });
        }
    }

    private static final class SectionEntry {
        private final AreaOfInterestSection section;
        private long lastAccessed;

        private SectionEntry(final AreaOfInterestSection section, final long accessed) {
            this.section = section;
            lastAccessed = accessed;
        }
    }

    private static final class DatabaseSectionEntry {
        private final AoiDatabaseSection section;
        private long lastAccessed;

        private DatabaseSectionEntry(final AoiDatabaseSection section, final long accessed) {
            this.section = section;
            lastAccessed = accessed;
        }
    }
}
