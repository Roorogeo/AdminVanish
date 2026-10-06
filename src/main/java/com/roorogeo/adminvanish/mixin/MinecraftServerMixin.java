package com.roorogeo.adminvanish.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Leaves vanished players out of the server list's player count and name sample. */
@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
	@ModifyExpressionValue(method = "buildPlayerStatus", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"))
	private List<ServerPlayer> adminvanish$hideVanished(List<ServerPlayer> players) {
		return VanishManager.visiblePlayers(players, null);
	}
}
