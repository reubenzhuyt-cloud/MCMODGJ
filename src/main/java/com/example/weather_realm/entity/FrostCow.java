package com.example.weather_realm.entity;

import com.example.weather_realm.ModEntities;
import com.example.weather_realm.WeatherRealm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 冰原牛 / Frost Cow - a cow adapted to the eternal blizzard. Breeds with wheat and is milkable,
 * producing a {@code frost_milk_bucket} instead of vanilla milk.
 */
public class FrostCow extends Cow {
    public FrostCow(EntityType<? extends Cow> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public FrostCow getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.FROST_COW.get().create(level);
    }

    /** Milking with an empty bucket fills a {@code frost_milk_bucket}. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (itemstack.is(Items.BUCKET) && !this.isBaby()) {
            player.playSound(SoundEvents.COW_MILK, 1.0F, 1.0F);
            ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, player,
                    new ItemStack(WeatherRealm.FROST_MILK_BUCKET.get()));
            player.setItemInHand(hand, itemstack1);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            return super.mobInteract(player, hand);
        }
    }
}
