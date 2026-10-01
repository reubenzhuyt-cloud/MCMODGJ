package com.example.weather_realm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.example.weather_realm.item.AncientWeatherTomeItem;
import com.example.weather_realm.item.BiomeMapItem;
import com.example.weather_realm.item.ClimateShardItem;
import com.example.weather_realm.item.FrostMilkBucketItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册 / Item registration.
 *
 * <p>Extracted from the former monolithic mod class. Every item id is unchanged and the declarations
 * keep their original order; the matching blocks live in {@link ModBlocks}. The generated ore
 * {@code BlockItem}s are built from {@link ModBlocks#GENERATED_ORES} so the block registry never has
 * to reference this class while it is still being initialised.</p>
 */
public final class ModItems {
    private ModItems() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(WeatherRealm.MODID);

    // --- Frost themed block items ---------------------------------------------------------------
    public static final DeferredItem<BlockItem> FROST_FLOWER_ITEM = ITEMS.registerSimpleBlockItem("frost_flower", ModBlocks.FROST_FLOWER);
    public static final DeferredItem<BlockItem> FROST_GRASS_ITEM = ITEMS.registerSimpleBlockItem("frost_grass", ModBlocks.FROST_GRASS);
    public static final DeferredItem<BlockItem> GLACIER_BLOOM_ITEM = ITEMS.registerSimpleBlockItem("glacier_bloom", ModBlocks.GLACIER_BLOOM);
    public static final DeferredItem<BlockItem> FROST_SPROUT_ITEM = ITEMS.registerSimpleBlockItem("frost_sprout", ModBlocks.FROST_SPROUT);
    public static final DeferredItem<BlockItem> TALL_FROST_FLOWER_ITEM = ITEMS.register("tall_frost_flower",
            () -> new DoubleHighBlockItem(ModBlocks.TALL_FROST_FLOWER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TALL_FROST_GRASS_ITEM = ITEMS.register("tall_frost_grass",
            () -> new DoubleHighBlockItem(ModBlocks.TALL_FROST_GRASS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FROST_SAPLING_ITEM = ITEMS.registerSimpleBlockItem("frost_sapling", ModBlocks.FROST_SAPLING);
    public static final DeferredItem<BlockItem> FROST_LOG_ITEM = ITEMS.registerSimpleBlockItem("frost_log", ModBlocks.FROST_LOG);

    // --- 坚冰木套件 / Frost wood family ---------------------------------------------------------
    public static final DeferredItem<BlockItem> FROST_PLANKS_ITEM = ITEMS.registerSimpleBlockItem("frost_planks", ModBlocks.FROST_PLANKS);
    public static final DeferredItem<BlockItem> FROST_STAIRS_ITEM = ITEMS.registerSimpleBlockItem("frost_stairs", ModBlocks.FROST_STAIRS);
    public static final DeferredItem<BlockItem> FROST_SLAB_ITEM = ITEMS.registerSimpleBlockItem("frost_slab", ModBlocks.FROST_SLAB);
    public static final DeferredItem<BlockItem> FROST_FENCE_ITEM = ITEMS.registerSimpleBlockItem("frost_fence", ModBlocks.FROST_FENCE);
    public static final DeferredItem<BlockItem> FROST_FENCE_GATE_ITEM = ITEMS.registerSimpleBlockItem("frost_fence_gate", ModBlocks.FROST_FENCE_GATE);
    public static final DeferredItem<BlockItem> FROST_DOOR_ITEM = ITEMS.register("frost_door",
            () -> new DoubleHighBlockItem(ModBlocks.FROST_DOOR.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> FROST_TRAPDOOR_ITEM = ITEMS.registerSimpleBlockItem("frost_trapdoor", ModBlocks.FROST_TRAPDOOR);
    public static final DeferredItem<BlockItem> FROST_PRESSURE_PLATE_ITEM = ITEMS.registerSimpleBlockItem("frost_pressure_plate", ModBlocks.FROST_PRESSURE_PLATE);
    public static final DeferredItem<BlockItem> FROST_BUTTON_ITEM = ITEMS.registerSimpleBlockItem("frost_button", ModBlocks.FROST_BUTTON);
    public static final DeferredItem<BlockItem> STRIPPED_FROST_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_frost_log", ModBlocks.STRIPPED_FROST_LOG);
    public static final DeferredItem<BlockItem> FROST_WOOD_ITEM = ITEMS.registerSimpleBlockItem("frost_wood", ModBlocks.FROST_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_FROST_WOOD_ITEM = ITEMS.registerSimpleBlockItem("stripped_frost_wood", ModBlocks.STRIPPED_FROST_WOOD);

    // --- 天气祭坛核心 / 调谐基座 ------------------------------------------------------------------
    public static final DeferredItem<BlockItem> WEATHER_ALTAR_CORE_ITEM = ITEMS.registerSimpleBlockItem("weather_altar_core", ModBlocks.WEATHER_ALTAR_CORE);
    public static final DeferredItem<BlockItem> WEATHER_PEDESTAL_ITEM = ITEMS.registerSimpleBlockItem("weather_pedestal", ModBlocks.WEATHER_PEDESTAL);

    // --- 气候碎片 / 《古代天气研究手记》/ 群系地图 -----------------------------------------------
    public static final DeferredItem<ClimateShardItem> CLIMATE_SHARD = ITEMS.register("climate_shard",
            () -> new ClimateShardItem(new Item.Properties()));
    public static final DeferredItem<AncientWeatherTomeItem> ANCIENT_WEATHER_TOME = ITEMS.register("ancient_weather_tome",
            () -> new AncientWeatherTomeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BiomeMapItem> BIOME_MAP = ITEMS.register("biome_map",
            () -> new BiomeMapItem(BiomeMapItem.defaultProperties()));

    // --- 坚冰木树叶 / 冻土地质 ---------------------------------------------------------------------
    public static final DeferredItem<BlockItem> FROST_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("frost_leaves", ModBlocks.FROST_LEAVES);
    public static final DeferredItem<BlockItem> PERMAFROST_ITEM = ITEMS.registerSimpleBlockItem("permafrost", ModBlocks.PERMAFROST);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost", ModBlocks.DEEP_PERMAFROST);
    public static final DeferredItem<BlockItem> PERMAFROST_IRON_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_iron_ore", ModBlocks.PERMAFROST_IRON_ORE);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_IRON_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_iron_ore", ModBlocks.DEEP_PERMAFROST_IRON_ORE);

    public static final DeferredItem<BlockItem> PERMAFROST_COAL_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_coal_ore", ModBlocks.PERMAFROST_COAL_ORE);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_COAL_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_coal_ore", ModBlocks.DEEP_PERMAFROST_COAL_ORE);
    public static final DeferredItem<BlockItem> PERMAFROST_COPPER_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_copper_ore", ModBlocks.PERMAFROST_COPPER_ORE);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_COPPER_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_copper_ore", ModBlocks.DEEP_PERMAFROST_COPPER_ORE);
    public static final DeferredItem<BlockItem> PERMAFROST_GOLD_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_gold_ore", ModBlocks.PERMAFROST_GOLD_ORE);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_GOLD_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_gold_ore", ModBlocks.DEEP_PERMAFROST_GOLD_ORE);
    public static final DeferredItem<BlockItem> PERMAFROST_REDSTONE_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_redstone_ore", ModBlocks.PERMAFROST_REDSTONE_ORE);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_REDSTONE_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_redstone_ore", ModBlocks.DEEP_PERMAFROST_REDSTONE_ORE);
    public static final DeferredItem<BlockItem> PERMAFROST_EMERALD_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_emerald_ore", ModBlocks.PERMAFROST_EMERALD_ORE);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_EMERALD_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_emerald_ore", ModBlocks.DEEP_PERMAFROST_EMERALD_ORE);
    public static final DeferredItem<BlockItem> PERMAFROST_LAPIS_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_lapis_ore", ModBlocks.PERMAFROST_LAPIS_ORE);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_LAPIS_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_lapis_ore", ModBlocks.DEEP_PERMAFROST_LAPIS_ORE);
    public static final DeferredItem<BlockItem> PERMAFROST_DIAMOND_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_diamond_ore", ModBlocks.PERMAFROST_DIAMOND_ORE);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_DIAMOND_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_diamond_ore", ModBlocks.DEEP_PERMAFROST_DIAMOND_ORE);

    // --- Blizzard Crystal suite -----------------------------------------------------------------
    public static final DeferredItem<Item> BLIZZARD_CRYSTAL = ITEMS.registerSimpleItem("blizzard_crystal");
    public static final DeferredItem<BlockItem> BLIZZARD_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("blizzard_crystal_block", ModBlocks.BLIZZARD_CRYSTAL_BLOCK);
    public static final DeferredItem<BlockItem> PERMAFROST_BLIZZARD_CRYSTAL_ORE_ITEM = ITEMS.registerSimpleBlockItem("permafrost_blizzard_crystal_ore", ModBlocks.PERMAFROST_BLIZZARD_CRYSTAL_ORE);
    public static final DeferredItem<BlockItem> DEEP_PERMAFROST_BLIZZARD_CRYSTAL_ORE_ITEM = ITEMS.registerSimpleBlockItem("deep_permafrost_blizzard_crystal_ore", ModBlocks.DEEP_PERMAFROST_BLIZZARD_CRYSTAL_ORE);

    // Armor material (datapack-backed ARMOR_MATERIAL registry).
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, WeatherRealm.MODID);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BLIZZARD_CRYSTAL_MATERIAL = ARMOR_MATERIALS.register("blizzard_crystal",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 3,
                            ArmorItem.Type.LEGGINGS, 6,
                            ArmorItem.Type.CHESTPLATE, 8,
                            ArmorItem.Type.HELMET, 3,
                            ArmorItem.Type.BODY, 9),
                    15,
                    SoundEvents.ARMOR_EQUIP_DIAMOND,
                    () -> Ingredient.of(BLIZZARD_CRYSTAL.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "blizzard_crystal"))),
                    2.5F,
                    0.1F));

    public static final DeferredItem<ArmorItem> BLIZZARD_CRYSTAL_HELMET = ITEMS.register("blizzard_crystal_helmet",
            () -> new ArmorItem(BLIZZARD_CRYSTAL_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(40))));
    public static final DeferredItem<ArmorItem> BLIZZARD_CRYSTAL_CHESTPLATE = ITEMS.register("blizzard_crystal_chestplate",
            () -> new ArmorItem(BLIZZARD_CRYSTAL_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(40))));
    public static final DeferredItem<ArmorItem> BLIZZARD_CRYSTAL_LEGGINGS = ITEMS.register("blizzard_crystal_leggings",
            () -> new ArmorItem(BLIZZARD_CRYSTAL_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(40))));
    public static final DeferredItem<ArmorItem> BLIZZARD_CRYSTAL_BOOTS = ITEMS.register("blizzard_crystal_boots",
            () -> new ArmorItem(BLIZZARD_CRYSTAL_MATERIAL, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(40))));

    // Custom tool tier (diamond-incorrect blocks, 1650 uses, 8.5 speed, +3.5 damage, 16 enchantability).
    public static final Tier BLIZZARD_CRYSTAL_TIER = new SimpleTier(
            BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1650, 8.5F, 3.5F, 16,
            () -> Ingredient.of(BLIZZARD_CRYSTAL.get()));

    public static final DeferredItem<SwordItem> BLIZZARD_CRYSTAL_SWORD = ITEMS.register("blizzard_crystal_sword",
            () -> new SwordItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(SwordItem.createAttributes(BLIZZARD_CRYSTAL_TIER, 3, -2.4F))));
    public static final DeferredItem<PickaxeItem> BLIZZARD_CRYSTAL_PICKAXE = ITEMS.register("blizzard_crystal_pickaxe",
            () -> new PickaxeItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(PickaxeItem.createAttributes(BLIZZARD_CRYSTAL_TIER, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> BLIZZARD_CRYSTAL_AXE = ITEMS.register("blizzard_crystal_axe",
            () -> new AxeItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(AxeItem.createAttributes(BLIZZARD_CRYSTAL_TIER, 5.0F, -3.0F))));
    public static final DeferredItem<ShovelItem> BLIZZARD_CRYSTAL_SHOVEL = ITEMS.register("blizzard_crystal_shovel",
            () -> new ShovelItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(ShovelItem.createAttributes(BLIZZARD_CRYSTAL_TIER, 1.5F, -3.0F))));
    public static final DeferredItem<HoeItem> BLIZZARD_CRYSTAL_HOE = ITEMS.register("blizzard_crystal_hoe",
            () -> new HoeItem(BLIZZARD_CRYSTAL_TIER, new Item.Properties().attributes(HoeItem.createAttributes(BLIZZARD_CRYSTAL_TIER, -3.0F, 0.0F))));

    // --- Entity spawn eggs ----------------------------------------------------------------------
    public static final DeferredItem<SpawnEggItem> FROST_SHEEP_SPAWN_EGG = ITEMS.register("frost_sheep_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROST_SHEEP, 0xA0E0F0, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> FROST_COW_SPAWN_EGG = ITEMS.register("frost_cow_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROST_COW, 0x5F402A, 0xE8F4FF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> FROST_PIG_SPAWN_EGG = ITEMS.register("frost_pig_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROST_PIG, 0xF0B4C0, 0xFFFFFF, new Item.Properties()));
    public static final DeferredItem<SpawnEggItem> FROST_CAT_SPAWN_EGG = ITEMS.register("frost_cat_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.FROST_CAT, 0xE8F0F8, 0x5A6A7A, new Item.Properties()));

    // --- Frost animal drops ---------------------------------------------------------------------
    public static final DeferredItem<BlockItem> FROST_WOOL_ITEM = ITEMS.registerSimpleBlockItem("frost_wool", ModBlocks.FROST_WOOL);

    public static final DeferredItem<Item> FROST_MUTTON = ITEMS.registerSimpleItem("frost_mutton",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> COOKED_FROST_MUTTON = ITEMS.registerSimpleItem("cooked_frost_mutton",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> FROST_BEEF = ITEMS.registerSimpleItem("frost_beef",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> COOKED_FROST_BEEF = ITEMS.registerSimpleItem("cooked_frost_beef",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> FROST_LEATHER = ITEMS.registerSimpleItem("frost_leather");
    public static final DeferredItem<Item> FROST_PORKCHOP = ITEMS.registerSimpleItem("frost_porkchop",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> COOKED_FROST_PORKCHOP = ITEMS.registerSimpleItem("cooked_frost_porkchop",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> FROST_PELT = ITEMS.registerSimpleItem("frost_pelt");

    // 冰树莓 / Frost Raspberry - a frosty berry snack matching vanilla sweet berries.
    public static final DeferredItem<Item> FROST_RASPBERRY = ITEMS.registerSimpleItem("frost_raspberry",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build()));

    // 冰牛奶桶 / Frost Milk Bucket - obtained by milking a frost cow with an empty bucket.
    public static final DeferredItem<MilkBucketItem> FROST_MILK_BUCKET = ITEMS.register("frost_milk_bucket",
            () -> new FrostMilkBucketItem(new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    // ============================================================================================
    // 三大群系地表覆盖 / Biome surface covers
    // ============================================================================================
    public static final DeferredItem<BlockItem> FROST_MOSS_ITEM = ITEMS.registerSimpleBlockItem("frost_moss", ModBlocks.FROST_MOSS);
    public static final DeferredItem<BlockItem> DRY_TURF_ITEM = ITEMS.registerSimpleBlockItem("dry_turf", ModBlocks.DRY_TURF);
    public static final DeferredItem<BlockItem> VOLCANIC_ASH_ITEM = ITEMS.registerSimpleBlockItem("volcanic_ash", ModBlocks.VOLCANIC_ASH);

    // ============================================================================================
    // 炎热 / 风沙 群系内容 / Blazing & arid biome content
    // ============================================================================================
    public static final DeferredItem<BlockItem> FIRE_STONE_ITEM = ITEMS.registerSimpleBlockItem("fire_stone", ModBlocks.FIRE_STONE);
    public static final DeferredItem<BlockItem> DEEP_FIRE_STONE_ITEM = ITEMS.registerSimpleBlockItem("deep_fire_stone", ModBlocks.DEEP_FIRE_STONE);
    public static final DeferredItem<BlockItem> WEATHERED_SANDSTONE_ITEM = ITEMS.registerSimpleBlockItem("weathered_sandstone", ModBlocks.WEATHERED_SANDSTONE);
    public static final DeferredItem<BlockItem> DEEP_WEATHERED_SANDSTONE_ITEM = ITEMS.registerSimpleBlockItem("deep_weathered_sandstone", ModBlocks.DEEP_WEATHERED_SANDSTONE);

    // 烈火晶石 / 风沙晶石 - items + decorative blocks
    public static final DeferredItem<Item> BLAZE_CRYSTAL = ITEMS.registerSimpleItem("blaze_crystal");
    public static final DeferredItem<BlockItem> BLAZE_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("blaze_crystal_block", ModBlocks.BLAZE_CRYSTAL_BLOCK);
    public static final DeferredItem<Item> WIND_CRYSTAL = ITEMS.registerSimpleItem("wind_crystal");
    public static final DeferredItem<BlockItem> WIND_CRYSTAL_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("wind_crystal_block", ModBlocks.WIND_CRYSTAL_BLOCK);

    // 矿石 BlockItem 批量注册 / Generated ore family items. Driven by {@link ModBlocks#GENERATED_ORES};
    // creative-page grouping is maintained as literal ids in {@link ModCreativeTabs}, not collected here.
    static {
        for (ModBlocks.OreBlock ore : ModBlocks.GENERATED_ORES) {
            ITEMS.registerSimpleBlockItem(ore.name(), ore.block());
        }
    }

    // --- 焦木 / Scorched wood (blaze biome tree family) -----------------------------------------
    public static final DeferredItem<BlockItem> SCORCHED_LOG_ITEM = ITEMS.registerSimpleBlockItem("scorched_log", ModBlocks.SCORCHED_LOG);
    public static final DeferredItem<BlockItem> SCORCHED_WOOD_ITEM = ITEMS.registerSimpleBlockItem("scorched_wood", ModBlocks.SCORCHED_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_SCORCHED_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_scorched_log", ModBlocks.STRIPPED_SCORCHED_LOG);
    public static final DeferredItem<BlockItem> SCORCHED_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("scorched_leaves", ModBlocks.SCORCHED_LEAVES);

    // --- 风化木 / Arid wood (arid biome tree family) --------------------------------------------
    public static final DeferredItem<BlockItem> ARID_LOG_ITEM = ITEMS.registerSimpleBlockItem("arid_log", ModBlocks.ARID_LOG);
    public static final DeferredItem<BlockItem> ARID_WOOD_ITEM = ITEMS.registerSimpleBlockItem("arid_wood", ModBlocks.ARID_WOOD);
    public static final DeferredItem<BlockItem> STRIPPED_ARID_LOG_ITEM = ITEMS.registerSimpleBlockItem("stripped_arid_log", ModBlocks.STRIPPED_ARID_LOG);
    public static final DeferredItem<BlockItem> ARID_LEAVES_ITEM = ITEMS.registerSimpleBlockItem("arid_leaves", ModBlocks.ARID_LEAVES);

    // --- 植被 / Vegetation ----------------------------------------------------------------------
    public static final DeferredItem<BlockItem> CINDER_BLOOM_ITEM = ITEMS.registerSimpleBlockItem("cinder_bloom", ModBlocks.CINDER_BLOOM);
    public static final DeferredItem<BlockItem> FLAME_SPROUT_ITEM = ITEMS.registerSimpleBlockItem("flame_sprout", ModBlocks.FLAME_SPROUT);
    public static final DeferredItem<BlockItem> FIRE_FLOWER_ITEM = ITEMS.registerSimpleBlockItem("fire_flower", ModBlocks.FIRE_FLOWER);
    public static final DeferredItem<BlockItem> DUNE_FLOWER_ITEM = ITEMS.registerSimpleBlockItem("dune_flower", ModBlocks.DUNE_FLOWER);
    public static final DeferredItem<BlockItem> WIND_SPROUT_ITEM = ITEMS.registerSimpleBlockItem("wind_sprout", ModBlocks.WIND_SPROUT);
    public static final DeferredItem<BlockItem> ARID_BUSH_ITEM = ITEMS.registerSimpleBlockItem("arid_bush", ModBlocks.ARID_BUSH);

    // ============================================================================================
    // 建筑方块 BlockItem 批量注册 / Building-block items (design §8.2)
    // ============================================================================================
    public static final List<DeferredItem<BlockItem>> BUILDING_BLOCK_ITEMS = new ArrayList<>();

    private static DeferredItem<BlockItem> buildingItem(String name, DeferredBlock<? extends Block> block) {
        DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(name, block);
        BUILDING_BLOCK_ITEMS.add(item);
        return item;
    }

    private static DeferredItem<BlockItem> buildingDoorItem(String name, DeferredBlock<? extends Block> block) {
        DeferredItem<BlockItem> item = ITEMS.register(name,
                () -> new DoubleHighBlockItem(block.get(), new Item.Properties()));
        BUILDING_BLOCK_ITEMS.add(item);
        return item;
    }

    static {
        for (ModBuildingBlocks.StoneFamily family : ModBuildingBlocks.STONE_FAMILIES) {
            for (ModBuildingBlocks.StoneLayer layer : new ModBuildingBlocks.StoneLayer[]{family.shallow(), family.deep()}) {
                buildingItem(layer.name() + "_polished", layer.polished());
                buildingItem(layer.name() + "_polished_stairs", layer.polishedStairs());
                buildingItem(layer.name() + "_polished_slab", layer.polishedSlab());
                buildingItem(layer.name() + "_polished_wall", layer.polishedWall());
                buildingItem(layer.name() + "_bricks", layer.bricks());
                buildingItem(layer.name() + "_brick_stairs", layer.brickStairs());
                buildingItem(layer.name() + "_brick_slab", layer.brickSlab());
                buildingItem(layer.name() + "_brick_wall", layer.brickWall());
                if (layer.crackedBricks() != null) {
                    buildingItem(layer.name() + "_cracked_bricks", layer.crackedBricks());
                }
                buildingItem(layer.name() + "_chiseled", layer.chiseled());
                if (layer.pillar() != null) {
                    buildingItem(layer.name() + "_pillar", layer.pillar());
                }
            }
        }
        for (ModBuildingBlocks.WoodFamily family : ModBuildingBlocks.WOOD_FAMILIES) {
            String p = family.prefix();
            buildingItem("stripped_" + p + "_wood", family.strippedWood());
            buildingItem(p + "_planks", family.planks());
            buildingItem(p + "_stairs", family.stairs());
            buildingItem(p + "_slab", family.slab());
            buildingItem(p + "_fence", family.fence());
            buildingItem(p + "_fence_gate", family.fenceGate());
            buildingDoorItem(p + "_door", family.door());
            buildingItem(p + "_trapdoor", family.trapdoor());
            buildingItem(p + "_pressure_plate", family.pressurePlate());
            buildingItem(p + "_button", family.button());
        }
        for (ModBuildingBlocks.LanternSet set : ModBuildingBlocks.LANTERNS) {
            buildingItem(set.prefix() + "_lantern", set.lantern());
        }
        for (ModBuildingBlocks.DecorationSet set : ModBuildingBlocks.DECORATIONS) {
            buildingItem(set.prefix() + "_glass", set.glass());
            buildingItem(set.prefix() + "_glass_pane", set.glassPane());
            buildingItem(set.prefix() + "_grate", set.grate());
            buildingItem(set.prefix() + "_chain", set.chain());
        }
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static void registerArmorMaterials(IEventBus bus) {
        ARMOR_MATERIALS.register(bus);
    }
}
