package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cow;

/**
 * Renders the frost cow with a recoloured icy hide.
 */
public class FrostCowRenderer extends CowRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "textures/entity/frost_cow.png");

    public FrostCowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Cow entity) {
        return TEXTURE;
    }
}
