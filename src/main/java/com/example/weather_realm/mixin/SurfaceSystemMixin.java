package com.example.weather_realm.mixin;

import com.example.weather_realm.world.ModSurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SurfaceSystem.class)
public abstract class SurfaceSystemMixin {
    @ModifyVariable(
        method = "buildSurface",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private SurfaceRules.RuleSource weather_realm$injectModSurfaceRules(SurfaceRules.RuleSource original) {
        return ModSurfaceRules.wrapSurfaceRules(original);
    }
}
