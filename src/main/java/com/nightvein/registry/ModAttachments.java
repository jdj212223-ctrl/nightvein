package com.nightvein.registry;

import com.nightvein.Nightvein;
import com.nightvein.vamp.VampData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Nightvein.MODID);

    /** Saved to player NBT and copied across death: dying doesn't cure you, and dying while turning finishes the job. */
    public static final Supplier<AttachmentType<VampData>> VAMP = ATTACHMENTS.register("vamp",
            () -> AttachmentType.builder(VampData::new).serialize(VampData.CODEC).copyOnDeath().build());

    private ModAttachments() {}
}
