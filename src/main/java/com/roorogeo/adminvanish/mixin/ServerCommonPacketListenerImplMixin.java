package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Runs every outgoing play packet through {@link VanishManager#filterOutgoing}. */
@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerImplMixin {
	@ModifyVariable(method = "send", at = @At("HEAD"), argsOnly = true)
	private Packet<?> adminvanish$filterPacket(Packet<?> packet) {
		if ((Object) this instanceof ServerGamePacketListenerImpl game && game.player != null) {
			return VanishManager.filterOutgoing(game.player, packet);
		}
		return packet;
	}
}
