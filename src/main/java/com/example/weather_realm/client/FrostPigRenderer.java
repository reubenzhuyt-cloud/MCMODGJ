package com.example.weather_realm.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pig;

/**
 * Renders the frost pig with the vanilla pig model and a thick frost-mane texture.
 */
public class FrostPigRenderer extends PigRenderer {
    public FrostPigRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Pig entity) {
        return FrostEntityTextures.PIG;
    }
}
