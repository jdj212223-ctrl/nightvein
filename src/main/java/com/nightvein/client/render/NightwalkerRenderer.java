package com.nightvein.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nightvein.Nightvein;
import com.nightvein.entity.Nightwalker;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class NightwalkerRenderer extends HumanoidMobRenderer<Nightwalker, HumanoidModel<Nightwalker>> {
    private static final ResourceLocation TEXTURE = Nightvein.id("textures/entity/nightwalker.png");
    private static final ResourceLocation EYES = Nightvein.id("textures/entity/nightwalker_eyes.png");

    public NightwalkerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
        addLayer(new Eyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Nightwalker entity) {
        return TEXTURE;
    }

    /** Eyes that catch the light when there isn't any. */
    private static final class Eyes extends RenderLayer<Nightwalker, HumanoidModel<Nightwalker>> {
        Eyes(RenderLayerParent<Nightwalker, HumanoidModel<Nightwalker>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, Nightwalker e, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (e.isInvisible()) return;
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(EYES)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }
}
