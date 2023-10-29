package io.github.stuff_stuffs.aiex.common.api.entity.mine;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.aiex.common.api.util.tag.DenseRefSet;
import net.fabricmc.fabric.api.mininglevel.v1.FabricMineableTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.BlockPos;

public enum BasicMiningUniverse {
    NONE,
    AXE(BlockTags.AXE_MINEABLE, true),
    PICKAXE(BlockTags.PICKAXE_MINEABLE, true),
    SHOVEL(BlockTags.SHOVEL_MINEABLE, false),
    HOE(BlockTags.HOE_MINEABLE, false),
    SHEARS(FabricMineableTags.SHEARS_MINEABLE, false),
    SWORD(FabricMineableTags.SWORD_MINEABLE, true),
    SWORD_EFFICIENT(BlockTags.SWORD_EFFICIENT, false);
    public static final UniverseInfo<BasicMiningUniverse> UNIVERSE_INFO = UniverseInfo.fromEnum(BasicMiningUniverse.class);
    public static final LocationClassifier<BasicMiningUniverse> CLASSIFIER = new LocationClassifier<>() {
        private static final BasicMiningUniverse[] EXCEPT_NONE = new BasicMiningUniverse[]{AXE, PICKAXE, SHOVEL, SHEARS, SWORD, HOE, SWORD_EFFICIENT};

        @Override
        public BasicMiningUniverse get(final int x, final int y, final int z, final ShapeCache cache) {
            final Block block = cache.getBlockState(x, y, z).getBlock();
            return get(block);
        }

        private BasicMiningUniverse get(final Block block) {
            for (final BasicMiningUniverse universe : EXCEPT_NONE) {
                if (universe.isIn(block)) {
                    return universe;
                }
            }
            return NONE;
        }

        @Override
        public boolean needsRebuild(final int chunkSectionX, final int chunkSectionY, final int chunkSectionZ, final int otherChunkSectionX, final int otherChunkSectionY, final int otherChunkSectionZ, final int x, final int y, final int z, final ShapeCache cache, final BlockState oldState, final BlockState newState, final BlockPos.Mutable scratch) {
            final int relX = x - chunkSectionX * 16;
            final int relY = y - chunkSectionY * 16;
            final int relZ = z - chunkSectionZ * 16;
            if (relX < 0 | relY < 0 | relZ < 0) {
                return false;
            } else if (relX > 15 | relY > 15 | relZ > 15) {
                return false;
            }
            final Block oldBlock = oldState.getBlock();
            final Block newBlock = newState.getBlock();
            return oldBlock != newBlock && get(oldBlock) != get(newBlock);
        }

        @Override
        public UniverseInfo<BasicMiningUniverse> universeInfo() {
            return UNIVERSE_INFO;
        }
    };
    private final DenseRefSet<Block> tagSet;
    public final boolean needed;


    BasicMiningUniverse() {
        tagSet = null;
        needed = false;
    }

    BasicMiningUniverse(final TagKey<Block> tag, final boolean needed) {
        tagSet = DenseRefSet.ofBlockTag(tag);
        this.needed = needed;
    }

    public boolean isIn(final Block block) {
        return tagSet == null || tagSet.isIn(block);
    }
}
