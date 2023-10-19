package io.github.stuff_stuffs.aiex.common.mixin;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import io.github.stuff_stuffs.aiex.common.internal.InternalBlockExtensions;
import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public class MixinBlock implements InternalBlockExtensions {
    private final int aiex$uniqueId = AiExCommon.NEXT_BLOCK_ID.getAndIncrement();

    @Override
    public int aiex$uniqueId() {
        return aiex$uniqueId;
    }
}
