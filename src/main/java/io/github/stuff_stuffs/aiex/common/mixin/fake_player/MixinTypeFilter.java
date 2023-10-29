package io.github.stuff_stuffs.aiex.common.mixin.fake_player;

import io.github.stuff_stuffs.aiex.common.internal.AiExCommon;
import net.minecraft.util.TypeFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TypeFilter.class)
public interface MixinTypeFilter {
    /**
     * @author Stuff-Stuffs
     * @reason Need to catch entities with player delegate
     */
    @Overwrite
    static <B, T extends B> TypeFilter<B, T> instanceOf(final Class<T> cls) {
        return AiExCommon.createDelegatingTypeFilter(cls);
    }
}
