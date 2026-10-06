package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Silences the vanilla "left the game" message for vanished players. */
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "removePlayerFromWorld", at = @At("HEAD"))
	private void adminvanish$beginSilentLeave(CallbackInfo ci) {
		VanishManager.beginSilence(this.player);
	}

	@Inject(method = "removePlayerFromWorld", at = @At("RETURN"))
	private void adminvanish$endSilentLeave(CallbackInfo ci) {
		VanishManager.endSilence();
	}
}
