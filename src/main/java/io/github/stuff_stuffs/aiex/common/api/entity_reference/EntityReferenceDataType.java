package io.github.stuff_stuffs.aiex.common.api.entity_reference;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.EntityReferenceDataTypeCache;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

import java.util.List;
import java.util.Map;

public interface EntityReferenceDataType<T extends EntityReferenceData, K> {
    RegistryKey<Registry<EntityReferenceDataType<?, ?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("entity_reference_data_types"));
    Registry<EntityReferenceDataType<?, ?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    Codec<T> codec();

    Class<K> applicableEntityType();

    T extract(K entity);

    static Map<EntityReferenceDataType<?, ?>, EntityReferenceData> compute(final Entity entity) {
        final List<EntityReferenceDataType<?, ?>> applicable = EntityReferenceDataTypeCache.getApplicable(entity.getClass());
        final Map<EntityReferenceDataType<?, ?>, EntityReferenceData> data = new Reference2ObjectOpenHashMap<>();
        for (final EntityReferenceDataType<?, ?> type : applicable) {
            //noinspection unchecked
            data.put(type, ((EntityReferenceDataType<?, Entity>) type).extract(entity));
        }
        return data;
    }
}
