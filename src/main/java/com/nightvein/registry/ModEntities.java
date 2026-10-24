package com.nightvein.registry;

import com.nightvein.Nightvein;
import com.nightvein.entity.Nightwalker;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Nightvein.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Nightwalker>> NIGHTWALKER = ENTITIES.register("nightwalker",
            () -> EntityType.Builder.of(Nightwalker::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .eyeHeight(1.74f)
                    .clientTrackingRange(8)
                    .build("nightwalker"));

    private ModEntities() {}
}
