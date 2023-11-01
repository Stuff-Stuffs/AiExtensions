package io.github.stuff_stuffs.aiex_test.common;

import io.github.stuff_stuffs.aiex.common.api.AiWorldExtensions;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestBounds;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestEntry;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex_test.common.aoi.BasicAreaOfInterest;
import io.github.stuff_stuffs.aiex_test.common.entity.AiExTestEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class AiExTestCommon implements ModInitializer {
    public static final AreaOfInterestType<BasicAreaOfInterest> BASIC_AOI_TYPE = new AreaOfInterestType<>(BasicAreaOfInterest.CODEC);
    public static final Block BASIC_AOI_BLOCK = new Block(FabricBlockSettings.copyOf(Blocks.STONE)) {

        @Override
        public void onBlockAdded(final BlockState state, final World world, final BlockPos pos, final BlockState oldState, final boolean notify) {
            super.onBlockAdded(state, world, pos, oldState, notify);
            if (world instanceof AiWorldExtensions extensions) {
                final int x = pos.getX();
                final int y = pos.getY();
                final int z = pos.getZ();
                extensions.aiex$getAoiWorld().add(new BasicAreaOfInterest(pos), new AreaOfInterestBounds(x, y, z, x + 1, y + 1, z + 1));
            }
        }

        @Override
        public void onStateReplaced(final BlockState state, final World world, final BlockPos pos, final BlockState newState, final boolean moved) {
            super.onStateReplaced(state, world, pos, newState, moved);
            if (world instanceof AiWorldExtensions extensions) {
                final int x = pos.getX();
                final int y = pos.getY();
                final int z = pos.getZ();
                final AreaOfInterestBounds bounds = new AreaOfInterestBounds(x, y, z, x + 1, y + 1, z + 1);
                final List<AreaOfInterestEntry<BasicAreaOfInterest>> entries = extensions.aiex$getAoiWorld().intersecting(bounds, BASIC_AOI_TYPE).filter(entry -> entry.value().source().equals(pos)).toList();
                for (final AreaOfInterestEntry<BasicAreaOfInterest> entry : entries) {
                    extensions.aiex$getAoiWorld().remove(entry.reference());
                }
            }
        }
    };

    @Override
    public void onInitialize() {
        AiExTestEntities.init();
        Registry.register(AreaOfInterestType.REGISTRY, AiExCommon.id("basic"), BASIC_AOI_TYPE);
        Registry.register(Registries.BLOCK, AiExCommon.id("basic_aoi_test"), BASIC_AOI_BLOCK);
    }
}
