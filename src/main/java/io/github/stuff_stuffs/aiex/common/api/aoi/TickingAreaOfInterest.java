package io.github.stuff_stuffs.aiex.common.api.aoi;

import net.minecraft.world.World;

public interface TickingAreaOfInterest extends AreaOfInterest {
    void tick(World world, AreaOfInterestBounds bounds, AreaOfInterestReference<?> thisReference);
}
