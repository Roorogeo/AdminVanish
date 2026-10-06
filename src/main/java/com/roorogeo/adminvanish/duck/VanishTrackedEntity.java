package com.roorogeo.adminvanish.duck;

import net.minecraft.server.level.ServerPlayer;

/** Implemented by {@code ChunkMap.TrackedEntity} via mixin. */
public interface VanishTrackedEntity {
	/** Re-evaluates whether {@code viewer} should be tracking this entity. */
	void adminvanish$refresh(ServerPlayer viewer);
}
