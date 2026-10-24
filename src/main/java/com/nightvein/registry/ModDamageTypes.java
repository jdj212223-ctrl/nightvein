package com.nightvein.registry;

import com.nightvein.Nightvein;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** The damage types themselves are JSON under data/nightvein/damage_type. */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> BITE = ResourceKey.create(Registries.DAMAGE_TYPE, Nightvein.id("bite"));
    public static final ResourceKey<DamageType> THIRST = ResourceKey.create(Registries.DAMAGE_TYPE, Nightvein.id("thirst"));

    private ModDamageTypes() {}

    public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), attacker);
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> type) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type));
    }
}
