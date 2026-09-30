package com.example.weather_realm.world;

import java.util.Map;

import com.example.weather_realm.ModStructureTypes;
import com.example.weather_realm.WeatherRealm;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Swaps the vanilla spruce blocks of a snowy village for the mod's frost wood blocks,
 * copying every compatible block state property so orientation and shape are kept.
 *
 * <p>It also rewrites the entities baked into the village pieces so that any overworld pig or
 * cat that would normally spawn in a snowy village is replaced by its frost counterpart.</p>
 */
public class FrostWoodProcessor extends StructureProcessor {
    public static final FrostWoodProcessor INSTANCE = new FrostWoodProcessor();
    public static final MapCodec<FrostWoodProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    /** Legacy/wild entity ids that must never appear inside the frozen village. */
    private static final Map<String, String> ENTITY_REPLACEMENTS = Map.of(
            "minecraft:pig", WeatherRealm.MODID + ":frost_pig",
            "minecraft:cat", WeatherRealm.MODID + ":frost_cat");

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructureTypes.FROST_WOOD_PROCESSOR.get();
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo blockInfo,
            StructureTemplate.StructureBlockInfo relativeBlockInfo,
            StructurePlaceSettings settings) {
        BlockState mapped = FrostWoodMapping.remap(relativeBlockInfo.state());
        if (mapped == relativeBlockInfo.state()) {
            return relativeBlockInfo;
        }
        return new StructureTemplate.StructureBlockInfo(relativeBlockInfo.pos(), mapped, relativeBlockInfo.nbt());
    }

    /**
     * Replaces overworld village animals with the frost variants. The entity type is stored in the
     * {@code id} tag of the entity NBT, so swapping it is enough to spawn the frost mob instead.
     */
    @Override
    public StructureTemplate.StructureEntityInfo processEntity(
            LevelReader level,
            BlockPos seedPos,
            StructureTemplate.StructureEntityInfo rawEntityInfo,
            StructureTemplate.StructureEntityInfo entityInfo,
            StructurePlaceSettings settings,
            StructureTemplate template) {
        CompoundTag nbt = entityInfo.nbt;
        if (nbt == null || !nbt.contains("id", Tag.TAG_STRING)) {
            return entityInfo;
        }
        String replacement = ENTITY_REPLACEMENTS.get(nbt.getString("id"));
        if (replacement == null) {
            return entityInfo;
        }
        CompoundTag replacementNbt = nbt.copy();
        replacementNbt.putString("id", replacement);
        return new StructureTemplate.StructureEntityInfo(entityInfo.pos, entityInfo.blockPos, replacementNbt);
    }
}
