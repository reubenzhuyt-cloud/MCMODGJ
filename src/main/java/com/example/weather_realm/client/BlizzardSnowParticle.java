package com.example.weather_realm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * A delicate, chaotically swirling snow flake used for the permanent blizzard in the Glacial
 * Realm. Flakes swirl in 3D, fall with a gentle downward bias, vanish on contact, and fade out
 * as they approach the camera. Client-only.
 */
public class BlizzardSnowParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    // Random turbulence seeds so every flake swirls on its own path.
    private final float phaseX;
    private final float phaseZ;
    private final float swirlSpeed;

    protected BlizzardSnowParticle(ClientLevel level, double x, double y, double z,
            double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.sprites = sprites;
        // Delicate snowflake size (roughly 1/2 - 1/3 of the previous scale).
        this.quadSize = 0.08F + this.random.nextFloat() * 0.05F;
        this.lifetime = 40 + this.random.nextInt(40);
        this.gravity = 0.0F;
        this.friction = 0.98F;
        this.hasPhysics = true; // collide with ground / roofs so flakes never clip into interiors

        this.phaseX = this.random.nextFloat() * Mth.TWO_PI;
        this.phaseZ = this.random.nextFloat() * Mth.TWO_PI;
        this.swirlSpeed = 0.7F + this.random.nextFloat() * 0.6F;

        // Gentle breeze plus a small random kick; overall fall handled in tick().
        this.xd = xSpeed * 0.3 + (this.random.nextFloat() - 0.5F) * 0.05F;
        this.yd = -0.08F - this.random.nextFloat() * 0.08F;
        this.zd = zSpeed * 0.3 + (this.random.nextFloat() - 0.5F) * 0.05F;
        this.setAlpha(0.9F);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        // Vanish immediately on contact with any solid block, roof, or the ground.
        if (this.onGround || this.removed) {
            this.remove();
            return;
        }

        // Chaotic 3D turbulence so the flakes fly around wildly ("胡乱飞舞").
        this.xd += Math.sin(this.age * 0.15 * this.swirlSpeed + this.phaseX) * 0.035
                + (this.random.nextFloat() - 0.5F) * 0.02;
        this.zd += Math.cos(this.age * 0.12 * this.swirlSpeed + this.phaseZ) * 0.035
                + (this.random.nextFloat() - 0.5F) * 0.02;
        this.yd += (this.random.nextFloat() - 0.5F) * 0.015;
        // Keep an overall downward bias between -0.16 and -0.08.
        this.yd = Mth.clamp(this.yd, -0.16, -0.08);

        // Fade out as the flake approaches the camera so it never smears the screen.
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer != null && minecraft.gameRenderer.getMainCamera().isInitialized()) {
            Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
            double dx = this.x - camera.x;
            double dy = this.y - camera.y;
            double dz = this.z - camera.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance < 2.0) {
                float fade = (float) Math.max(0.0, (distance - 0.4) / 1.6);
                this.setAlpha(fade * 0.9F);
            } else {
                this.setAlpha(0.9F);
            }
        }

        this.setSpriteFromAge(this.sprites);
    }

    /** Provider that binds the sprite set to the particle. Registered on the mod event bus (client). */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public TextureSheetParticle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new BlizzardSnowParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
