package com.example.weather_realm;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式标签页注册 / Creative mode tab registration.
 *
 * <p>Two tabs replace the previous single tab: a building-blocks page
 * ({@code building_blocks}) and an items page ({@code items}, keeping the original
 * {@code itemGroup.weather_realm} translation key). Each page is ordered by a pure-data
 * {@link TabCategory} table whose {@code itemIds} are namespace-less item ids; the verifier
 * {@code tools/verify_tab_coverage.py} regex-parses those lists and asserts every registered
 * item appears exactly once.</p>
 */
public final class ModCreativeTabs {
    private ModCreativeTabs() {
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WeatherRealm.MODID);

    /** 一个创造页分类：分类键（仅校验脚本使用）+ 按显示次序排列的 item id（不含命名空间）。 */
    public record TabCategory(String key, List<String> itemIds) {
    }

    // ---- 页 A：建筑方块（9 类，列表次序即显示次序）--------------------------------------------
    public static final List<TabCategory> BUILDING_CATEGORIES = List.of(
            new TabCategory("wood", List.of(
                    "frost_log", "frost_wood", "stripped_frost_log", "stripped_frost_wood",
                    "frost_planks", "frost_stairs", "frost_slab", "frost_fence", "frost_fence_gate",
                    "frost_door", "frost_trapdoor", "frost_pressure_plate", "frost_button",
                    "scorched_log", "scorched_wood", "stripped_scorched_log",
                    "arid_log", "arid_wood", "stripped_arid_log")),
            new TabCategory("stone", List.of()),
            new TabCategory("lighting", List.of()),
            new TabCategory("glass", List.of()),
            new TabCategory("decoration", List.of("frost_wool")),
            new TabCategory("ores", List.of(
                    "permafrost_iron_ore", "deep_permafrost_iron_ore",
                    "permafrost_coal_ore", "deep_permafrost_coal_ore",
                    "permafrost_copper_ore", "deep_permafrost_copper_ore",
                    "permafrost_gold_ore", "deep_permafrost_gold_ore",
                    "permafrost_redstone_ore", "deep_permafrost_redstone_ore",
                    "permafrost_emerald_ore", "deep_permafrost_emerald_ore",
                    "permafrost_lapis_ore", "deep_permafrost_lapis_ore",
                    "permafrost_diamond_ore", "deep_permafrost_diamond_ore",
                    "permafrost_blizzard_crystal_ore", "deep_permafrost_blizzard_crystal_ore",
                    "fire_stone_coal_ore", "deep_fire_stone_coal_ore",
                    "weathered_sandstone_coal_ore", "deep_weathered_sandstone_coal_ore",
                    "fire_stone_copper_ore", "deep_fire_stone_copper_ore",
                    "weathered_sandstone_copper_ore", "deep_weathered_sandstone_copper_ore",
                    "fire_stone_iron_ore", "deep_fire_stone_iron_ore",
                    "weathered_sandstone_iron_ore", "deep_weathered_sandstone_iron_ore",
                    "fire_stone_gold_ore", "deep_fire_stone_gold_ore",
                    "weathered_sandstone_gold_ore", "deep_weathered_sandstone_gold_ore",
                    "fire_stone_redstone_ore", "deep_fire_stone_redstone_ore",
                    "weathered_sandstone_redstone_ore", "deep_weathered_sandstone_redstone_ore",
                    "fire_stone_emerald_ore", "deep_fire_stone_emerald_ore",
                    "weathered_sandstone_emerald_ore", "deep_weathered_sandstone_emerald_ore",
                    "fire_stone_lapis_ore", "deep_fire_stone_lapis_ore",
                    "weathered_sandstone_lapis_ore", "deep_weathered_sandstone_lapis_ore",
                    "fire_stone_diamond_ore", "deep_fire_stone_diamond_ore",
                    "weathered_sandstone_diamond_ore", "deep_weathered_sandstone_diamond_ore",
                    "fire_stone_blaze_crystal_ore", "deep_fire_stone_blaze_crystal_ore",
                    "weathered_sandstone_wind_crystal_ore", "deep_weathered_sandstone_wind_crystal_ore",
                    "blizzard_crystal_block", "blaze_crystal_block", "wind_crystal_block")),
            new TabCategory("geology", List.of(
                    "permafrost", "deep_permafrost",
                    "fire_stone", "deep_fire_stone",
                    "weathered_sandstone", "deep_weathered_sandstone",
                    "frost_moss", "dry_turf", "volcanic_ash")),
            new TabCategory("plants", List.of(
                    "frost_flower", "frost_grass", "glacier_bloom", "frost_sprout",
                    "tall_frost_flower", "tall_frost_grass", "frost_sapling", "frost_leaves",
                    "scorched_leaves", "arid_leaves",
                    "cinder_bloom", "flame_sprout", "fire_flower",
                    "dune_flower", "wind_sprout", "arid_bush")),
            new TabCategory("functional", List.of("weather_altar_core", "weather_pedestal")));

    // ---- 页 B：物品（6 类）--------------------------------------------------------------------
    public static final List<TabCategory> ITEM_CATEGORIES = List.of(
            new TabCategory("crystals", List.of(
                    "blizzard_crystal", "blaze_crystal", "wind_crystal",
                    "climate_shard", "frost_leather", "frost_pelt")),
            new TabCategory("tools", List.of(
                    "blizzard_crystal_sword", "blizzard_crystal_pickaxe", "blizzard_crystal_axe",
                    "blizzard_crystal_shovel", "blizzard_crystal_hoe")),
            new TabCategory("armor", List.of(
                    "blizzard_crystal_helmet", "blizzard_crystal_chestplate",
                    "blizzard_crystal_leggings", "blizzard_crystal_boots")),
            new TabCategory("food", List.of(
                    "frost_mutton", "cooked_frost_mutton", "frost_beef", "cooked_frost_beef",
                    "frost_porkchop", "cooked_frost_porkchop", "frost_raspberry", "frost_milk_bucket")),
            new TabCategory("spawn_eggs", List.of(
                    "frost_sheep_spawn_egg", "frost_cow_spawn_egg",
                    "frost_pig_spawn_egg", "frost_cat_spawn_egg")),
            new TabCategory("misc", List.of("ancient_weather_tome", "biome_map")));

    private static final ResourceKey<CreativeModeTab> BUILDING_TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "building_blocks"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BUILDING_TAB =
            CREATIVE_MODE_TABS.register("building_blocks", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.weather_realm.building_blocks"))
                    .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
                    .icon(() -> ModItems.PERMAFROST_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptCategories(output, BUILDING_CATEGORIES))
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ITEMS_TAB =
            CREATIVE_MODE_TABS.register("items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.weather_realm"))
                    .withTabsAfter(BUILDING_TAB_KEY)
                    .icon(() -> ModItems.BLIZZARD_CRYSTAL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> acceptCategories(output, ITEM_CATEGORIES))
                    .build());

    private static void acceptCategories(CreativeModeTab.Output output, List<TabCategory> categories) {
        for (TabCategory category : categories) {
            for (String id : category.itemIds()) {
                output.accept(BuiltInRegistries.ITEM
                        .getOptional(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, id))
                        .orElseThrow(() -> new IllegalStateException(
                                "ModCreativeTabs: unknown item id '" + id + "'")));
            }
        }
    }

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}
