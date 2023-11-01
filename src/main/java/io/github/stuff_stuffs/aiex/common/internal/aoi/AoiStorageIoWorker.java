package io.github.stuff_stuffs.aiex.common.internal.aoi;

import com.mojang.datafixers.util.Either;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.util.thread.TaskQueue;
import net.minecraft.world.World;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class AoiStorageIoWorker implements AutoCloseable {
    private final Path dir;
    private final TaskExecutor<TaskQueue.PrioritizedTask> storageWorker;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public AoiStorageIoWorker(final Path dir, final RegistryKey<World> worldKey) {
        this.dir = dir;
        storageWorker = new TaskExecutor<>(new TaskQueue.Prioritized(Priority.values().length), Util.getIoWorkerExecutor(), "AoiIOWorker-" + worldKey.getValue());
    }

    private Path sectionFilename(final AoiSectionPos pos) {
        return dir.resolve("s(" + pos.x() + ")(" + pos.z() + ").nbt");
    }

    private Path databaseFilename(final long id) {
        return dir.resolve("d(" + Long.toUnsignedString(id, 16) + ").nbt");
    }

    private Path rootFilename() {
        return dir.resolve("root.nbt");
    }

    public void saveRoot(final long nextId) {
        storageWorker.askFallible(listener -> new TaskQueue.PrioritizedTask(Priority.SAVE.ordinal(), () -> {
            listener.send(Either.left(null));
            try {
                final NbtCompound write = new NbtCompound();
                write.putLong("nextId", nextId);
                final Path filename = rootFilename();
                final Path parent = filename.getParent();
                if (!Files.isDirectory(parent)) {
                    Files.createDirectories(parent);
                }
                NbtIo.writeCompressed(write, filename.toFile());
                listener.send(Either.left(null));
            } catch (final Exception e) {
                AiExCommon.LOGGER.error("Error while saving aoi!", e);
                listener.send(Either.right(e));
            }
        }));
    }

    public void saveSection(final AreaOfInterestSection section) {
        storageWorker.askFallible(listener -> new TaskQueue.PrioritizedTask(Priority.SAVE.ordinal(), () -> {
            listener.send(Either.left(null));
            try {
                final NbtCompound write = section.write();
                final Path filename = sectionFilename(section.pos());
                final Path parent = filename.getParent();
                if (!Files.isDirectory(parent)) {
                    Files.createDirectories(parent);
                }
                if (write != null) {
                    NbtIo.writeCompressed(write, filename.toFile());
                } else {
                    Files.deleteIfExists(filename);
                }
                listener.send(Either.left(null));
            } catch (final Exception e) {
                AiExCommon.LOGGER.error("Error while saving aoi!", e);
                listener.send(Either.right(e));
            }
        }));
    }

    public void saveDatabaseSection(final AoiDatabaseSection section) {
        storageWorker.askFallible(listener -> new TaskQueue.PrioritizedTask(Priority.SAVE.ordinal(), () -> {
            listener.send(Either.left(null));
            try {
                final NbtCompound write = section.write();
                final Path filename = databaseFilename(section.offset() / AoiDatabaseSection.SIZE);
                final Path parent = filename.getParent();
                if (!Files.isDirectory(parent)) {
                    Files.createDirectories(parent);
                }
                if (write != null) {
                    NbtIo.writeCompressed(write, filename.toFile());
                } else {
                    Files.deleteIfExists(filename);
                }
                listener.send(Either.left(null));
            } catch (final Exception e) {
                AiExCommon.LOGGER.error("Error while saving aoi!", e);
                listener.send(Either.right(e));
            }
        }));
    }

    public CompletableFuture<Optional<NbtCompound>> loadSection(final AoiSectionPos pos) {
        return storageWorker.askFallible(listener -> new TaskQueue.PrioritizedTask(Priority.LOAD.ordinal(), () -> {
            final Path filename = sectionFilename(pos);
            final boolean exists = Files.isRegularFile(filename);
            if (exists) {
                try {
                    final NbtCompound compound = NbtIo.readCompressed(filename.toFile());
                    listener.send(Either.left(Optional.of(compound)));
                } catch (final FileNotFoundException e) {
                    listener.send(Either.left(Optional.empty()));
                } catch (final Exception e) {
                    AiExCommon.LOGGER.error("Error while loading aoi!", e);
                    listener.send(Either.right(e));
                }
            } else {
                listener.send(Either.left(Optional.empty()));
            }
        }));
    }

    public CompletableFuture<Optional<NbtCompound>> loadDatabaseSection(final long offset) {
        return storageWorker.askFallible(listener -> new TaskQueue.PrioritizedTask(Priority.LOAD.ordinal(), () -> {
            final Path filename = databaseFilename(offset);
            final boolean exists = Files.isRegularFile(filename);
            if (exists) {
                try {
                    final NbtCompound compound = NbtIo.readCompressed(filename.toFile());
                    listener.send(Either.left(Optional.of(compound)));
                } catch (final FileNotFoundException e) {
                    listener.send(Either.left(Optional.empty()));
                } catch (final Exception e) {
                    AiExCommon.LOGGER.error("Error while loading aoi!", e);
                    listener.send(Either.right(e));
                }
            } else {
                listener.send(Either.left(Optional.empty()));
            }
        }));
    }

    public CompletableFuture<Optional<NbtCompound>> loadRoot() {
        return storageWorker.askFallible(listener -> new TaskQueue.PrioritizedTask(Priority.LOAD.ordinal(), () -> {
            final Path filename = rootFilename();
            final boolean exists = Files.isRegularFile(filename);
            if (exists) {
                try {
                    final NbtCompound compound = NbtIo.readCompressed(filename.toFile());
                    listener.send(Either.left(Optional.of(compound)));
                } catch (final FileNotFoundException e) {
                    listener.send(Either.left(Optional.empty()));
                } catch (final Exception e) {
                    AiExCommon.LOGGER.error("Error while loading aoi!", e);
                    listener.send(Either.right(e));
                }
            } else {
                listener.send(Either.left(Optional.empty()));
            }
        }));
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            storageWorker.ask(listener -> new TaskQueue.PrioritizedTask(Priority.SHUTDOWN.ordinal(), () -> listener.send(Unit.INSTANCE))).join();
            storageWorker.close();
        }
    }

    enum Priority {
        LOAD,
        SAVE,
        SHUTDOWN
    }
}
