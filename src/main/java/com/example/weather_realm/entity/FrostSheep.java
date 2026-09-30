package com.example.weather_realm.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.example.weather_realm.WeatherRealm;
import com.example.weather_realm.ModBlocks;
import com.example.weather_realm.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 冰原羊 / Frost Sheep - a sheep adapted to the eternal blizzard. Breeds with wheat, is sheared
 * for {@code frost_wool}, and drops frost mutton on death.
 */
public class FrostSheep extends Sheep {
    private static final ResourceKey<LootTable> LOOT =
            ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "entities/frost_sheep"));
    private static final ResourceKey<LootTable> LOOT_SHEARED =
            ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(WeatherRealm.MODID, "entities/frost_sheep_sheared"));

    public FrostSheep(EntityType<? extends Sheep> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public FrostSheep getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        FrostSheep baby = ModEntities.FROST_SHEEP.get().create(level);
        if (baby != null) {
            baby.setColor(this.getColor());
        }
        return baby;
    }

    @Override
    public ResourceKey<LootTable> getDefaultLootTable() {
        return this.isSheared() ? LOOT_SHEARED : LOOT;
    }

    /** Drops 1-3 {@code frost_wool} instead of vanilla wool. */
    @Override
    public void shear(SoundSource source) {
        this.level().playSound(null, this, SoundEvents.SHEEP_SHEAR, source, 1.0F, 1.0F);
        if (!this.level().isClientSide) {
            this.setSheared(true);
            int count = 1 + this.random.nextInt(3);
            for (int i = 0; i < count; i++) {
                ItemEntity drop = this.spawnAtLocation(new ItemStack(ModBlocks.FROST_WOOL.get()), 1.0F);
                if (drop != null) {
                    drop.setDeltaMovement(drop.getDeltaMovement().add(
                            (this.random.nextFloat() - this.random.nextFloat()) * 0.1F,
                            this.random.nextFloat() * 0.05F,
                            (this.random.nextFloat() - this.random.nextFloat()) * 0.1F));
                }
            }
        }
    }

    /**
     * NeoForge {@code IShearable} entry point (shears / dispensers). Delegates to {@link #shear}
     * and returns the captured drops, mirroring the vanilla behaviour.
     */
    @Override
    public List<ItemStack> onSheared(@Nullable Player player, ItemStack item, Level level, BlockPos pos) {
        if (level.isClientSide) {
            return List.of();
        }
        Collection<ItemEntity> previous = this.captureDrops(new ArrayList<>());
        this.shear(player == null ? SoundSource.BLOCKS : SoundSource.PLAYERS);
        return this.captureDrops(previous).stream().map(ItemEntity::getItem).toList();
    }
}
