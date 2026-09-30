package com.example.weather_realm.client;

import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;

/**
 * Renders the frost cat with the vanilla cat model and a silver snow-leopard texture.
 */
public class FrostCatRenderer extends CatRenderer {
    public FrostCatRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Cat entity) {
        return FrostEntityTextures.CAT;
    }
}
