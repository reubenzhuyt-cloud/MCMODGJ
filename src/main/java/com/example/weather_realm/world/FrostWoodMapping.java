package com.example.weather_realm.world;

import java.util.Map;

import com.example.weather_realm.ModBlocks;
import com.google.common.collect.ImmutableMap;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Maps the vanilla snowy-village spruce palette onto the mod's frost wood set while
 * preserving every matching {@link Property} (facing / half / open / axis / waterlogged ...).
 */
public final class FrostWoodMapping {
    private static volatile Map<Block, Block> mapping;

    private FrostWoodMapping() {
    }

    public static BlockState remap(BlockState state) {
        Block target = map().get(state.getBlock());
        if (target == null || target == state.getBlock()) {
            return state;
        }
        BlockState result = target.defaultBlockState();
        for (Property<?> property : state.getProperties()) {
            result = copyProperty(result, state, property);
        }
        return result;
    }

    private static BlockState copyProperty(BlockState target, BlockState source, Property<?> property) {
        Property<?> targetProperty = target.getBlock().getStateDefinition().getProperty(property.getName());
        if (targetProperty == null) {
            return target;
        }
        return setValue(target, targetProperty, source, property);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setValue(BlockState target, Property targetProperty, BlockState source, Property sourceProperty) {
        return target.setValue(targetProperty, (Comparable) source.getValue(sourceProperty));
    }

    private static Map<Block, Block> map() {
        Map<Block, Block> current = mapping;
        if (current == null) {
            synchronized (FrostWoodMapping.class) {
                current = mapping;
                if (current == null) {
                    ImmutableMap.Builder<Block, Block> builder = ImmutableMap.builder();
                    builder.put(Blocks.SPRUCE_LOG, ModBlocks.FROST_LOG.get());
                    builder.put(Blocks.SPRUCE_WOOD, ModBlocks.FROST_WOOD.get());
                    builder.put(Blocks.STRIPPED_SPRUCE_LOG, ModBlocks.STRIPPED_FROST_LOG.get());
                    builder.put(Blocks.STRIPPED_SPRUCE_WOOD, ModBlocks.STRIPPED_FROST_WOOD.get());
                    builder.put(Blocks.SPRUCE_PLANKS, ModBlocks.FROST_PLANKS.get());
                    builder.put(Blocks.SPRUCE_STAIRS, ModBlocks.FROST_STAIRS.get());
                    builder.put(Blocks.SPRUCE_SLAB, ModBlocks.FROST_SLAB.get());
                    builder.put(Blocks.SPRUCE_FENCE, ModBlocks.FROST_FENCE.get());
                    builder.put(Blocks.SPRUCE_FENCE_GATE, ModBlocks.FROST_FENCE_GATE.get());
                    builder.put(Blocks.SPRUCE_DOOR, ModBlocks.FROST_DOOR.get());
                    builder.put(Blocks.SPRUCE_TRAPDOOR, ModBlocks.FROST_TRAPDOOR.get());
                    builder.put(Blocks.SPRUCE_PRESSURE_PLATE, ModBlocks.FROST_PRESSURE_PLATE.get());
                    builder.put(Blocks.SPRUCE_BUTTON, ModBlocks.FROST_BUTTON.get());
                    current = builder.build();
                    mapping = current;
                }
            }
        }
        return current;
    }
}
