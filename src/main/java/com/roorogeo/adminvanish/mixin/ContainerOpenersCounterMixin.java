package com.roorogeo.adminvanish.mixin;

import com.roorogeo.adminvanish.VanishManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Chests, trapped/copper chests, ender chests and barrels: vanished players don't count as
 * openers, so there is no lid animation, open/close sound or vibration.
 */
@Mixin(ContainerOpenersCounter.class)
public class ContainerOpenersCounterMixin {
	@Shadow
	private int openCount;

	@Inject(method = "incrementOpeners", at = @At("HEAD"), cancellable = true)
	private void adminvanish$silentOpen(LivingEntity entity, Level level, BlockPos pos, BlockState state, double range, CallbackInfo ci) {
		if (VanishManager.isVanished(entity)) {
			ci.cancel();
		}
	}

	@Inject(method = "decrementOpeners", at = @At("HEAD"), cancellable = true)
	private void adminvanish$silentClose(LivingEntity entity, Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
		// Also guards against going negative if someone opened while vanished and closed after unvanishing.
		if (VanishManager.isVanished(entity) || this.openCount <= 0) {
			ci.cancel();
		}
	}

	@Inject(method = "getEntitiesWithContainerOpen", at = @At("RETURN"), cancellable = true)
	private void adminvanish$ignoreVanished(Level level, BlockPos pos, CallbackInfoReturnable<List<ContainerUser>> cir) {
		List<ContainerUser> users = cir.getReturnValue();
		if (users.stream().anyMatch(user -> VanishManager.isVanished(user.getLivingEntity()))) {
			cir.setReturnValue(users.stream().filter(user -> !VanishManager.isVanished(user.getLivingEntity())).toList());
		}
	}
}
