package com.roorogeo.adminvanish.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Leaves vanished players out of /list (names and count) for players who can't see them. */
@Mixin(ListPlayersCommand.class)
public class ListPlayersCommandMixin {
	@ModifyExpressionValue(method = "format", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"))
	private static List<ServerPlayer> adminvanish$hideVanished(List<ServerPlayer> players, @Local(argsOnly = true) CommandSourceStack source) {
		ServerPlayer viewer = source.getPlayer();
		// The console and command blocks still get the full list.
		return viewer == null ? players : VanishManager.visiblePlayers(players, viewer);
	}
}
