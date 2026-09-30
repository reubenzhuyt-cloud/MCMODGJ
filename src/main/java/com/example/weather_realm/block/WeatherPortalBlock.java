package com.example.weather_realm.block;

import javax.annotation.Nullable;

import com.example.weather_realm.ModDimensions;
import com.example.weather_realm.ModParticles;
import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.config.WeatherRealmConfig;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
            int surfaceY = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING, targetX, targetZ);
            int landingY = Math.max(targetLevel.getMinBuildHeight() + 10, surfaceY);

            // 落地安全保障：脚下生成 3x3 浮冰承台，清空身位 / Safe landing: 3x3 packed-ice platform.
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
