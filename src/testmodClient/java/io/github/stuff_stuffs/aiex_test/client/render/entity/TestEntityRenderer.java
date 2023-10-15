package io.github.stuff_stuffs.aiex_test.client.render.entity;

import io.github.stuff_stuffs.aiex_test.common.entity.TestEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.*;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.UseAction;

public class TestEntityRenderer<T extends TestEntity, M extends PlayerEntityModel<T>> extends LivingEntityRenderer<T, M> {
    private static final Identifier[] SKINS_SLIM = new Identifier[]{
            new Identifier("textures/entity/player/slim/alex.png"),
            new Identifier("textures/entity/player/slim/ari.png"),
            new Identifier("textures/entity/player/slim/efe.png"),
            new Identifier("textures/entity/player/slim/kai.png"),
            new Identifier("textures/entity/player/slim/makena.png"),
            new Identifier("textures/entity/player/slim/noor.png"),
            new Identifier("textures/entity/player/slim/steve.png"),
            new Identifier("textures/entity/player/slim/sunny.png"),
            new Identifier("textures/entity/player/slim/zuri.png")
    };
    private static final Identifier[] SKINS_WIDE = new Identifier[]{
            new Identifier("textures/entity/player/wide/alex.png"),
            new Identifier("textures/entity/player/wide/ari.png"),
            new Identifier("textures/entity/player/wide/efe.png"),
            new Identifier("textures/entity/player/wide/kai.png"),
            new Identifier("textures/entity/player/wide/makena.png"),
            new Identifier("textures/entity/player/wide/noor.png"),
            new Identifier("textures/entity/player/wide/steve.png"),
            new Identifier("textures/entity/player/wide/sunny.png"),
            new Identifier("textures/entity/player/wide/zuri.png")
    };
    private final M thickModel;
    private final M slimModel;

    public static TestEntityRenderer<TestEntity, PlayerEntityModel<TestEntity>> createBasic(final EntityRendererFactory.Context ctx) {
        return new TestEntityRenderer<>(ctx, new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false), new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    public TestEntityRenderer(final EntityRendererFactory.Context ctx, final M thickModel, final M slimModel, final float shadowRadius) {
        super(ctx, thickModel, shadowRadius);
        this.thickModel = thickModel;
        this.slimModel = slimModel;
        final ModelPart innerArmor = ctx.getPart(EntityModelLayers.PLAYER_INNER_ARMOR);
        final ModelPart outerArmor = ctx.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR);
        final ModelPart slimInnerArmor = ctx.getPart(EntityModelLayers.PLAYER_SLIM_INNER_ARMOR);
        final ModelPart slimOuterArmor = ctx.getPart(EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR);
        addFeature(new BiArmorFeatureRenderer<>(
                        this,
                        new ArmorEntityModel<>(innerArmor),
                        new ArmorEntityModel<>(outerArmor),
                        new ArmorEntityModel<>(slimInnerArmor),
                        new ArmorEntityModel<>(slimOuterArmor),
                        ctx.getModelManager(),
                        TestEntity::slim
                )
        );
        addFeature(new HeadFeatureRenderer<>(this, ctx.getModelLoader(), 1.0F, 1.0F, 1.0F, ctx.getHeldItemRenderer()));
        addFeature(new ElytraFeatureRenderer<>(this, ctx.getModelLoader()));
        addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
        addFeature(new StuckArrowsFeatureRenderer<>(ctx, this));
        addFeature(new StuckStingersFeatureRenderer<>(this));
    }

    @Override
    public Identifier getTexture(final T entity) {
        final Identifier[] skins = entity.slim() ? SKINS_SLIM : SKINS_WIDE;
        return skins[Math.floorMod(entity.getUuid().hashCode(), skins.length)];
    }

    @Override
    public void render(final T livingEntity, final float f, final float g, final MatrixStack matrixStack, final VertexConsumerProvider vertexConsumerProvider, final int i) {
        if (livingEntity.slim()) {
            model = slimModel;
        } else {
            model = thickModel;
        }
        setModelPose(livingEntity);
        super.render(livingEntity, f, g, matrixStack, vertexConsumerProvider, i);
    }

    protected void setModelPose(final T entity) {
        final M model = getModel();
        model.setVisible(true);
        model.sneaking = entity.isInSneakingPose();
        final BipedEntityModel.ArmPose mainArmPose = getArmPose(entity, Hand.MAIN_HAND);
        BipedEntityModel.ArmPose offArmPose = getArmPose(entity, Hand.OFF_HAND);
        if (mainArmPose.isTwoHanded()) {
            offArmPose = entity.getOffHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
        }

        if (entity.getMainArm() == Arm.RIGHT) {
            model.rightArmPose = mainArmPose;
            model.leftArmPose = offArmPose;
        } else {
            model.rightArmPose = offArmPose;
            model.leftArmPose = mainArmPose;
        }
    }

    protected BipedEntityModel.ArmPose getArmPose(final T player, final Hand hand) {
        final ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isEmpty()) {
            return BipedEntityModel.ArmPose.EMPTY;
        } else {
            if (player.getActiveHand() == hand && player.getItemUseTimeLeft() > 0) {
                final UseAction useAction = itemStack.getUseAction();
                if (useAction == UseAction.BLOCK) {
                    return BipedEntityModel.ArmPose.BLOCK;
                }

                if (useAction == UseAction.BOW) {
                    return BipedEntityModel.ArmPose.BOW_AND_ARROW;
                }

                if (useAction == UseAction.SPEAR) {
                    return BipedEntityModel.ArmPose.THROW_SPEAR;
                }

                if (useAction == UseAction.CROSSBOW && hand == player.getActiveHand()) {
                    return BipedEntityModel.ArmPose.CROSSBOW_CHARGE;
                }

                if (useAction == UseAction.SPYGLASS) {
                    return BipedEntityModel.ArmPose.SPYGLASS;
                }

                if (useAction == UseAction.TOOT_HORN) {
                    return BipedEntityModel.ArmPose.TOOT_HORN;
                }

                if (useAction == UseAction.BRUSH) {
                    return BipedEntityModel.ArmPose.BRUSH;
                }
            } else if (!player.handSwinging && itemStack.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(itemStack)) {
                return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
            }

            return BipedEntityModel.ArmPose.ITEM;
        }
    }

    @Override
    protected boolean hasLabel(final T livingEntity) {
        return true;
    }
}
