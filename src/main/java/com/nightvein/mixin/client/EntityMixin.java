package com.nightvein.mixin.client;

import com.nightvein.client.BloodSense;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blood sense colours the outline by what's inside: red for blood, grey for nothing at all. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void nightvein$bloodColour(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide && BloodSense.senses(self)) cir.setReturnValue(BloodSense.colour(self));
    }
}
