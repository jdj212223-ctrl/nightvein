package com.nightvein.registry;

import com.nightvein.Nightvein;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Nightvein.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> BITE = reg("bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEARTBEAT = reg("heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURNED = reg("turned");
    public static final DeferredHolder<SoundEvent, SoundEvent> CURE = reg("cure");
    public static final DeferredHolder<SoundEvent, SoundEvent> DASH = reg("dash");
    public static final DeferredHolder<SoundEvent, SoundEvent> MIST = reg("mist");
    public static final DeferredHolder<SoundEvent, SoundEvent> HYPNOTIZE = reg("hypnotize");
    public static final DeferredHolder<SoundEvent, SoundEvent> BAT = reg("bat");
    public static final DeferredHolder<SoundEvent, SoundEvent> TACKLE = reg("tackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOODRUSH = reg("bloodrush");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRASH = reg("crash");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTWALKER_AMBIENT = reg("nightwalker_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTWALKER_HURT = reg("nightwalker_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIGHTWALKER_DEATH = reg("nightwalker_death");

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Nightvein.id(name)));
    }

    private ModSounds() {}
}
