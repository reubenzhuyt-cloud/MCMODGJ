package com.example.weather_realm.client;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pig;

/**
 * Renders the frost pig with the vanilla pig model and a thick frost-mane texture.
 */
public class FrostPigRenderer extends PigRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "textures/entity/frost_pig.png");

    public FrostPigRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Pig entity) {
        return TEXTURE;
    }
}
