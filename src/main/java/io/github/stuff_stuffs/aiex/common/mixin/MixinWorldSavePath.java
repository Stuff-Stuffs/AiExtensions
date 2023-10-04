package io.github.stuff_stuffs.aiex.common.mixin;

import net.minecraft.util.WorldSavePath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(WorldSavePath.class)
public interface MixinWorldSavePath {
    @Invoker(value = "<init>")
    static WorldSavePath callInit(final String path) {
        throw new AssertionError();
    }
}
