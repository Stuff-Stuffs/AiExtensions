package io.github.stuff_stuffs.aiex_test.common.aoi;

import com.mojang.serialization.Codec;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterest;
import io.github.stuff_stuffs.aiex.common.api.aoi.AreaOfInterestType;
import io.github.stuff_stuffs.aiex_test.common.AiExTestCommon;
import net.minecraft.util.math.BlockPos;

public class BasicAreaOfInterest implements AreaOfInterest {
    public static final Codec<BasicAreaOfInterest> CODEC = BlockPos.CODEC.xmap(BasicAreaOfInterest::new, aoi -> aoi.source);
    private final BlockPos source;

    public BasicAreaOfInterest(final BlockPos source) {
        this.source = source;
    }

    @Override
    public AreaOfInterestType<?> type() {
        return AiExTestCommon.BASIC_AOI_TYPE;
    }

    public BlockPos source() {
        return source;
    }
}
