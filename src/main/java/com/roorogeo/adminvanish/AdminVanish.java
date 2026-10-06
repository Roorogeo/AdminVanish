package com.roorogeo.adminvanish;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.slf4j.LoggerFactory;

public class AdminVanish implements ModInitializer {
	public static final String MOD_ID = "adminvanish";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> VanishCommand.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTING.register(VanishManager::onServerStarting);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> VanishManager.onServerStopped());
		ServerTickEvents.END_SERVER_TICK.register(server -> VanishManager.tick());

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> VanishManager.onJoin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> VanishManager.onDisconnect(handler.player));

		ServerMessageEvents.ALLOW_GAME_MESSAGE.register((server, message, overlay) -> VanishManager.allowGameMessage(message));

		if (Boolean.getBoolean("adminvanish.mixinAudit")) {
			// Used by CI: load every mixin target so a broken injection fails the build.
			ServerLifecycleEvents.SERVER_STARTED.register(server -> {
				MixinEnvironment.getCurrentEnvironment().audit();
				LOGGER.info("AdminVanish mixin audit passed");
				server.halt(false);
			});
		}
	}
}
