package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Silences the vanilla "joined the game" message for vanished players. */
@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Inject(method = "placeNewPlayer", at = @At("HEAD"))
	private void adminvanish$beginSilentJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		VanishManager.beginSilence(player);
	}

	@Inject(method = "placeNewPlayer", at = @At("RETURN"))
	private void adminvanish$endSilentJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		VanishManager.endSilence();
	}
}
