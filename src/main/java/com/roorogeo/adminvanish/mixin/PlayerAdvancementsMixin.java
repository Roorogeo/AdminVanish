package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanished players don't earn advancement progress, so nothing gets announced in chat. */
@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
	@Shadow
	private ServerPlayer player;

	@Inject(method = "award", at = @At("HEAD"), cancellable = true)
	private void adminvanish$blockWhileVanished(AdvancementHolder advancement, String criterion, CallbackInfoReturnable<Boolean> cir) {
		if (this.player != null && VanishManager.isVanished(this.player)) {
			cir.setReturnValue(false);
		}
	}
}
