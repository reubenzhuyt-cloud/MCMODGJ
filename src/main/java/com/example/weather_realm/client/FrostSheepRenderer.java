package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.client.model.SheepModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;

/**
 * Renders the frost sheep with a recoloured icy body and a custom frost fur layer.
 */
public class FrostSheepRenderer extends MobRenderer<Sheep, SheepModel<Sheep>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "textures/entity/frost_sheep.png");

    public FrostSheepRenderer(EntityRendererProvider.Context context) {
        super(context, new SheepModel<>(context.bakeLayer(ModelLayers.SHEEP)), 0.7F);
        this.addLayer(new FrostSheepFurLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(Sheep entity) {
        return TEXTURE;
    }
}
