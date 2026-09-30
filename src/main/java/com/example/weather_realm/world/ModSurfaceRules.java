package com.example.weather_realm.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.weather_realm.ModBlocks;
import com.example.weather_realm.WeatherRealm;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

/**
 * Additional surface rules injected in front of the vanilla overworld rules so that each of the
 * Glacial Realm's climate biomes gets its own surface material.
 *
 * <p>Business logic lives here (not in the mixin) so it can be hot-swapped; the mixin only
 * delegates to {@link #wrapSurfaceRules(SurfaceRules.RuleSource)}.</p>
 *
 * <p><b>Full-column stone replacement</b>: the old {@code ON_FLOOR}/{@code UNDER_FLOOR} depth limits
 * have been removed on purpose for the stone layers. For every stone block the surface system visits
 * (which is the whole column, not just the top few layers) the biome rule now wins, so an entire
 * mountain and the bedrock-to-surface underground are converted in one pass. That is what lets the
 * full permafrost / deep-permafrost ore suite generate against our own replaceables tags.</p>
 *
 * <p><b>Noisy deep/shallow transition</b>: the deep/shallow stone split is not a hard cut at y=0 but
 * a vanilla-style noisy gradient ({@link SurfaceRules#verticalGradient}) that is deep stone at or
 * below y=0, shallow stone at or above y=8, and a per-block noise blend in between.</p>
 *
 * <p><b>Topsoil guard</b>: {@code ON_FLOOR} is true for any solid block that has at most one solid
 * block above it, so on its own it would also skin cave floors and the undersides of overhangs (the
 * surface system evaluates the entire column, top to {@code minBuildHeight}). The topsoil branch is
 * therefore additionally wrapped in {@link SurfaceRules#abovePreliminarySurface()}, exactly how
 * vanilla {@code SurfaceRuleData.overworld()} keeps its grass/sand/dirt layers on the true surface.
 * The full-column stone branches are intentionally <em>not</em> wrapped, so cave walls stay stone
 * and can still host ore veins.</p>
 *
 * <p>The remaining vertical guard is {@link #ABOVE_BEDROCK}: it keeps the vanilla bedrock floor
 * (which lives in the wrapped {@code original} rules) intact so the bottom of the world does not
 * turn into permafrost. Vanilla builds its bedrock floor from the bottom five layers, so our rules
 * simply do not claim anything at or below that band.</p>
 */
public final class ModSurfaceRules {
    private ModSurfaceRules() {
    }

    /** 极寒群系 / Frozen biome. */
    public static final ResourceKey<Biome> CRYSTAL_PLAINS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "crystal_plains"));

    /** 炎热群系 / Scorching biome. */
    public static final ResourceKey<Biome> BLAZING_PLAINS = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "blazing_plains"));

    /** 干旱风沙群系 / Arid wasteland biome. */
    public static final ResourceKey<Biome> ARID_WASTELAND = ResourceKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "arid_wasteland"));

    /**
     * 基岩层之上判定 / True for every y at or above the vanilla bedrock floor band (bottom five
     * layers). Keeps {@code minecraft:bedrock_floor} working instead of smearing permafrost to y=min.
     */
    private static final SurfaceRules.ConditionSource ABOVE_BEDROCK =
            SurfaceRules.yBlockCheck(VerticalAnchor.aboveBottom(5), 0);

    /**
     * 原版 deepslate 式噪声过渡 / Vanilla-style noisy deepslate gradient: deep stone at/below y=0,
     * shallow stone at/above y=8, noisy blend in between. Reuses vanilla's random name "deepslate"
     * (-> minecraft:deepslate) so the transition is byte-identical to vanilla's deepslate boundary.
     */
    private static final SurfaceRules.ConditionSource DEEP_GRADIENT =
            SurfaceRules.verticalGradient("deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(8));

    /** 浅层判定(DEEP_GRADIENT 的补集)/ Shallow stone; complement of {@link #DEEP_GRADIENT}. */
    private static final SurfaceRules.ConditionSource SHALLOW_GRADIENT = SurfaceRules.not(DEEP_GRADIENT);

    /**
     * Memoised {@code original -> prepended} rule trees, keyed by the identity of the incoming rule
     * source. Vanilla passes the very same {@link SurfaceRules.RuleSource} instance for every chunk
     * of a dimension (it is held by the noise generator settings), so without this cache the tree
     * would be rebuilt for every single column.
     */
    private static final Map<SurfaceRules.RuleSource, SurfaceRules.RuleSource> WRAPPED_RULES =
            new ConcurrentHashMap<>();

    /** The three-biome rule group, built lazily on first worldgen use. */
    private static volatile SurfaceRules.RuleSource MOD_RULES;

    /**
     * Prepends the mod's per-biome surface rules to the original (vanilla overworld) rules, so mod
     * rules win when their biome condition matches and vanilla rules handle everything else.
     *
     * <p>The result is memoised per incoming rule source, and the three-biome group itself is built
     * only once. Vanilla invokes the surface builder for every chunk (and every column), so this
     * keeps the rule-tree construction off the worldgen hot path.</p>
     */
    public static SurfaceRules.RuleSource wrapSurfaceRules(SurfaceRules.RuleSource original) {
        if (original == null) {
            return modRules();
        }
        return WRAPPED_RULES.computeIfAbsent(original, ModSurfaceRules::prependModRules);
    }

    private static SurfaceRules.RuleSource prependModRules(SurfaceRules.RuleSource original) {
        return SurfaceRules.sequence(modRules(), original);
    }

    /**
     * Lazily builds and memoises the three-biome rule group. First construction happens during
     * worldgen, after the deferred registers have been populated, so dereferencing
     * {@code ModBlocks.*.get()} here can never observe a not-yet-created holder (doing it in a
     * static field initialiser would risk exactly that).
     */
    private static SurfaceRules.RuleSource modRules() {
        SurfaceRules.RuleSource rules = MOD_RULES;
        if (rules == null) {
            synchronized (ModSurfaceRules.class) {
                rules = MOD_RULES;
                if (rules == null) {
                    rules = createModRules();
                    MOD_RULES = rules;
                }
            }
        }
        return rules;
    }

    private static SurfaceRules.RuleSource createModRules() {
        return SurfaceRules.sequence(
                createCrystalPlainsRules(),
                createBlazingPlainsRules(),
                createAridWastelandRules());
    }

    /**
     * 极寒群系全柱替换 / Frozen biome, full-column swap: the top floor block becomes
     * {@code frost_moss}, stone becomes {@code permafrost} above the deepslate-style noisy
     * gradient (y >= 8) and {@code deep_permafrost} at or below y=0, all the way down to (but not
     * including) the vanilla bedrock floor.
     */
    private static SurfaceRules.RuleSource createCrystalPlainsRules() {
        return SurfaceRules.ifTrue(ABOVE_BEDROCK,
                SurfaceRules.ifTrue(SurfaceRules.isBiome(CRYSTAL_PLAINS),
                        SurfaceRules.sequence(
                                // 表层草皮:仅在初步地表之上,避免洞口/山体内部地板被草皮覆盖
                                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(),
                                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                                SurfaceRules.state(ModBlocks.FROST_MOSS.get().defaultBlockState()))),
                                // 原版 deepslate 式噪声过渡(y ≤ 0 深层 / y ≥ 8 浅层,中间为噪声带)
                                SurfaceRules.ifTrue(SHALLOW_GRADIENT,
                                        SurfaceRules.state(ModBlocks.PERMAFROST.get().defaultBlockState())),
                                SurfaceRules.ifTrue(DEEP_GRADIENT,
                                        SurfaceRules.state(ModBlocks.DEEP_PERMAFROST.get().defaultBlockState())))));
    }

    /**
     * 炎热群系全柱替换 / Scorching biome, full-column swap: the top floor block becomes
     * {@code volcanic_ash}, stone becomes {@code fire_stone} above the deepslate-style noisy
     * gradient (y >= 8) and {@code deep_fire_stone} at or below y=0.
     */
    private static SurfaceRules.RuleSource createBlazingPlainsRules() {
        return SurfaceRules.ifTrue(ABOVE_BEDROCK,
                SurfaceRules.ifTrue(SurfaceRules.isBiome(BLAZING_PLAINS),
                        SurfaceRules.sequence(
                                // 表层火山灰:仅在初步地表之上,避免洞口/山体内部地板被覆盖
                                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(),
                                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                                SurfaceRules.state(ModBlocks.VOLCANIC_ASH.get().defaultBlockState()))),
                                // 原版 deepslate 式噪声过渡(y ≤ 0 深层 / y ≥ 8 浅层,中间为噪声带)
                                SurfaceRules.ifTrue(SHALLOW_GRADIENT,
                                        SurfaceRules.state(ModBlocks.FIRE_STONE.get().defaultBlockState())),
                                SurfaceRules.ifTrue(DEEP_GRADIENT,
                                        SurfaceRules.state(ModBlocks.DEEP_FIRE_STONE.get().defaultBlockState())))));
    }

    /**
     * 旱地群系全柱替换 / Arid biome, full-column swap: the top floor block becomes {@code dry_turf},
     * stone becomes {@code weathered_sandstone} above the deepslate-style noisy gradient (y >= 8)
     * and {@code deep_weathered_sandstone} at or below y=0.
     */
    private static SurfaceRules.RuleSource createAridWastelandRules() {
        return SurfaceRules.ifTrue(ABOVE_BEDROCK,
                SurfaceRules.ifTrue(SurfaceRules.isBiome(ARID_WASTELAND),
                        SurfaceRules.sequence(
                                // 表层干草坪:仅在初步地表之上,避免洞口/山体内部地板被覆盖
                                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(),
                                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                                SurfaceRules.state(ModBlocks.DRY_TURF.get().defaultBlockState()))),
                                // 原版 deepslate 式噪声过渡(y ≤ 0 深层 / y ≥ 8 浅层,中间为噪声带)
                                SurfaceRules.ifTrue(SHALLOW_GRADIENT,
                                        SurfaceRules.state(ModBlocks.WEATHERED_SANDSTONE.get().defaultBlockState())),
                                SurfaceRules.ifTrue(DEEP_GRADIENT,
                                        SurfaceRules.state(ModBlocks.DEEP_WEATHERED_SANDSTONE.get().defaultBlockState())))));
    }
}
