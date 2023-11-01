package io.github.stuff_stuffs.aiex.common.api.aoi;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;

public final class AreaOfInterestType<T extends AreaOfInterest> {
    public static final RegistryKey<Registry<AreaOfInterestType<?>>> REGISTRY_KEY = RegistryKey.ofRegistry(AiExCommon.id("area_of_interest_types"));
    public static final Registry<AreaOfInterestType<?>> REGISTRY = FabricRegistryBuilder.createSimple(REGISTRY_KEY).attribute(RegistryAttribute.SYNCED).buildAndRegister();
    private final Codec<T> codec;

    public AreaOfInterestType(final Codec<T> codec) {
        this.codec = codec;
    }

    public Codec<T> codec() {
        return codec;
    }
}
