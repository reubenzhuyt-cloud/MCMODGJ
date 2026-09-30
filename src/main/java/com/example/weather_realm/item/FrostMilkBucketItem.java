package com.example.weather_realm.item;

import net.minecraft.world.item.MilkBucketItem;

/**
 * 冰牛奶桶 / Frost Milk Bucket.
 *
 * <p>Obtained by milking a {@code frost_cow} with an empty bucket. Drinking inherits the vanilla
 * milk behaviour: a 32 tick {@code UseAnim.DRINK} animation, curing the drinker of the effects that
 * milk cures, and returning an empty {@code Items.BUCKET}.</p>
 */
public class FrostMilkBucketItem extends MilkBucketItem {
    public FrostMilkBucketItem(Properties properties) {
        super(properties);
    }
}
