package com.example.weather_realm.block;

import com.example.weather_realm.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 冰树莓丛 / Frost Raspberry Bush - a frozen sweet-berry bush.
 *
 * <p>Identical to vanilla {@link SweetBerryBushBlock} except that the berries it drops are
 * {@link ModItems#FROST_RASPBERRY} and that it may also be planted on the frost topsoil
 * ({@link FrostPlantBlock#isFrostPlantable}). Growth, bone meal, the slowdown / prick damage,
 * the age-dependent shape and the random-tick cadence are all inherited unchanged.</p>
 */
public class FrostRaspberryBushBlock extends SweetBerryBushBlock {
    public FrostRaspberryBushBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.FARMLAND)
                || FrostPlantBlock.isFrostPlantable(state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        int age = state.getValue(AGE);
        boolean fullyGrown = age == 3;
        if (age > 1) {
            int count = 1 + level.random.nextInt(2);
            popResource(level, pos, new ItemStack(ModItems.FROST_RASPBERRY.get(), count + (fullyGrown ? 1 : 0)));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F,
                    0.8F + level.random.nextFloat() * 0.4F);
            BlockState picked = state.setValue(AGE, 1);
            level.setBlock(pos, picked, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.FROST_RASPBERRY.get());
    }
}
