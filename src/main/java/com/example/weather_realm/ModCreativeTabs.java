package com.example.weather_realm;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式标签页注册 / Creative mode tab registration.
 *
 * <p>Extracted from the former monolithic mod class. The tab's title key ({@code itemGroup.weather_realm})
 * and icon are unchanged; its id was renamed from {@code example_tab} to {@code weather_realm_tab}
 * (a zero-reference MDK leftover). {@link #addCreative} keeps the permafrost building-block entries
 * in the vanilla building-blocks tab.</p>
 */
public final class ModCreativeTabs {
    private ModCreativeTabs() {
    }

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, WeatherRealm.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> WEATHER_REALM_TAB = CREATIVE_MODE_TABS.register("weather_realm_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.weather_realm"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ModItems.FROST_FLOWER_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.FROST_FLOWER_ITEM.get());
                output.accept(ModItems.FROST_GRASS_ITEM.get());
                output.accept(ModItems.GLACIER_BLOOM_ITEM.get());
                output.accept(ModItems.FROST_SPROUT_ITEM.get());
                output.accept(ModItems.TALL_FROST_FLOWER_ITEM.get());
                output.accept(ModItems.TALL_FROST_GRASS_ITEM.get());
                output.accept(ModItems.FROST_SAPLING_ITEM.get());
                output.accept(ModItems.FROST_LOG_ITEM.get());
                output.accept(ModItems.FROST_WOOD_ITEM.get());
                output.accept(ModItems.STRIPPED_FROST_LOG_ITEM.get());
                output.accept(ModItems.STRIPPED_FROST_WOOD_ITEM.get());
                output.accept(ModItems.FROST_PLANKS_ITEM.get());
                output.accept(ModItems.FROST_STAIRS_ITEM.get());
                output.accept(ModItems.FROST_SLAB_ITEM.get());
                output.accept(ModItems.FROST_FENCE_ITEM.get());
                output.accept(ModItems.FROST_FENCE_GATE_ITEM.get());
                output.accept(ModItems.FROST_DOOR_ITEM.get());
                output.accept(ModItems.FROST_TRAPDOOR_ITEM.get());
                output.accept(ModItems.FROST_PRESSURE_PLATE_ITEM.get());
                output.accept(ModItems.FROST_BUTTON_ITEM.get());
                output.accept(ModItems.WEATHER_ALTAR_CORE_ITEM.get());
                output.accept(ModItems.WEATHER_PEDESTAL_ITEM.get());
                output.accept(ModItems.CLIMATE_SHARD.get());
                output.accept(ModItems.ANCIENT_WEATHER_TOME.get());
                output.accept(ModItems.BIOME_MAP.get());
                output.accept(ModItems.FROST_LEAVES_ITEM.get());
                output.accept(ModItems.PERMAFROST_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_ITEM.get());
                output.accept(ModItems.PERMAFROST_IRON_ORE_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_IRON_ORE_ITEM.get());
                output.accept(ModItems.PERMAFROST_COAL_ORE_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_COAL_ORE_ITEM.get());
                output.accept(ModItems.PERMAFROST_COPPER_ORE_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_COPPER_ORE_ITEM.get());
                output.accept(ModItems.PERMAFROST_GOLD_ORE_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_GOLD_ORE_ITEM.get());
                output.accept(ModItems.PERMAFROST_REDSTONE_ORE_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_REDSTONE_ORE_ITEM.get());
                output.accept(ModItems.PERMAFROST_EMERALD_ORE_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_EMERALD_ORE_ITEM.get());
                output.accept(ModItems.PERMAFROST_LAPIS_ORE_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_LAPIS_ORE_ITEM.get());
                output.accept(ModItems.PERMAFROST_DIAMOND_ORE_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_DIAMOND_ORE_ITEM.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_BLOCK_ITEM.get());
                output.accept(ModItems.PERMAFROST_BLIZZARD_CRYSTAL_ORE_ITEM.get());
                output.accept(ModItems.DEEP_PERMAFROST_BLIZZARD_CRYSTAL_ORE_ITEM.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_HELMET.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_CHESTPLATE.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_LEGGINGS.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_BOOTS.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_SWORD.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_PICKAXE.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_AXE.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_SHOVEL.get());
                output.accept(ModItems.BLIZZARD_CRYSTAL_HOE.get());
                output.accept(ModItems.FROST_SHEEP_SPAWN_EGG.get());
                output.accept(ModItems.FROST_COW_SPAWN_EGG.get());
                output.accept(ModItems.FROST_PIG_SPAWN_EGG.get());
                output.accept(ModItems.FROST_CAT_SPAWN_EGG.get());
                output.accept(ModItems.FROST_WOOL_ITEM.get());
                output.accept(ModItems.FROST_MUTTON.get());
                output.accept(ModItems.COOKED_FROST_MUTTON.get());
                output.accept(ModItems.FROST_BEEF.get());
                output.accept(ModItems.COOKED_FROST_BEEF.get());
                output.accept(ModItems.FROST_LEATHER.get());
                output.accept(ModItems.FROST_PORKCHOP.get());
                output.accept(ModItems.COOKED_FROST_PORKCHOP.get());
                output.accept(ModItems.FROST_PELT.get());
                output.accept(ModItems.FROST_MILK_BUCKET.get());
                output.accept(ModItems.FROST_RASPBERRY.get());
                output.accept(ModItems.FROST_MOSS_ITEM.get());
                output.accept(ModItems.DRY_TURF_ITEM.get());
                output.accept(ModItems.VOLCANIC_ASH_ITEM.get());
                output.accept(ModItems.FIRE_STONE_ITEM.get());
                output.accept(ModItems.DEEP_FIRE_STONE_ITEM.get());
                output.accept(ModItems.WEATHERED_SANDSTONE_ITEM.get());
                output.accept(ModItems.DEEP_WEATHERED_SANDSTONE_ITEM.get());
                output.accept(ModItems.BLAZE_CRYSTAL.get());
                output.accept(ModItems.BLAZE_CRYSTAL_BLOCK_ITEM.get());
                output.accept(ModItems.WIND_CRYSTAL.get());
                output.accept(ModItems.WIND_CRYSTAL_BLOCK_ITEM.get());
                output.accept(ModItems.SCORCHED_LOG_ITEM.get());
                output.accept(ModItems.SCORCHED_WOOD_ITEM.get());
                output.accept(ModItems.STRIPPED_SCORCHED_LOG_ITEM.get());
                output.accept(ModItems.SCORCHED_LEAVES_ITEM.get());
                output.accept(ModItems.ARID_LOG_ITEM.get());
                output.accept(ModItems.ARID_WOOD_ITEM.get());
                output.accept(ModItems.STRIPPED_ARID_LOG_ITEM.get());
                output.accept(ModItems.ARID_LEAVES_ITEM.get());
                output.accept(ModItems.CINDER_BLOOM_ITEM.get());
                output.accept(ModItems.FLAME_SPROUT_ITEM.get());
                output.accept(ModItems.FIRE_FLOWER_ITEM.get());
                output.accept(ModItems.DUNE_FLOWER_ITEM.get());
                output.accept(ModItems.WIND_SPROUT_ITEM.get());
                output.accept(ModItems.ARID_BUSH_ITEM.get());
                ModItems.GENERATED_ORE_ITEMS.forEach(item -> output.accept(item.get()));
            }).build());

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }

    /** Add the permafrost building blocks to the vanilla building blocks tab. */
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.PERMAFROST_ITEM);
            event.accept(ModItems.DEEP_PERMAFROST_ITEM);
        }
    }
}
