package com.nightvein.mixin.client;

import com.nightvein.client.BloodSense;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blood sense: anything alive nearby gets an outline, which draws through walls. */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void nightvein$bloodSense(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (BloodSense.senses(entity)) cir.setReturnValue(true);
    }
}
