package com.nightvein.mixin.compat;

import com.nightvein.vamp.Stuns;
import com.voidclip.entity.BackroomsMonster;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * liminal. (VoidClip): something a vampire has tackled flat can't grab anyone until it gets back up.
 * Skipped entirely when VoidClip isn't installed.
 */
@Pseudo
@Mixin(targets = "com.voidclip.entity.GrabManager", remap = false)
public abstract class GrabManagerMixin {
    @Inject(method = "start(Lnet/minecraft/server/level/ServerPlayer;Lcom/voidclip/entity/BackroomsMonster;FI)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void nightvein$notWhileDown(ServerPlayer player, BackroomsMonster grabber, float gain, int duration,
                                               CallbackInfoReturnable<Boolean> cir) {
        if ((Object) grabber instanceof LivingEntity e && Stuns.isDowned(e)) cir.setReturnValue(false);
    }
}
