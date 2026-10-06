package com.roorogeo.adminvanish;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.lang.reflect.Method;

/**
 * Permission checks. Uses the Fabric Permissions API (LuckPerms etc.) when it is installed,
 * and otherwise falls back to operator status (gamemaster level, i.e. op level 2+).
 */
public final class VanishPermissions {
	public static final String USE = "adminvanish.use";
	public static final String SEE = "adminvanish.see";
	public static final String INVSEE = "adminvanish.invsee";
	public static final String ENDERSEE = "adminvanish.endersee";

	private static final Method PERMISSIONS_CHECK = findPermissionsApi();

	private VanishPermissions() {
	}

	public static boolean check(CommandSourceStack source, String permission) {
		boolean isOp = source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
		if (PERMISSIONS_CHECK != null) {
			try {
				return (boolean) PERMISSIONS_CHECK.invoke(null, source, permission, isOp);
			} catch (ReflectiveOperationException | RuntimeException e) {
				AdminVanish.LOGGER.debug("Fabric Permissions API check failed for {}", permission, e);
			}
		}
		return isOp;
	}

	public static boolean check(ServerPlayer player, String permission) {
		return check(player.createCommandSourceStack(), permission);
	}

	/** Looks up {@code Permissions.check(SharedSuggestionProvider, String, boolean)} without a hard dependency. */
	private static Method findPermissionsApi() {
		if (!FabricLoader.getInstance().isModLoaded("fabric-permissions-api-v0")) {
			return null;
		}
		try {
			Class<?> permissions = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
			for (Method method : permissions.getMethods()) {
				Class<?>[] params = method.getParameterTypes();
				if (method.getName().equals("check")
						&& params.length == 3
						&& params[0].isAssignableFrom(CommandSourceStack.class)
						&& params[1] == String.class
						&& params[2] == boolean.class) {
					AdminVanish.LOGGER.info("Using Fabric Permissions API for permission checks");
					return method;
				}
			}
		} catch (ClassNotFoundException ignored) {
		}
		AdminVanish.LOGGER.warn("Fabric Permissions API is installed but no usable check method was found; falling back to op level");
		return null;
	}
}
