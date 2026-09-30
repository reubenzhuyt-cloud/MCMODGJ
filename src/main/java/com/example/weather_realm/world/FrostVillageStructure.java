package com.example.weather_realm.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.weather_realm.WeatherRealm;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * A jigsaw structure that mirrors {@link JigsawStructure} but rewrites every placed pool
 * element so the {@link FrostWoodProcessor} is applied. Used by {@code weather_realm:crystal_village}
 * to turn the vanilla snowy village into a frost wood village while leaving bells, workstations,
 * beds and other non-spruce blocks untouched.
 */
public class FrostVillageStructure extends Structure {
    public static final MapCodec<FrostVillageStructure> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
                    ResourceLocation.CODEC.optionalFieldOf("start_jigsaw_name").forGetter(structure -> structure.startJigsawName),
                    Codec.intRange(0, 20).fieldOf("size").forGetter(structure -> structure.maxDepth),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(structure -> structure.startHeight),
                    Codec.BOOL.fieldOf("use_expansion_hack").forGetter(structure -> structure.useExpansionHack),
                    Heightmap.Types.CODEC.optionalFieldOf("project_start_to_heightmap").forGetter(structure -> structure.projectStartToHeightmap),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter),
                    Codec.list(PoolAliasBinding.CODEC).optionalFieldOf("pool_aliases", List.of()).forGetter(structure -> structure.poolAliases),
                    DimensionPadding.CODEC
                            .optionalFieldOf("dimension_padding", JigsawStructure.DEFAULT_DIMENSION_PADDING)
                            .forGetter(structure -> structure.dimensionPadding),
                    LiquidSettings.CODEC
                            .optionalFieldOf("liquid_settings", JigsawStructure.DEFAULT_LIQUID_SETTINGS)
                            .forGetter(structure -> structure.liquidSettings))
                    .apply(instance, FrostVillageStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final Optional<ResourceLocation> startJigsawName;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final boolean useExpansionHack;
    private final Optional<Heightmap.Types> projectStartToHeightmap;
    private final int maxDistanceFromCenter;
    private final List<PoolAliasBinding> poolAliases;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;

    public FrostVillageStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            Optional<ResourceLocation> startJigsawName,
            int maxDepth,
            HeightProvider startHeight,
            boolean useExpansionHack,
            Optional<Heightmap.Types> projectStartToHeightmap,
            int maxDistanceFromCenter,
            List<PoolAliasBinding> poolAliases,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.useExpansionHack = useExpansionHack;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.poolAliases = poolAliases;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int y = this.startHeight.sample(context.random(),
                new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        BlockPos pos = new BlockPos(chunkPos.getMinBlockX(), y, chunkPos.getMinBlockZ());
        Optional<Structure.GenerationStub> stub = JigsawPlacement.addPieces(
                context,
                this.startPool,
                this.startJigsawName,
                this.maxDepth,
                pos,
                this.useExpansionHack,
                this.projectStartToHeightmap,
                this.maxDistanceFromCenter,
                PoolAliasLookup.create(this.poolAliases, pos, context.seed()),
                this.dimensionPadding,
                this.liquidSettings);
        if (stub.isEmpty()) {
            return stub;
        }
        Structure.GenerationStub original = stub.get();
        return Optional.of(new Structure.GenerationStub(original.position(), builder -> {
            original.generator().ifLeft(consumer -> consumer.accept(builder));
            original.generator().ifRight(existing -> existing.build().pieces().forEach(builder::addPiece));
            applyFrostWood(builder, context);
        }));
    }

    private void applyFrostWood(StructurePiecesBuilder builder, Structure.GenerationContext context) {
        List<StructurePiece> pieces = new ArrayList<>(builder.build().pieces());
        builder.clear();
        for (StructurePiece piece : pieces) {
            if (piece instanceof PoolElementStructurePiece poolPiece) {
                StructurePoolElement element = FrostPoolElements.withProcessors(poolPiece.getElement(), FrostWoodProcessor.INSTANCE);
                PoolElementStructurePiece replacement = new PoolElementStructurePiece(
                        context.structureTemplateManager(),
                        element,
                        poolPiece.getPosition(),
                        poolPiece.getGroundLevelDelta(),
                        poolPiece.getRotation(),
                        poolPiece.getBoundingBox(),
                        this.liquidSettings);
                for (JigsawJunction junction : poolPiece.getJunctions()) {
                    replacement.addJunction(junction);
                }
                builder.addPiece(replacement);
            } else {
                builder.addPiece(piece);
            }
        }
    }

    @Override
    public StructureType<?> type() {
        return WeatherRealm.FROST_VILLAGE_STRUCTURE.get();
    }
}
