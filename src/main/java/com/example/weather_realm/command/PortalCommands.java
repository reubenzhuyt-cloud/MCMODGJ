package com.example.weather_realm.command;

import com.example.weather_realm.WeatherRealm;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 调试指令 / Debug command: {@code /build_portal}.
 *
 * <p>在指令执行者脚下直接搭建一座结构完整、尚未点燃的 4x4 气候传送门底座
 * （{@code py-2} 平滑石承台 + {@code py-1} 层 2x2 水源与 12 格外围框架 + {@code py..py+1} 净空），
 * 便于开发期验证 {@link com.example.weather_realm.portal.ClimatePortalHandler} 的
 * {@code isPoolValid} + {@code isRingValid} 判定链路。指令本身<b>不</b>点燃传送门——
 * 点燃仍由玩家向水池投掷 {@code weather_realm:climate_shard} 触发。</p>
 */
@EventBusSubscriber(modid = WeatherRealm.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class PortalCommands {

    private PortalCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("build_portal")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> buildPortal(ctx.getSource())));
    }

    /**
     * 以玩家脚下方块为基准分层搭建未激活传送门。
     *
     * @return 恒为 {@code 1}（Brigadier 成功返回码）。
     */
    private static int buildPortal(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();

        BlockPos foot = player.blockPosition();
        int px = foot.getX();
        int py = foot.getY();
        int pz = foot.getZ();

        // 4x4 区域最小角（py-1 层的最小角）。
        BlockPos min = new BlockPos(px - 1, py - 1, pz - 1);

        // 1) y = py-2：4x4 平滑石承台，防止水流向下泄漏。
        BlockState smoothStone = Blocks.SMOOTH_STONE.defaultBlockState();
        for (int dx = 0; dx < 4; dx++) {
            for (int dz = 0; dz < 4; dz++) {
                level.setBlock(min.offset(dx, -1, dz), smoothStone, 3);
            }
        }

        // 2) y = py-1：按标准矩阵铺设 12 格框架 + 2x2 水源。
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState packedIce = Blocks.PACKED_ICE.defaultBlockState();
        BlockState sandstone = Blocks.SANDSTONE.defaultBlockState();
        BlockState magma = Blocks.MAGMA_BLOCK.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();

        char[][] pattern = {
                {'o', 'i', 's', 'n'},
                {'i', 'w', 'w', 's'},
                {'s', 'w', 'w', 'i'},
                {'n', 'i', 's', 'o'}
        };

        for (int dz = 0; dz < 4; dz++) {
            for (int dx = 0; dx < 4; dx++) {
                BlockState state = switch (pattern[dz][dx]) {
                    case 'o' -> obsidian;
                    case 'i' -> packedIce;
                    case 's' -> sandstone;
                    case 'n' -> magma;
                    default -> water;
                };
                level.setBlock(min.offset(dx, 0, dz), state, 3);
            }
        }

        // 3) y = py 与 y = py+1：4x4 置空，保证站位与头部净空。
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dy = 1; dy <= 2; dy++) {
            for (int dx = 0; dx < 4; dx++) {
                for (int dz = 0; dz < 4; dz++) {
                    level.setBlock(min.offset(dx, dy, dz), air, 3);
                }
            }
        }

        source.sendSuccess(() -> Component.literal("§a[天象之境] 已在脚下成功搭建未激活传送门！向水中丢入气候碎片即可激活。"), true);
        return 1;
    }
}
