package io.github.stuff_stuffs.aiex.common.api.entity_reference;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public interface EntityReferenceDataType<T extends EntityReferenceData, K> {
    RegistryKey<Registry<EntityReferenceDataType<?, ?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("entity_reference_data_types"));
    Registry<EntityReferenceDataType<?, ?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).buildAndRegister();

    Codec<T> codec();

    Class<K> applicableEntityType();

    T extract(K entity);
}
