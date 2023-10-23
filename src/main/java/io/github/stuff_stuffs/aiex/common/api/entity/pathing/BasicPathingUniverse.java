package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai_pathing.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai_pathing.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.aiex.common.api.util.CollisionHelper;
import io.github.stuff_stuffs.aiex.common.api.util.tag.CombinedDenseBlockTagSet;
import io.github.stuff_stuffs.aiex.common.api.util.tag.DenseBlockTagSet;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public enum BasicPathingUniverse {
    BLOCKED(false, false, 1.0),
    OPEN(false, true, 1.0),
    FLOOR(true, false, 1.0),
    WATER(true, true, 6.0),
    LAVA(true, true, 128),
    DANGER(false, true, 24),
    DANGER_FLOOR(true, false, 24),
    PATH(true, false, 0.2),
    OPENABLE_DOOR(true, false, 2.0),
    LADDER(true, true, 3.0);
    public static final CombinedDenseBlockTagSet FLOOR_TAGS;
    public static final CombinedDenseBlockTagSet BOX_TAGS;

    static {
        final Set<TagKey<Block>> floorKeys = new ObjectOpenHashSet<>();
        final Set<TagKey<Block>> boxKeys = new ObjectOpenHashSet<>();
        for (final Flag flag : Flag.values()) {
            if (flag.floor) {
                floorKeys.add(flag.key);
            }
            if (flag.box) {
                boxKeys.add(flag.key);
            }
        }
        FLOOR_TAGS = CombinedDenseBlockTagSet.get(floorKeys);
        BOX_TAGS = CombinedDenseBlockTagSet.get(boxKeys);
    }

    public static final UniverseInfo<BasicPathingUniverse> UNIVERSE_INFO = UniverseInfo.fromEnum(BasicPathingUniverse.class);
    public static final CollisionHelper<Long, Long, BasicPathingUniverse> COLLISION_HELPER = new CollisionHelper<>(0.6, 1.8, 0L, 0L) {
        @Override
        protected @Nullable BasicPathingUniverse boxCollision(final Long box) {
            if (box < 0) {
                return BLOCKED;
            }
            if ((Flag.LAVA.mask & box) != 0) {
                return LAVA;
            }
            if ((Flag.WATER.mask & box) != 0) {
                return WATER;
            }
            if ((Flag.LADDER.mask & box) != 0) {
                return LADDER;
            }
            return null;
        }

        @Override
        protected BasicPathingUniverse bothCollision(final Long box, final Long floor) {
            if ((Flag.DOOR.mask & box) != 0) {
                return OPENABLE_DOOR;
            }
            if ((Flag.DANGER.mask & box) != 0 || (Flag.DANGER.mask & floor) != 0) {
                return floor < 0 ? DANGER_FLOOR : DANGER;
            }
            if ((Flag.PATH.mask & floor) != 0) {
                return PATH;
            }
            return floor < 0 ? FLOOR : OPEN;
        }

        @Override
        protected boolean shouldFloorReturnEarly(final Long state) {
            return false;
        }

        @Override
        protected Long updateFloorState(final Long old, final int bx, final int by, final int bz, final double x, final double y, final double z, final VoxelShape thisShape, final VoxelShape shape, final BlockState state, final ShapeCache world) {
            long l = old;
            final Block block = state.getBlock();
            if (FLOOR_TAGS.isInAny(block)) {
                if (Flag.DANGER.set.isIn(block)) {
                    l |= Flag.DANGER.mask;
                }
                if (Flag.PATH.set.isIn(block)) {
                    l |= Flag.PATH.mask;
                }
            }
            if (checkFloorCollision(box, thisShape, shape, bx, by, bz, x, y, z)) {
                return l | 0x8000000000000000L;
            }
            return l;
        }

        @Override
        protected boolean shouldReturnEarly(final Long state) {
            return state < 0 | (state & Flag.LADDER.mask) != 0;
        }

        @Override
        protected Long updateState(final Long old, final int bx, final int by, final int bz, final double x, final double y, final double z, final VoxelShape thisShape, final VoxelShape shape, final BlockState state, final ShapeCache world) {
            long l = old;
            final Block block = state.getBlock();
            if (BOX_TAGS.isInAny(block)) {
                if (Flag.DOOR.set.isIn(block)) {
                    l |= Flag.DOOR.mask;
                    return l;
                }
                if (Flag.LADDER.set.isIn(block) && !shape.isEmpty()) {
                    l |= Flag.LADDER.mask;
                    return l;
                }
                if (Flag.WATER.set.isIn(block)) {
                    l |= Flag.WATER.mask;
                }
                if (Flag.LAVA.set.isIn(block)) {
                    l |= Flag.LAVA.mask;
                }
                if (Flag.DANGER.set.isIn(block)) {
                    l |= Flag.DANGER.mask;
                }
            }
            if (checkBoxCollision(box, thisShape, shape, bx, by, bz, x, y, z)) {
                return l | 0x8000000000000000L;
            }
            return l;
        }
    };
    public static final LocationClassifier<BasicPathingUniverse> CLASSIFIER = new LocationClassifier<>() {
        @Override
        public BasicPathingUniverse get(final int x, final int y, final int z, final ShapeCache cache) {
            return COLLISION_HELPER.test(x, y, z, cache);
        }

        @Override
        public boolean needsRebuild(final int chunkSectionX, final int chunkSectionY, final int chunkSectionZ, final int otherChunkSectionX, final int otherChunkSectionY, final int otherChunkSectionZ, final int x, final int y, final int z, final ShapeCache cache, final BlockState oldState, final BlockState newState) {
            final int relX = x - chunkSectionX * 16;
            final int relY = y - chunkSectionY * 16;
            final int relZ = z - chunkSectionZ * 16;
            if (relY > 16 | relY < -1) {
                return false;
            }
            if (Flag.DOOR.set.isIn(oldState.getBlock()) && Flag.DOOR.set.isIn(newState.getBlock())) {
                return false;
            }
            if (relY == -1) {
                return true;
            }
            final boolean xAdj = (relX == -1 | relX == 16);
            final boolean zAdj = (relZ == -1 | relZ == 16);
            return xAdj | zAdj;
        }

        @Override
        public UniverseInfo<BasicPathingUniverse> universeInfo() {
            return UNIVERSE_INFO;
        }
    };

    public final boolean floor;
    public final boolean air;
    public final double costMultiplier;

    BasicPathingUniverse(final boolean floor, final boolean air, final double costMultiplier) {
        this.floor = floor;
        this.air = air;
        this.costMultiplier = costMultiplier;
    }

    private enum Flag {
        WATER(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_water")), false, true),
        LAVA(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_lava")), false, true),
        DANGER(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_danger")), true, true),
        PATH(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_path")), true, false),
        DOOR(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_door")), false, true),
        LADDER(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_ladder")), false, true);
        public final TagKey<Block> key;
        public final DenseBlockTagSet set;
        public final boolean floor;
        public final boolean box;
        public final long mask;

        Flag(final TagKey<Block> key, final boolean floor, final boolean box) {
            this.key = key;
            set = DenseBlockTagSet.get(key);
            this.floor = floor;
            this.box = box;
            mask = 1L << ordinal();
        }
    }
}
