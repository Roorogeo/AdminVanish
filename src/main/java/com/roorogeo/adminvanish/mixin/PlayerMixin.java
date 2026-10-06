package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla resets {@code noPhysics} to "is spectator" every tick. Re-enable it for no-clipping
 * vanished players so the server accepts their movement through blocks.
 */
@Mixin(Player.class)
public class PlayerMixin {
	@Inject(method = "tick", at = @At("RETURN"))
	private void adminvanish$keepNoclip(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayer player && VanishManager.isNoclipActive(player)) {
			player.noPhysics = true;
		}
	}
}
