package com.example.weather_realm.item;

import com.example.weather_realm.WeatherRealm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/**
 * 《古代天气研究手记》/ Ancient Weather Tome.
 *
 * <p>Right-clicking opens the bundled Patchouli guidebook when Patchouli is installed. Patchouli is
 * an <em>optional</em> dependency, so the API is reached through reflection and a native chat
 * fallback is shown when it is missing — the item never crashes a client that lacks the mod.</p>
 */
public class AncientWeatherTomeItem extends Item {
    /** The Patchouli book id shipped under {@code data/weather_realm/patchouli_books/weather_tome/}. */
    public static final ResourceLocation BOOK_ID =
            ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "weather_tome");

    private static final String[] FALLBACK_CHAPTERS = {"prologue", "ritual", "fracture", "dimension"};

    public AncientWeatherTomeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (!openPatchouliBook(serverPlayer)) {
                sendFallback(serverPlayer);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static boolean openPatchouliBook(ServerPlayer player) {
        if (!ModList.get().isLoaded("patchouli")) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object api = apiClass.getMethod("get").invoke(null);
            Method openMethod = null;
            for (Method candidate : api.getClass().getMethods()) {
                if (!candidate.getName().equals("openBookGUI") || candidate.getParameterCount() != 2) {
                    continue;
                }
                Class<?>[] params = candidate.getParameterTypes();
                if (ServerPlayer.class.isAssignableFrom(params[0])
                        && ResourceLocation.class.isAssignableFrom(params[1])) {
                    openMethod = candidate;
                    break;
                }
            }
            if (openMethod == null) {
                WeatherRealm.LOGGER.warn("No openBookGUI(ServerPlayer, ResourceLocation) method found on {}", api.getClass());
                return false;
            }
            openMethod.invoke(api, player, BOOK_ID);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            WeatherRealm.LOGGER.warn("Could not open Patchouli book {}", BOOK_ID, exception);
            return false;
        }
    }

    private static void sendFallback(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.weather_realm.tome.fallback_hint")
                .withStyle(ChatFormatting.GOLD), false);
        for (String chapter : FALLBACK_CHAPTERS) {
            player.displayClientMessage(Component.translatable("message.weather_realm.tome." + chapter)
                    .withStyle(ChatFormatting.AQUA), false);
        }
    }
}
