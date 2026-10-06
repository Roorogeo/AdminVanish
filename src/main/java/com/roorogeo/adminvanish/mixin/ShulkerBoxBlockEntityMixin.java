package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shulker boxes track openers themselves; skip vanished players so the box stays shut and silent. */
@Mixin(ShulkerBoxBlockEntity.class)
public class ShulkerBoxBlockEntityMixin {
	@Shadow
	private int openCount;

	@Inject(method = "startOpen", at = @At("HEAD"), cancellable = true)
	private void adminvanish$silentOpen(ContainerUser user, CallbackInfo ci) {
		if (VanishManager.isVanished(user.getLivingEntity())) {
			ci.cancel();
		}
	}

	@Inject(method = "stopOpen", at = @At("HEAD"), cancellable = true)
	private void adminvanish$silentClose(ContainerUser user, CallbackInfo ci) {
		if (VanishManager.isVanished(user.getLivingEntity()) || this.openCount <= 0) {
			ci.cancel();
		}
	}
}
