package com.example.weather_realm.client;

import java.util.Map;
import java.util.Set;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Adds an English annotation (and optional {@code .desc} line) to every tooltip of this mod's
 * items. Client-only so the annotation is never localised away by the player's language.
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientTooltipHandler {
    private ClientTooltipHandler() {
    }

    /** Item paths whose title-cased form does not match the shipped English name. */
    private static final Map<String, String> ENGLISH_NAMES = Map.of(
            "frost_log", "Glacial Log",
            "frost_leaves", "Glacial Leaves",
            "frost_sapling", "Glacial Sapling");

    /** Armour pieces share a single description key. */
    private static final Set<String> ARMOR_PIECES = Set.of(
            "blizzard_crystal_helmet",
            "blizzard_crystal_chestplate",
            "blizzard_crystal_leggings",
            "blizzard_crystal_boots");

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!WeatherRealm.MODID.equals(key.getNamespace())) {
            return;
        }

        String path = key.getPath();

        // 1. English annotation: gray + italic.
        event.getToolTip().add(Component.literal(englishName(path))
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        // 2. Optional flavour description from the language files.
        String descPath = ARMOR_PIECES.contains(path) ? "blizzard_crystal_armor" : path;
        String descKey = firstExistingKey(descPath);
        if (descKey != null) {
            event.getToolTip().add(Component.translatable(descKey)
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
    }

    private static String firstExistingKey(String path) {
        String itemKey = "item." + WeatherRealm.MODID + "." + path + ".desc";
        if (I18n.exists(itemKey)) {
            return itemKey;
        }
        String blockKey = "block." + WeatherRealm.MODID + "." + path + ".desc";
        if (I18n.exists(blockKey)) {
            return blockKey;
        }
        return null;
    }

    private static String englishName(String path) {
        String override = ENGLISH_NAMES.get(path);
        if (override != null) {
            return override;
        }
        StringBuilder builder = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return builder.toString();
    }
}
