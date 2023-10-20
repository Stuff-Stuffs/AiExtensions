package io.github.stuff_stuffs.aiex.common.api.entity.pathing;

import io.github.stuff_stuffs.advanced_ai.common.api.pathing.location_caching.LocationClassifier;
import io.github.stuff_stuffs.advanced_ai.common.api.util.ShapeCache;
import io.github.stuff_stuffs.advanced_ai.common.api.util.UniverseInfo;
import io.github.stuff_stuffs.aiex.common.api.util.tag.CombinedDenseBlockTagSet;
import io.github.stuff_stuffs.aiex.common.api.util.tag.DenseBlockTagSet;
import io.github.stuff_stuffs.aiex.common.api.util.FlaggedCollisionHelper;
import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public enum BasicPathingUniverse {
    BLOCKED(false, false, 1.0),
    OPEN(false, true, 1.0),
    FLOOR(true, false, 1.0),
    WATER(true, true, 1.5),
    LAVA(true, true, 128),
    DANGER(false, true, 24),
    DANGER_FLOOR(true, false, 24),
    PATH(true, false, 0.2),
    OPENABLE_DOOR(true, false, 2.0);
    public static final DenseBlockTagSet DANGER_DENSE_SET = DenseBlockTagSet.get(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_danger")));
    public static final DenseBlockTagSet WATER_DENSE_SET = DenseBlockTagSet.get(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_water")));
    public static final DenseBlockTagSet LAVA_DENSE_SET = DenseBlockTagSet.get(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_lava")));
    public static final DenseBlockTagSet PATH_DENSE_SET = DenseBlockTagSet.get(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_path")));
    public static final DenseBlockTagSet OPENABLE_DOOR_DENSE_SET = DenseBlockTagSet.get(TagKey.of(RegistryKeys.BLOCK, AiExCommon.id("npc_door")));
    public static final CombinedDenseBlockTagSet FLOOR_TAGS = CombinedDenseBlockTagSet.get(Set.of(DANGER_DENSE_SET.key(), PATH_DENSE_SET.key()));
    public static final CombinedDenseBlockTagSet BOX_TAGS = CombinedDenseBlockTagSet.get(Set.of(LAVA_DENSE_SET.key(), DANGER_DENSE_SET.key(), OPENABLE_DOOR_DENSE_SET.key(), WATER_DENSE_SET.key()));
    public static final UniverseInfo<BasicPathingUniverse> UNIVERSE_INFO = UniverseInfo.fromEnum(BasicPathingUniverse.class);
    public static final LocationClassifier<BasicPathingUniverse> CLASSIFIER = new LocationClassifier<>() {
        private final FlaggedCollisionHelper<Flag, BasicPathingUniverse> helper = new FlaggedCollisionHelper<>(0.6, 1.8, OPEN, BLOCKED, FLOOR) {
            @Override
            protected BasicPathingUniverse combineWithOpen(final Flag flag) {
                return switch (flag) {
                    case WATER -> WATER;
                    case LAVA -> LAVA;
                    case DANGER -> DANGER;
                    case PATH -> OPEN;
                    case DOOR -> OPENABLE_DOOR;
                };
            }

            @Override
            protected BasicPathingUniverse combineWithClose(final Flag flag) {
                return BLOCKED;
            }

            @Override
            protected BasicPathingUniverse combineWithFloor(final Flag flag) {
                return switch (flag) {
                    case WATER -> WATER;
                    case LAVA -> LAVA;
                    case DANGER -> DANGER_FLOOR;
                    case PATH -> PATH;
                    case DOOR -> OPENABLE_DOOR;
                };
            }

            @Override
            protected @Nullable Flag testFloorState(final BlockState state, final int x, final int y, final int z, final ShapeCache world) {
                final Block block = state.getBlock();
                if (FLOOR_TAGS.isInAny(block)) {
                    if (DANGER_DENSE_SET.isIn(block)) {
                        return Flag.DANGER;
                    }
                    if (PATH_DENSE_SET.isIn(block)) {
                        return Flag.PATH;
                    }
                }
                return null;
            }

            @Override
            protected @Nullable Flag testBoxState(final BlockState state, final int x, final int y, final int z, final ShapeCache world) {
                final Block block = state.getBlock();
                if (BOX_TAGS.isInAny(block)) {
                    if (LAVA_DENSE_SET.isIn(block)) {
                        return Flag.LAVA;
                    }
                    if (DANGER_DENSE_SET.isIn(block)) {
                        return Flag.DANGER;
                    }
                    if (OPENABLE_DOOR_DENSE_SET.isIn(block)) {
                        return Flag.DOOR;
                    }
                    if (WATER_DENSE_SET.isIn(block)) {
                        return Flag.WATER;
                    }
                }
                return null;
            }

            @Override
            protected Flag combineFlag(final Flag oldFlag, final Flag newFlag) {
                return switch (oldFlag) {
                    case PATH, WATER -> newFlag;
                    case DOOR -> switch (newFlag) {
                        case PATH, DOOR, WATER -> Flag.DOOR;
                        case DANGER -> Flag.DANGER;
                        case LAVA -> Flag.LAVA;
                    };
                    case LAVA -> Flag.LAVA;
                    case DANGER -> Flag.DANGER;
                };
            }
        };

        @Override
        public BasicPathingUniverse get(final int x, final int y, final int z, final ShapeCache cache) {
            return helper.test(x, y, z, cache);
        }

        @Override
        public boolean needsRebuild(final int chunkSectionX, final int chunkSectionY, final int chunkSectionZ, final int otherChunkSectionX, final int otherChunkSectionY, final int otherChunkSectionZ, final int x, final int y, final int z, final ShapeCache cache, final BlockState oldState, final BlockState newState) {
            if (x > 16 | y < -1) {
                return false;
            }
            if (y == -1) {
                return true;
            }
            final boolean xAdj = (x == -1 | x == 16);
            final boolean zAdj = (z == -1 | z == 16);
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
        WATER,
        LAVA,
        DANGER,
        PATH,
        DOOR
    }
}
