package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanished players don't cause game events, so sculk sensors, wardens etc. don't react to them. */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {
	@Inject(method = "gameEvent", at = @At("HEAD"), cancellable = true)
	private void adminvanish$noVibrations(Holder<GameEvent> gameEvent, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
		if (VanishManager.isVanished(context.sourceEntity())) {
			ci.cancel();
		}
	}
}
