package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import com.roorogeo.adminvanish.duck.VanishTrackedEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops vanished players' entities (model, name tag, armor, ...) from being sent to players who can't see them. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements VanishTrackedEntity {
	@Shadow
	@Final
	Entity entity;

	@Shadow
	public abstract void updatePlayer(ServerPlayer player);

	@Shadow
	public abstract void removePlayer(ServerPlayer player);

	@Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
	private void adminvanish$hideVanished(ServerPlayer viewer, CallbackInfo ci) {
		if (this.entity instanceof ServerPlayer target && VanishManager.shouldHide(target, viewer)) {
			this.removePlayer(viewer);
			ci.cancel();
		}
	}

	@Override
	public void adminvanish$refresh(ServerPlayer viewer) {
		this.updatePlayer(viewer);
	}
}
