package com.roorogeo.adminvanish;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;

/**
 * <pre>
 * /adminvanish                 toggle vanish (with a fake leave/join message)
 * /adminvanish &lt;player&gt;        vanish (if not already) and teleport to the player
 * /adminvanish tp &lt;player&gt;     same as above
 * /adminvanish quiet           toggle vanish without a fake message
 * /adminvanish fakejoin        broadcast a fake "joined the game" message
 * /adminvanish fakeleave       broadcast a fake "left the game" message
 * /adminvanish noclip          toggle moving through blocks while vanished
 * /adminvanish list            list vanished players
 * </pre>
 */
public final class VanishCommand {
	private VanishCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("adminvanish")
				.requires(source -> VanishPermissions.check(source, VanishPermissions.USE))
				.executes(ctx -> toggle(ctx, true))
				.then(Commands.literal("quiet").executes(ctx -> toggle(ctx, false)))
				.then(Commands.literal("fakejoin").executes(ctx -> fakeMessage(ctx, true)))
				.then(Commands.literal("fakeleave").executes(ctx -> fakeMessage(ctx, false)))
				.then(Commands.literal("noclip").executes(VanishCommand::noclip))
				.then(Commands.literal("list").executes(VanishCommand::list))
				.then(Commands.literal("tp")
						.then(Commands.argument("player", EntityArgument.player()).executes(VanishCommand::teleport)))
				.then(Commands.argument("player", EntityArgument.player()).executes(VanishCommand::teleport)));
	}

	private static int toggle(CommandContext<CommandSourceStack> ctx, boolean fakeMessage) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		if (VanishManager.isVanished(player)) {
			VanishManager.unvanish(player, fakeMessage);
			ctx.getSource().sendSuccess(() -> Component.literal("You are no longer vanished.").withStyle(ChatFormatting.GREEN), false);
		} else {
			VanishManager.vanish(player, fakeMessage);
			ctx.getSource().sendSuccess(() -> Component.literal("You are now vanished.").withStyle(ChatFormatting.GREEN), false);
		}
		return 1;
	}

	private static int teleport(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
		if (target == player) {
			ctx.getSource().sendFailure(Component.literal("You can't teleport to yourself."));
			return 0;
		}
		if (!VanishManager.isVanished(player)) {
			VanishManager.vanish(player, true);
		}
		player.teleportTo((ServerLevel) target.level(), target.getX(), target.getY(), target.getZ(), Set.of(),
				target.getYRot(), target.getXRot(), true);
		ctx.getSource().sendSuccess(() -> Component.literal("Vanished and teleported to ").withStyle(ChatFormatting.GREEN)
				.append(target.getDisplayName()), false);
		return 1;
	}

	private static int fakeMessage(CommandContext<CommandSourceStack> ctx, boolean join) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		if (join) {
			VanishManager.broadcastFakeJoin(player);
		} else {
			VanishManager.broadcastFakeLeave(player);
		}
		return 1;
	}

	private static int noclip(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		if (!VanishManager.isVanished(player)) {
			ctx.getSource().sendFailure(Component.literal("You need to be vanished to toggle no-clip."));
			return 0;
		}
		boolean enabled = VanishManager.toggleNoclip(player);
		ctx.getSource().sendSuccess(() -> Component.literal(enabled
				? "No-clip enabled. Use the inventory screen to change hotbar slots."
				: "No-clip disabled.").withStyle(ChatFormatting.GREEN), false);
		return 1;
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		List<String> names = VanishManager.vanishedNames();
		ctx.getSource().sendSuccess(() -> Component.literal(names.isEmpty()
				? "Nobody is vanished."
				: "Vanished (" + names.size() + "): " + String.join(", ", names)), false);
		return names.size();
	}
}
