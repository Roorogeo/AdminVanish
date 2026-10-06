package com.roorogeo.adminvanish;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * <pre>
 * /invsee &lt;player&gt;     view and edit a player's inventory, armor and offhand
 * /endersee &lt;player&gt;   view and edit a player's ender chest
 * </pre>
 * Both views are live: changes on either side show up immediately.
 */
public final class InvseeCommand {
	private InvseeCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("invsee")
				.requires(source -> VanishPermissions.check(source, VanishPermissions.INVSEE))
				.then(Commands.argument("player", EntityArgument.player()).executes(InvseeCommand::openInventory)));
		dispatcher.register(Commands.literal("endersee")
				.requires(source -> VanishPermissions.check(source, VanishPermissions.ENDERSEE))
				.then(Commands.argument("player", EntityArgument.player()).executes(InvseeCommand::openEnderChest)));
	}

	private static int openInventory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer viewer = ctx.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
		InventoryView view = new InventoryView(target);
		viewer.openMenu(new SimpleMenuProvider(
				(id, viewerInventory, player) -> new InventoryMenu(id, viewerInventory, view, target),
				Component.literal(target.getName().getString() + "'s inventory")));
		return 1;
	}

	private static int openEnderChest(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer viewer = ctx.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
		viewer.openMenu(new SimpleMenuProvider(
				(id, viewerInventory, player) -> new ChestMenu(MenuType.GENERIC_9x3, id, viewerInventory, target.getEnderChestInventory(), 3) {
					@Override
					public boolean stillValid(Player player) {
						return !target.isRemoved();
					}
				},
				Component.literal(target.getName().getString() + "'s ender chest")));
		return 1;
	}

	/**
	 * Five rows: main inventory, hotbar, then helmet, chestplate, leggings, boots and offhand.
	 * The remaining four slots are locked placeholders.
	 */
	private static final class InventoryView implements Container {
		private static final int SIZE = 45;
		private static final int EQUIPMENT_START = 36;
		private static final EquipmentSlot[] EQUIPMENT = {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND
		};
		private static final ItemStack FILLER = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);

		private final ServerPlayer target;

		InventoryView(ServerPlayer target) {
			this.target = target;
		}

		static boolean isFiller(int slot) {
			return slot >= EQUIPMENT_START + EQUIPMENT.length;
		}

		/** Rows 1-3 are inventory slots 9-35, row 4 is the hotbar (0-8). */
		private static int inventoryIndex(int slot) {
			return slot < 27 ? slot + 9 : slot - 27;
		}

		@Override
		public int getContainerSize() {
			return SIZE;
		}

		@Override
		public boolean isEmpty() {
			for (int i = 0; i < EQUIPMENT_START + EQUIPMENT.length; i++) {
				if (!getItem(i).isEmpty()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public ItemStack getItem(int slot) {
			if (slot < EQUIPMENT_START) {
				return this.target.getInventory().getItem(inventoryIndex(slot));
			}
			if (!isFiller(slot)) {
				return this.target.getItemBySlot(EQUIPMENT[slot - EQUIPMENT_START]);
			}
			return FILLER.copy();
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			if (slot < EQUIPMENT_START) {
				this.target.getInventory().setItem(inventoryIndex(slot), stack);
			} else if (!isFiller(slot)) {
				this.target.setItemSlot(EQUIPMENT[slot - EQUIPMENT_START], stack);
			}
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			if (isFiller(slot)) {
				return ItemStack.EMPTY;
			}
			ItemStack stack = getItem(slot);
			if (stack.isEmpty() || amount <= 0) {
				return ItemStack.EMPTY;
			}
			ItemStack taken = stack.split(amount);
			setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
			return taken;
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			if (isFiller(slot)) {
				return ItemStack.EMPTY;
			}
			ItemStack stack = getItem(slot);
			setItem(slot, ItemStack.EMPTY);
			return stack;
		}

		@Override
		public void setChanged() {
			this.target.getInventory().setChanged();
		}

		@Override
		public boolean stillValid(Player player) {
			return !this.target.isRemoved();
		}

		@Override
		public void clearContent() {
			// Never clear a real player's inventory through this view.
		}
	}

	private static final class InventoryMenu extends ChestMenu {
		private final ServerPlayer target;

		InventoryMenu(int id, Inventory viewerInventory, InventoryView view, ServerPlayer target) {
			super(MenuType.GENERIC_9x5, id, viewerInventory, view, 5);
			this.target = target;
			for (int i = 0; i < view.getContainerSize(); i++) {
				if (InventoryView.isFiller(i)) {
					Slot original = this.slots.get(i);
					Slot locked = new LockedSlot(view, i, original.x, original.y);
					locked.index = original.index;
					this.slots.set(i, locked);
				}
			}
		}

		@Override
		public boolean stillValid(Player player) {
			return !this.target.isRemoved();
		}
	}

	private static final class LockedSlot extends Slot {
		LockedSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}
}
