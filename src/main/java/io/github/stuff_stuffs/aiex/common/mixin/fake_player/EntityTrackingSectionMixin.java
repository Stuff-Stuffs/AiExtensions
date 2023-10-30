package io.github.stuff_stuffs.aiex.common.mixin.fake_player;

import io.github.stuff_stuffs.aiex.common.api.entity.AiEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.collection.TypeFilterableList;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(EntityTrackingSection.class)
public class EntityTrackingSectionMixin<T extends EntityLike> {
    @Shadow
    @Final
    private TypeFilterableList<T> collection;


    @Inject(
            method = "forEach(Lnet/minecraft/util/TypeFilter;Lnet/minecraft/util/math/Box;Lnet/minecraft/util/function/LazyIterationConsumer;)Lnet/minecraft/util/function/LazyIterationConsumer$NextIteration;",
            at = @At("RETURN"),
            cancellable = true
    )
    private <U extends T> void removeDelegateHook(final TypeFilter<T, U> type, final Box box, final LazyIterationConsumer<? super U> consumer, final CallbackInfoReturnable<LazyIterationConsumer.NextIteration> cir) {
        final LazyIterationConsumer.NextIteration iteration = cir.getReturnValue();
        if (iteration == LazyIterationConsumer.NextIteration.CONTINUE && type.getBaseClass() == PlayerEntity.class) {
            final Collection<AiEntity> all = collection.getAllOfType(AiEntity.class);
            if (all.isEmpty()) {
                return;
            }
            for (final AiEntity entity : all) {
                if (entity.aiex$getBrain().hasFakePlayerDelegate()) {
                    final ServerPlayerEntity delegate = entity.aiex$getBrain().fakePlayerDelegate();
                    final U player = type.downcast((T) delegate);
                    if (player != null && delegate.getBoundingBox().intersects(box) && consumer.accept(player).shouldAbort()) {
                        cir.setReturnValue(LazyIterationConsumer.NextIteration.ABORT);
                        return;
                    }
                }
            }
        }
    }
}
