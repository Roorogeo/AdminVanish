package com.roorogeo.adminvanish.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.class)
public interface ChunkMapAccessor {
	/** Entity id to {@code ChunkMap.TrackedEntity}. */
	@Accessor("entityMap")
	Int2ObjectMap<?> adminvanish$getEntityMap();
}
