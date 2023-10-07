package io.github.stuff_stuffs.aiex.common.internal;

import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceData;
import io.github.stuff_stuffs.aiex.common.api.entity_reference.EntityReferenceDataType;
import io.github.stuff_stuffs.aiex.common.impl.EntityReferenceImpl;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class EntityReferenceContainer {
    private final Map<UUID, EntityReferenceImpl> map;

    public EntityReferenceContainer() {
        map = new Object2ReferenceOpenHashMap<>();
    }

    public @Nullable EntityReferenceImpl get(final UUID uuid) {
        return map.get(uuid);
    }

    public @Nullable EntityReferenceImpl get(final UUID uuid, final Entity entity) {
        update(entity);
        return map.get(uuid);
    }

    public void update(final Entity entity) {
        final UUID uuid = entity.getUuid();
        final RegistryEntry<EntityType<?>> typeEntry = Registries.ENTITY_TYPE.getEntry(entity.getType());
        final RegistryKey<World> worldRegistryKey = entity.getEntityWorld().getRegistryKey();
        final Map<EntityReferenceDataType<?, ?>, EntityReferenceData> entityData = EntityReferenceDataType.compute(entity);
        final EntityReferenceImpl old = map.put(uuid, new EntityReferenceImpl(uuid, typeEntry, worldRegistryKey, entityData));
        if (old != null && old.type().value() != entity.getType()) {
            AiExCommon.LOGGER.error("Type changed while updating entity ref!");
        }
    }

    public void load(final LevelStorage.Session session) {
        map.clear();
        final Path directory = session.getDirectory(AiExCommon.ENTITY_REFERENCE_SAVE_PATH);
        try {
            final NbtCompound compound = NbtIo.readCompressed(directory.toFile());
            for (final String key : compound.getKeys()) {
                final UUID uuid = UUID.fromString(key);
                final Optional<EntityReferenceImpl> result = EntityReferenceImpl.decode(uuid, compound.getCompound(key));
                if (result.isPresent()) {
                    map.put(uuid, result.get());
                }
            }
        } catch (final IOException e) {
            if (!(e instanceof FileNotFoundException)) {
                AiExCommon.LOGGER.error("Exception while loading entity references! ", e);
            }
        }
    }

    public void save(final LevelStorage.Session session) {
        final Path directory = session.getDirectory(AiExCommon.ENTITY_REFERENCE_SAVE_PATH);
        final NbtCompound root = new NbtCompound();
        for (final Map.Entry<UUID, EntityReferenceImpl> entry : map.entrySet()) {
            final Optional<NbtCompound> result = EntityReferenceImpl.encode(entry.getValue(), false);
            if (result.isPresent()) {
                root.put(entry.getKey().toString(), result.get());
            }
        }
        try {
            NbtIo.writeCompressed(root, directory.toFile());
        } catch (final IOException e) {
            AiExCommon.LOGGER.error("Exception while saving entity references! ", e);
        }
    }
}
