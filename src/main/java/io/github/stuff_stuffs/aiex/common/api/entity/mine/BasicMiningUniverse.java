package io.github.stuff_stuffs.aiex.common.api.entity.mine;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.aiex.common.api.util.tag.DenseBlockTagSet;
import net.fabricmc.fabric.api.mininglevel.v1.FabricMineableTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public enum BasicMiningUniverse {
    NONE,
    AXE(TagKey.of(RegistryKeys.BLOCK, new Identifier("minecraft", "mineable/axe")), true),
    PICKAXE(TagKey.of(RegistryKeys.BLOCK, new Identifier("minecraft", "mineable/pickaxe")), true),
    SHOVEL(TagKey.of(RegistryKeys.BLOCK, new Identifier("minecraft", "mineable/shovel")), false),
    HOE(TagKey.of(RegistryKeys.BLOCK, new Identifier("minecraft", "mineable/hoe")), false),
    SHEARS(FabricMineableTags.SHEARS_MINEABLE, false),
    SWORD(FabricMineableTags.SWORD_MINEABLE, true);
    public static final UniverseInfo<BasicMiningUniverse> UNIVERSE_INFO = UniverseInfo.fromEnum(BasicMiningUniverse.class);
    public static final LocationClassifier<BasicMiningUniverse> CLASSIFIER = new LocationClassifier<>() {
        private static final BasicMiningUniverse[] EXCEPT_NONE = new BasicMiningUniverse[]{AXE, PICKAXE, SHOVEL, SHEARS, SWORD, HOE};

        @Override
        public BasicMiningUniverse get(final int x, final int y, final int z, final ShapeCache cache) {
            final Block block = cache.getBlockState(x, y, z).getBlock();
            for (final BasicMiningUniverse universe : EXCEPT_NONE) {
                if (universe.isIn(block)) {
                    return universe;
                }
            }
            return NONE;
        }

        @Override
        public boolean needsRebuild(final int chunkSectionX, final int chunkSectionY, final int chunkSectionZ, final int otherChunkSectionX, final int otherChunkSectionY, final int otherChunkSectionZ, final int x, final int y, final int z, final ShapeCache cache, final BlockState oldState, final BlockState newState) {
            final int relX = x - chunkSectionX * 16;
            final int relY = y - chunkSectionY * 16;
            final int relZ = z - chunkSectionZ * 16;
            if (relX < 0 | relY < 0 | relZ < 0) {
                return false;
            } else if (relX > 15 | relY > 15 | relZ > 15) {
                return false;
            }
            return true;
        }

        @Override
        public UniverseInfo<BasicMiningUniverse> universeInfo() {
            return UNIVERSE_INFO;
        }
    };
    private final TagKey<Block> tag;
    private final DenseBlockTagSet tagSet;
    public final boolean needed;


    BasicMiningUniverse() {
        tag = null;
        tagSet = null;
        needed = false;
    }

    BasicMiningUniverse(final TagKey<Block> tag, final boolean needed) {
        this.tag = tag;
        tagSet = DenseBlockTagSet.get(tag);
        this.needed = needed;
    }

    public boolean isIn(final Block block) {
        return tagSet == null || tagSet.isIn(block);
    }
}
