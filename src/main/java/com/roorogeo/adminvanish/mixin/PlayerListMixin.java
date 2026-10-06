package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerList.class)
public class PlayerListMixin {
	@Shadow
	@Final
	private List<ServerPlayer> players;

	/** Silences the vanilla "joined the game" message for vanished players. */
	@Inject(method = "placeNewPlayer", at = @At("HEAD"))
	private void adminvanish$beginSilentJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		VanishManager.beginSilence(player);
	}

	@Inject(method = "placeNewPlayer", at = @At("RETURN"))
	private void adminvanish$endSilentJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
		VanishManager.endSilence();
	}

	/**
	 * Sounds and level events (block breaking/placing, doors, ...) caused by a player are sent
	 * through here with that player excluded. For vanished players, only staff hear them.
	 */
	@Inject(method = "broadcast", at = @At("HEAD"), cancellable = true)
	private void adminvanish$hideVanishedSounds(Player except, double x, double y, double z, double radius,
			ResourceKey<Level> dimension, Packet<?> packet, CallbackInfo ci) {
		if (!VanishManager.isVanished(except)) {
			return;
		}
		ci.cancel();
		for (ServerPlayer player : this.players) {
			if (player == except || player.level().dimension() != dimension || !VanishManager.canSeeVanished(player)) {
				continue;
			}
			double dx = x - player.getX();
			double dy = y - player.getY();
			double dz = z - player.getZ();
			if (dx * dx + dy * dy + dz * dz < radius * radius) {
				player.connection.send(packet);
			}
		}
	}
}
