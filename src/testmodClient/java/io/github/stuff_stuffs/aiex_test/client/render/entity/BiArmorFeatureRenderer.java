package io.github.stuff_stuffs.aiex_test.client.render.entity;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

import java.util.function.Predicate;

public class BiArmorFeatureRenderer<T extends LivingEntity, M extends BipedEntityModel<T>, A extends BipedEntityModel<T>> extends FeatureRenderer<T, M> {
    private final Predicate<? super T> slimPredicate;
    private final ArmorFeatureRenderer<T, M, A> thick;
    private final ArmorFeatureRenderer<T, M, A> slim;

    public BiArmorFeatureRenderer(final FeatureRendererContext<T, M> context, final A innerModel, final A outerModel, final A slimInnerModel, final A slimOuterModel, final BakedModelManager bakery, final Predicate<? super T> slimPredicate) {
        super(context);
        this.slimPredicate = slimPredicate;
        thick = new ArmorFeatureRenderer<>(context, innerModel, outerModel, bakery);
        slim = new ArmorFeatureRenderer<>(context, slimInnerModel, slimOuterModel, bakery);
    }

    @Override
    public void render(final MatrixStack matrices, final VertexConsumerProvider vertexConsumers, final int light, final T entity, final float limbAngle, final float limbDistance, final float tickDelta, final float animationProgress, final float headYaw, final float headPitch) {
        (slimPredicate.test(entity) ? slim : thick).render(matrices, vertexConsumers, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
    }
}
