package com.roorogeo.adminvanish;

import com.roorogeo.adminvanish.duck.VanishTrackedEntity;
import com.roorogeo.adminvanish.mixin.ChunkMapAccessor;
import com.roorogeo.adminvanish.mixin.PlayerInfoUpdatePacketAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds vanish state and applies it to the world. Everything here runs on the server thread.
 */
public final class VanishManager {
	private static final int PERMISSION_REFRESH_TICKS = 40;

	private static Map<UUID, VanishStorage.Entry> vanished = new HashMap<>();
	/** Cached "can see vanished players" permission per online player. */
	private static final Map<UUID, Boolean> canSeeCache = new HashMap<>();
	/** Player whose vanilla join/leave message is currently being suppressed. */
	private static UUID silenced;
	private static MinecraftServer server;
	private static int tickCounter;

	private VanishManager() {
	}

	// ---------------------------------------------------------------- lifecycle

	public static void onServerStarting(MinecraftServer minecraftServer) {
		server = minecraftServer;
		vanished = VanishStorage.load();
		canSeeCache.clear();
	}

	public static void onServerStopped() {
		VanishStorage.save(vanished);
		server = null;
		canSeeCache.clear();
	}

	public static void onJoin(ServerPlayer player) {
		VanishStorage.Entry entry = vanished.get(player.getUUID());
		if (entry == null) {
			return;
		}
		entry.name = player.getName().getString();
		applyVanishedState(player);
		player.sendSystemMessage(Component.literal("You joined silently and are still vanished.").withStyle(ChatFormatting.GRAY));
		notifyStaff(player, Component.literal(entry.name + " joined silently (vanished)."));
	}

	public static void onDisconnect(ServerPlayer player) {
		canSeeCache.remove(player.getUUID());
		if (isVanished(player)) {
			notifyStaff(player, Component.literal(player.getName().getString() + " left silently (vanished)."));
		}
	}

	public static void tick() {
		if (server == null) {
			return;
		}
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// No-clip without flying would drop the player through the floor, so keep them flying.
			if (isNoclipActive(player) && !player.getAbilities().flying) {
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			}
		}
		if (++tickCounter % PERMISSION_REFRESH_TICKS == 0) {
			refreshSeePermissions();
		}
	}

	// ---------------------------------------------------------------- queries

	public static boolean isVanished(ServerPlayer player) {
		return vanished.containsKey(player.getUUID());
	}

	/** True for vanished players; false for any other entity, including null. */
	public static boolean isVanished(Entity entity) {
		return entity instanceof ServerPlayer player && isVanished(player);
	}

	/** {@code players} without the vanished ones, unless {@code viewer} can see them (null viewer = hide all). */
	public static List<ServerPlayer> visiblePlayers(List<ServerPlayer> players, ServerPlayer viewer) {
		if (vanished.isEmpty() || viewer != null && canSeeVanished(viewer)) {
			return players;
		}
		return players.stream().filter(player -> !isVanished(player)).toList();
	}

	public static boolean isVanished(UUID uuid) {
		return vanished.containsKey(uuid);
	}

	public static List<String> vanishedNames() {
		List<String> names = new ArrayList<>();
		vanished.forEach((uuid, entry) -> {
			boolean online = server != null && server.getPlayerList().getPlayer(uuid) != null;
			names.add((entry.name == null ? uuid.toString() : entry.name) + (online ? "" : " (offline)"));
		});
		return names;
	}

	/** Vanished players and holders of {@code adminvanish.see} can see vanished players. */
	public static boolean canSeeVanished(ServerPlayer viewer) {
		if (isVanished(viewer)) {
			return true;
		}
		return canSeeCache.computeIfAbsent(viewer.getUUID(), uuid -> VanishPermissions.check(viewer, VanishPermissions.SEE));
	}

	public static boolean shouldHide(ServerPlayer target, ServerPlayer viewer) {
		return target != viewer && isVanished(target) && !canSeeVanished(viewer);
	}

	/**
	 * No-clip is active for vanished players in creative who haven't turned it off.
	 * Server side the player is in creative; the client is told it is a spectator, which
	 * makes the vanilla client move through blocks while keeping the creative inventory.
	 */
	public static boolean isNoclipActive(ServerPlayer player) {
		VanishStorage.Entry entry = vanished.get(player.getUUID());
		return entry != null && entry.noclip && player.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
	}

	// ---------------------------------------------------------------- actions

	public static void vanish(ServerPlayer player, boolean fakeMessage) {
		if (isVanished(player)) {
			return;
		}
		VanishStorage.Entry entry = new VanishStorage.Entry();
		entry.name = player.getName().getString();
		entry.previousGameMode = player.gameMode.getGameModeForPlayer().getName();
		vanished.put(player.getUUID(), entry);
		VanishStorage.save(vanished);

		for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
			if (shouldHide(player, viewer)) {
				refreshTracking(player, viewer);
				viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID())));
			}
		}
		if (fakeMessage) {
			broadcastFakeLeave(player);
		}
		applyVanishedState(player);
		notifyStaff(player, Component.literal(entry.name + " vanished."));
	}

	public static void unvanish(ServerPlayer player, boolean fakeMessage) {
		VanishStorage.Entry entry = vanished.remove(player.getUUID());
		if (entry == null) {
			return;
		}
		VanishStorage.save(vanished);

		for (ServerPlayer viewer : server.getPlayerList().getPlayers()) {
			if (viewer != player && !canSeeVanished(viewer)) {
				viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player)));
				refreshTracking(player, viewer);
			}
		}

		GameType previous = GameType.byName(entry.previousGameMode, GameType.SURVIVAL);
		player.setGameMode(previous);
		// Always resend our real game mode: if it was already creative, setGameMode sent nothing
		// and the client would still believe it is a spectator.
		resendOwnGameMode(player);
		if (fakeMessage) {
			broadcastFakeJoin(player);
		}
		notifyStaff(player, Component.literal(player.getName().getString() + " is no longer vanished."));
	}

	/** Toggles no-clip and returns the new state. */
	public static boolean toggleNoclip(ServerPlayer player) {
		VanishStorage.Entry entry = vanished.get(player.getUUID());
		if (entry == null) {
			return false;
		}
		entry.noclip = !entry.noclip;
		VanishStorage.save(vanished);
		resendOwnGameMode(player);
		if (entry.noclip) {
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		}
		return entry.noclip;
	}

	public static void broadcastFakeJoin(ServerPlayer player) {
		server.getPlayerList().broadcastSystemMessage(
				Component.translatable("multiplayer.player.joined", player.getDisplayName()).withStyle(ChatFormatting.YELLOW), false);
	}

	public static void broadcastFakeLeave(ServerPlayer player) {
		server.getPlayerList().broadcastSystemMessage(
				Component.translatable("multiplayer.player.left", player.getDisplayName()).withStyle(ChatFormatting.YELLOW), false);
	}

	private static void applyVanishedState(ServerPlayer player) {
		player.setGameMode(GameType.CREATIVE);
		player.getAbilities().flying = true;
		player.onUpdateAbilities();
		resendOwnGameMode(player);
	}

	private static void resendOwnGameMode(ServerPlayer player) {
		// Goes through the outgoing packet filter, which swaps in "spectator" when no-clip is active.
		player.connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, player));
	}

	// ---------------------------------------------------------------- visibility plumbing

	private static void refreshTracking(ServerPlayer target, ServerPlayer viewer) {
		if (target == viewer) {
			return;
		}
		ServerLevel level = (ServerLevel) target.level();
		Object tracked = ((ChunkMapAccessor) level.getChunkSource().chunkMap).adminvanish$getEntityMap().get(target.getId());
		if (tracked instanceof VanishTrackedEntity trackedEntity) {
			trackedEntity.adminvanish$refresh(viewer);
		}
	}

	private static void refreshSeePermissions() {
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		for (ServerPlayer viewer : players) {
			boolean couldSee = canSeeVanished(viewer);
			canSeeCache.put(viewer.getUUID(), VanishPermissions.check(viewer, VanishPermissions.SEE));
			boolean canSee = canSeeVanished(viewer);
			if (couldSee == canSee) {
				continue;
			}
			for (ServerPlayer target : players) {
				if (target == viewer || !isVanished(target)) {
					continue;
				}
				if (canSee) {
					viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target)));
					refreshTracking(target, viewer);
				} else {
					refreshTracking(target, viewer);
					viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(target.getUUID())));
				}
			}
		}
	}

	/**
	 * Called for every packet sent to a player. Strips vanished players out of tab list updates
	 * for players who can't see them, and tells a no-clipping player that it is a spectator.
	 */
	public static Packet<?> filterOutgoing(ServerPlayer viewer, Packet<?> packet) {
		if (!(packet instanceof ClientboundPlayerInfoUpdatePacket info) || vanished.isEmpty()) {
			return packet;
		}
		boolean hideOthers = !canSeeVanished(viewer);
		boolean rewriteSelf = isNoclipActive(viewer)
				&& info.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE);
		if (!hideOthers && !rewriteSelf) {
			return packet;
		}

		List<ClientboundPlayerInfoUpdatePacket.Entry> entries = new ArrayList<>();
		boolean changed = false;
		for (ClientboundPlayerInfoUpdatePacket.Entry entry : info.entries()) {
			if (entry.profileId().equals(viewer.getUUID())) {
				if (rewriteSelf && entry.gameMode() != GameType.SPECTATOR) {
					entries.add(withGameMode(entry, GameType.SPECTATOR));
					changed = true;
					continue;
				}
			} else if (hideOthers && isVanished(entry.profileId())) {
				changed = true;
				continue;
			}
			entries.add(entry);
		}
		if (!changed) {
			return packet;
		}
		// The original packet may be shared with other players, so build a new one.
		ClientboundPlayerInfoUpdatePacket copy = new ClientboundPlayerInfoUpdatePacket(info.actions(), List.of());
		((PlayerInfoUpdatePacketAccessor) copy).adminvanish$setEntries(entries);
		return copy;
	}

	private static ClientboundPlayerInfoUpdatePacket.Entry withGameMode(ClientboundPlayerInfoUpdatePacket.Entry entry, GameType gameMode) {
		return new ClientboundPlayerInfoUpdatePacket.Entry(
				entry.profileId(),
				entry.profile(),
				entry.listed(),
				entry.latency(),
				gameMode,
				entry.displayName(),
				entry.showHat(),
				entry.listOrder(),
				entry.chatSession());
	}

	// ---------------------------------------------------------------- silent join / leave

	public static void beginSilence(ServerPlayer player) {
		if (isVanished(player)) {
			silenced = player.getUUID();
		}
	}

	public static void endSilence() {
		silenced = null;
	}

	/** Fabric message hook: drop the vanilla join/leave message of a vanished player. */
	public static boolean allowGameMessage(Component message) {
		if (silenced == null || !(message.getContents() instanceof TranslatableContents translatable)) {
			return true;
		}
		String key = translatable.getKey();
		return !(key.startsWith("multiplayer.player.joined") || key.equals("multiplayer.player.left"));
	}

	private static void notifyStaff(ServerPlayer subject, Component message) {
		Component formatted = Component.literal("[AdminVanish] ").withStyle(ChatFormatting.DARK_AQUA)
				.append(message.copy().withStyle(ChatFormatting.GRAY));
		for (ServerPlayer staff : server.getPlayerList().getPlayers()) {
			if (staff != subject && canSeeVanished(staff)) {
				staff.sendSystemMessage(formatted);
			}
		}
	}
}
