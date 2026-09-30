package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.SheepFurModel;
import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.SheepFurLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;

/**
 * A {@link SheepFurLayer} variant that renders the glacial fleece with the mod's recoloured fur
 * texture instead of vanilla {@code sheep_fur.png}.
 */
public class FrostSheepFurLayer extends SheepFurLayer {
    private static final ResourceLocation FUR_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "textures/entity/frost_sheep_fur.png");

    private final SheepFurModel<Sheep> frostModel;

    public FrostSheepFurLayer(RenderLayerParent<Sheep, SheepModel<Sheep>> parent, EntityModelSet modelSet) {
        super(parent, modelSet);
        this.frostModel = new SheepFurModel<>(modelSet.bakeLayer(ModelLayers.SHEEP_FUR));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Sheep entity,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (!entity.isSheared()) {
            coloredCutoutModelCopyLayerRender(this.getParentModel(), this.frostModel, FUR_TEXTURE,
                    poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, ageInTicks,
                    netHeadYaw, headPitch, partialTicks, Sheep.getColor(entity.getColor()));
        }
    }
}
