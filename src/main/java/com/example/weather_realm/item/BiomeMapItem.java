package com.example.weather_realm.item;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.example.weather_realm.ModDimensions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * 极域天象图 / Biome Map.
 *
 * <p>Extends vanilla {@link MapItem} so the first-person "two hands holding a map up to the face"
 * pose, the arm rendering and the {@code MapRenderer} pixel pipeline are reused verbatim. Vanilla
 * {@code ItemInHandRenderer.renderArmWithItem} routes any {@code stack.getItem() instanceof MapItem}
 * through its map branch, and {@code renderMap} asks the item for its map data through the
 * virtual {@link #getCustomMapData(ItemStack, Level)} hook.</p>
 *
 * <p>The item always carries {@link #MAP_ID} (a negative id that never collides with the
 * {@code Level.getFreeMapId()} sequence) so the renderer never receives a {@code null} {@link MapId}.
 * That id is purely cosmetic: it keys the client-side {@code MapRenderer} cache. The map's actual
 * pixel data is produced by the client-only {@code BiomeMapClientData}.</p>
 *
 * <p>This class is common code and must never reference {@code net.minecraft.client.*}. The bridge
 * to the client pixel pipeline is a plain {@link Function} installed at client startup, so a
 * dedicated server can load and use this item class without ever touching client classes.</p>
 */
public class BiomeMapItem extends MapItem {
    /** 负值 id，避开 {@code Level.getFreeMapId()} 从 0 递增的真实地图 id。 */
    public static final MapId MAP_ID = new MapId(-1);

    /** 档位 1：2 区块/像素 / Macro tier: one pixel covers a 2x2 chunk patch. */
    public static final int TIER_MACRO = 1;
    /** 档位 2：4 区块/像素 / Wide tier: one pixel covers a 4x4 chunk patch. */
    public static final int TIER_WIDE = 2;

    private static final int CHUNKS_PER_TIER = 2;
    private static final int TEXTURE_SIZE = 128;

    /** 客户端缩放档位 / Client-local zoom tier; cosmetic only, never authoritative. */
    private static int zoomTier = TIER_MACRO;

    /**
     * 客户端地图数据提供者 / Installed once on the physical client by
     * {@code BiomeMapClientData}'s static initialiser. {@code null} on a dedicated server, which
     * keeps every client reference out of common code.
     */
    @Nullable
    private static Function<Level, MapItemSavedData> clientMapDataProvider;

    public BiomeMapItem(Item.Properties properties) {
        super(properties);
    }

    /** 默认属性：单堆叠 + 必需的 {@link DataComponents#MAP_ID} 组件。 */
    public static Item.Properties defaultProperties() {
        return new Item.Properties()
                .stacksTo(1)
                .component(DataComponents.MAP_ID, MAP_ID);
    }

    /** 安装客户端地图数据提供者 / Called from client-only code at startup. */
    public static void setClientMapDataProvider(@Nullable Function<Level, MapItemSavedData> provider) {
        clientMapDataProvider = provider;
    }

    public static int getZoomTier() {
        return zoomTier;
    }

    /** 每像素覆盖的区块数 / Chunks covered by one map pixel for the active tier (2 or 4). */
    public static int getChunksPerPixel() {
        return zoomTier * CHUNKS_PER_TIER;
    }

    /** 128x128 纹理覆盖的总格数 / Total world coverage in blocks (4096 or 8192). */
    public static int getCoverageBlocks() {
        return TEXTURE_SIZE * getChunksPerPixel() * 16;
    }

    /**
     * Vanilla NeoForge hook: {@code MapItem.getSavedData(ItemStack, Level)} dispatches here, and
     * {@code ItemInHandRenderer.renderMap} calls {@code MapItem.getSavedData(stack, level)}.
     *
     * <p>Server: always {@code null} (no server-side behaviour, no client classes). Client: the
     * shared {@link MapItemSavedData} produced by {@code BiomeMapClientData}.</p>
     */
    @Nullable
    @Override
    protected MapItemSavedData getCustomMapData(ItemStack stack, Level level) {
        if (level == null || !level.isClientSide()) {
            return null;
        }
        if (stack.get(DataComponents.MAP_ID) == null) {
            return null;
        }
        Function<Level, MapItemSavedData> provider = clientMapDataProvider;
        return provider == null ? null : provider.apply(level);
    }

    /** 右键缩放档位，并保留维度限制 / Cycle the zoom tier; keep the dimension gate. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.dimension().equals(ModDimensions.CRYSTAL_REALM)) {
            if (level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("message.weather_realm.biome_map.wrong_dimension")
                                .withStyle(ChatFormatting.AQUA),
                        true);
                level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL,
                        SoundSource.PLAYERS, 1.0F, 1.4F);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide()) {
            cycleZoom(player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Client-only tier swap + actionbar hint + page-turn sound (common APIs only). */
    private static void cycleZoom(Player player) {
        zoomTier = (zoomTier == TIER_MACRO) ? TIER_WIDE : TIER_MACRO;
        if (player != null) {
            player.displayClientMessage(
                    Component.translatable("message.weather_realm.biome_map.zoom",
                            getCoverageBlocks(), getChunksPerPixel()),
                    true);
            player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.6F, 1.2F);
        }
    }

    /** 地图数据由客户端自行维护，服务端无横幅标记行为 / No server-side banner tracking. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return InteractionResult.PASS;
    }

    /** 服务端无 tick 行为（渲染数据完全由客户端生成） / No server-side inventory ticking. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int itemSlot, boolean isSelected) {
        // Intentionally empty: the pixel data is client-generated via getCustomMapData.
    }

    /**
     * 阻断原版 {@code filled_map.unknown} 高级提示 / Supersedes {@link MapItem}'s vanilla tooltip,
     * which would otherwise print the semantically wrong {@code filled_map.unknown} sentinel under
     * F3+H. The visible name is supplied by the client tooltip handler instead.
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
    }

    @Nullable
    @Override
    public Packet<?> getUpdatePacket(ItemStack stack, Level level, Player player) {
        return null;
    }
}
