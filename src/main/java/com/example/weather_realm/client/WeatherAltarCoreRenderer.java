package com.example.weather_realm.client;

import com.example.weather_realm.ModParticles;
import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.block.WeatherAltarCoreBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the weather altar core as a translucent shell plus a floating, rotating inner crystal,
 * with a faint halo of blizzard snow particles.
 */
public class WeatherAltarCoreRenderer implements BlockEntityRenderer<WeatherAltarCoreBlockEntity> {
    private static final ModelResourceLocation SHELL_MODEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "block/weather_altar_core_shell"));
    private static final ModelResourceLocation INNER_MODEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "block/weather_altar_core_inner"));

    public WeatherAltarCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WeatherAltarCoreBlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partialTick;
        BlockState state = blockEntity.getBlockState();
        Minecraft minecraft = Minecraft.getInstance();
        BakedModel shell = minecraft.getModelManager().getModel(SHELL_MODEL);
        BakedModel inner = minecraft.getModelManager().getModel(INNER_MODEL);
        ModelBlockRenderer modelRenderer = minecraft.getBlockRenderer().getModelRenderer();

        VertexConsumer shellBuffer = bufferSource.getBuffer(RenderType.translucent());
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.scale(1.15F, 1.15F, 1.15F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        modelRenderer.renderModel(poseStack.last(), shellBuffer, state, shell, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay);
        poseStack.popPose();

        float bob = Mth.sin(time * 0.11F) * 0.08F;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.6D + bob, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.2F));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(time * 0.05F) * 14.0F));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        modelRenderer.renderModel(poseStack.last(), bufferSource.getBuffer(RenderType.translucent()), state, inner,
                1.0F, 1.0F, 1.0F, LightTexture.FULL_BRIGHT, packedOverlay);
        poseStack.popPose();

        spawnHalo(level, blockEntity, time);
    }

    private void spawnHalo(Level level, WeatherAltarCoreBlockEntity blockEntity, float time) {
        if (level.getGameTime() % 5L != 0L) {
            return;
        }
        RandomSource random = level.getRandom();
        double centerX = blockEntity.getBlockPos().getX() + 0.5D;
        double centerY = blockEntity.getBlockPos().getY() + 0.7D;
        double centerZ = blockEntity.getBlockPos().getZ() + 0.5D;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.75D;
        level.addParticle(ModParticles.BLIZZARD_SNOW.get(),
                centerX + Math.cos(angle) * radius,
                centerY + (random.nextDouble() - 0.5D) * 0.8D,
                centerZ + Math.sin(angle) * radius,
                0.0D, 0.03D, 0.0D);
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
