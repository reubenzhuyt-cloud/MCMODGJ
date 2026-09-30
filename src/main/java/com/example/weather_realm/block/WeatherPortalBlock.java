package com.example.weather_realm.block;

import javax.annotation.Nullable;

import com.example.weather_realm.ModDimensions;
import com.example.weather_realm.ModParticles;
import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.config.WeatherRealmConfig;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 气候传送门 / Weather Portal.
 *
 * <p>A non-solid, glowing slab of condensed storm. Behaviour mirrors vanilla {@code EndPortalBlock}:
 * {@link #getShape} returns {@link Shapes#empty()} so the gate has no outline and can never be
 * broken by hand, and travel is delegated to the native {@link Portal} pipeline via
 * {@link #getPortalDestination}. The engine's portal ticket handling loads the target chunks
 * safely off the critical path, so no synchronous chunk access happens on the server thread.</p>
 */
public class WeatherPortalBlock extends Block implements Portal {
    public static final MapCodec<WeatherPortalBlock> CODEC = simpleCodec(WeatherPortalBlock::new);

    public WeatherPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    /**
     * 空选框 / Empty outline: makes the gate unselectable and unbreakable by hand in both survival
     * and creative mode, exactly like a light/air block.
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** 水流 / 岩浆不可冲毁传送门 / Fluids may never wash the gate away. */
    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }

    /** 禁止中键吸取 / No middle-click pick-block, the gate is not an obtainable block. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos pos) {
        MinecraftServer server = currentLevel.getServer();
        ResourceKey<Level> targetDim = currentLevel.dimension().equals(ModDimensions.CRYSTAL_REALM)
                ? Level.OVERWORLD
                : ModDimensions.CRYSTAL_REALM;
        ServerLevel targetLevel = server.getLevel(targetDim);
        if (targetLevel == null) {
            return null;
        }

        Vec3 targetVec;
        if (targetDim.equals(Level.OVERWORLD)) {
            BlockPos spawn = targetLevel.getSharedSpawnPos();
            targetVec = Vec3.atBottomCenterOf(spawn);
        } else {
            // 搜索最近的极寒群系 / Search for the nearest frozen biome so the player always lands
            // inside the blizzard region instead of an arbitrary chunk.
            ResourceLocation crystalPlainsId =
                    ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "crystal_plains");
            BlockPos searchCenter = new BlockPos(pos.getX(), 64, pos.getZ());
            int searchRadius = Math.max(1, WeatherRealmConfig.PORTAL_SEARCH_RADIUS.getAsInt());
            int horizontalStep = Math.max(1, WeatherRealmConfig.PORTAL_SEARCH_HORIZONTAL_STEP.getAsInt());
            int verticalStep = Math.max(1, WeatherRealmConfig.PORTAL_SEARCH_VERTICAL_STEP.getAsInt());
            var closest = targetLevel.findClosestBiome3d(
                    holder -> holder.is(crystalPlainsId),
                    searchCenter,
                    searchRadius,   // 搜索半径 / search radius in blocks
                    horizontalStep, // 水平步长 / horizontal step
                    verticalStep    // 垂直步长 / vertical step
            );
            BlockPos targetXZ = (closest != null) ? closest.getFirst() : searchCenter;
            int targetX = targetXZ.getX();
            int targetZ = targetXZ.getZ();

            // 先强制加载目标区块再取高度，并在最高方块之上扫描安全站立点。
            // 原实现直接调用 Level#getHeight：该方法对未加载区块会返回 getMinBuildHeight()，
            // 于是落点被夹到 minBuildHeight+10 附近的地底（见 Level.java:383-398）。
            int landingY = findSafeLandingY(targetLevel, targetX, targetZ);
            if (landingY == UNSAFE_LANDING) {
                // 兜底：目标区块拿不到有效高度时，回退到该维度共享出生点并做同样的安全扫描。
                BlockPos spawn = targetLevel.getSharedSpawnPos();
                targetX = spawn.getX();
                targetZ = spawn.getZ();
                landingY = findSafeLandingY(targetLevel, targetX, targetZ);
            }
            if (landingY == UNSAFE_LANDING) {
                // 极端兜底：仍无安全点，则退回到共享出生点 Y 并夹取到合法建造范围，绝不再出现世界底部落点。
                BlockPos spawn = targetLevel.getSharedSpawnPos();
                targetX = spawn.getX();
                targetZ = spawn.getZ();
                landingY = Mth.clamp(spawn.getY(),
                        targetLevel.getMinBuildHeight() + 1,
                        targetLevel.getMaxBuildHeight() - 2);
            }

            // 落地安全保障：脚下生成 3x3 浮冰承台，清空身位 / Safe landing: 3x3 packed-ice platform.
            // 承台方块位于 landingY - 1，玩家脚底位于 landingY，脚底严格高于承台方块。
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos floorPos = new BlockPos(targetX + dx, landingY - 1, targetZ + dz);
                    if (targetLevel.isEmptyBlock(floorPos) || targetLevel.getBlockState(floorPos).canBeReplaced()) {
                        targetLevel.setBlockAndUpdate(floorPos, Blocks.PACKED_ICE.defaultBlockState());
                    }
                    targetLevel.setBlockAndUpdate(floorPos.above(), Blocks.AIR.defaultBlockState());
                    targetLevel.setBlockAndUpdate(floorPos.above(2), Blocks.AIR.defaultBlockState());
                }
            }
            targetVec = new Vec3(targetX + 0.5D, landingY, targetZ + 0.5D);
        }

        return new DimensionTransition(
                targetLevel,
                targetVec,
                Vec3.ZERO,
                entity.getYRot(),
                entity.getXRot(),
                DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET)
        );
    }

    /** 落点安全扫描失败的哨兵值 / Sentinel for a failed safe-landing scan. */
    private static final int UNSAFE_LANDING = Integer.MIN_VALUE;

    /**
     * 取目标 XZ 的安全落点 Y（玩家脚底）。
     *
     * <p>先强制加载/生成目标区块，再用该区块的高度图取“最高非树叶方块之上”的第一格；
     * 高度图对未加载区块会退化到世界底部，因此区块必须先生成。随后在
     * {@code [getMinBuildHeight()+1, getMaxBuildHeight()-2]} 范围内向上扫描第一个
     * 脚底与头顶两格可站立、且脚下可承托的空位，保证玩家落在最高的方块之上。</p>
     *
     * @return 安全落点 Y，或 {@link #UNSAFE_LANDING}
     */
    private static int findSafeLandingY(ServerLevel level, int x, int z) {
        int lower = level.getMinBuildHeight() + 1;
        int upper = level.getMaxBuildHeight() - 2;
        if (lower > upper) {
            return UNSAFE_LANDING;
        }

        // 强制加载目标区块（可能触发同步生成），生成后其高度图才是权威的。
        LevelChunk chunk = level.getChunk(
                SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(z));

        // MOTION_BLOCKING_NO_LEAVES 忽略树叶，避免把落点定在树冠上；+1 得到“最高方块之上”的第一格。
        int firstFreeY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
        int startY = Mth.clamp(firstFreeY, lower, upper);

        for (int y = startY; y <= upper; y++) {
            if (isSafeStandingSpot(level, x, y, z)) {
                return y;
            }
        }
        return UNSAFE_LANDING;
    }

    /** 脚底与头顶两格可替换/为空，且脚下那格可承托（固体或有碰撞体，否则会被承台填补）。 */
    private static boolean isSafeStandingSpot(ServerLevel level, int x, int y, int z) {
        BlockPos feet = new BlockPos(x, y, z);
        BlockPos head = feet.above();
        BlockPos ground = feet.below();

        if (!isPassable(level.getBlockState(feet)) || !isPassable(level.getBlockState(head))) {
            return false;
        }
        BlockState groundState = level.getBlockState(ground);
        return isPassable(groundState) || !groundState.getCollisionShape(level, ground).isEmpty();
    }

    /** 空气或可被替换 / Air or replaceable. */
    private static boolean isPassable(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 4; i++) {
            double px = pos.getX() + random.nextDouble();
            double py = pos.getY() + random.nextDouble() * 0.7D;
            double pz = pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.END_ROD, px, py, pz,
                    0.0D, 0.015D + random.nextDouble() * 0.02D, 0.0D);
        }
        if (random.nextInt(3) == 0) {
            double px = pos.getX() + random.nextDouble();
            double py = pos.getY() + random.nextDouble();
            double pz = pos.getZ() + random.nextDouble();
            level.addParticle(ModParticles.BLIZZARD_SNOW.get(), px, py, pz, 0.0D, 0.04D, 0.0D);
        }
    }
}
