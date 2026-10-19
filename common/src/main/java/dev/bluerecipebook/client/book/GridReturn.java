package dev.bluerecipebook.client.book;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Moves items from the crafting grid back into the player inventory.
 * Moves nothing when the cursor is holding an item or the inventory cannot take every one of them.
 */
public final class GridReturn {
    private GridReturn() {
    }

    /**
     * Moves one item from each filled crafting slot into the inventory.
     *
     * @return false when the move was refused or a slot left an item on the cursor, true otherwise, including for an empty grid
     */
    public static boolean takeOne(AbstractContainerMenu menu, Player player) {
        List<Slot> grid = filledGrid(menu, player);
        if (grid == null || !canTake(menu, player, grid, false)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) {
            return false;
        }
        for (Slot slot : grid) {
            returnOne(minecraft, menu, player, slot);
            if (!menu.getCarried().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Moves the whole stack from each filled crafting slot into the inventory, by the shift-click the server also uses to clear the grid.
     *
     * @return false when the move was refused or a slot still holds an item after its click, true otherwise, including for an empty grid
     */
    public static boolean takeAll(AbstractContainerMenu menu, Player player) {
        List<Slot> grid = filledGrid(menu, player);
        if (grid == null || !canTake(menu, player, grid, true)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) {
            return false;
        }
        for (Slot slot : grid) {
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, ClickType.QUICK_MOVE, player);
            if (slot.hasItem() || !menu.getCarried().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the filled crafting slots, or null when the menu is not a recipe-book menu, the cursor holds an item,
     * or a filled slot cannot be picked up. An empty grid is an empty list.
     */
    private static List<Slot> filledGrid(AbstractContainerMenu menu, Player player) {
        if (!(menu instanceof RecipeBookMenu<?, ?> book) || !menu.getCarried().isEmpty()) {
            return null;
        }
        int limit = book.getSize();
        int result = book.getResultSlotIndex();
        List<Slot> grid = new ArrayList<>();
        for (int index = 0; index < limit; index++) {
            if (index == result) {
                continue;
            }
            Slot slot = menu.getSlot(index);
            if (!slot.hasItem()) {
                continue;
            }
            if (!slot.mayPickup(player)) {
                return null;
            }
            grid.add(slot);
        }
        return grid;
    }

    /**
     * Moves one item from {@code source} into the inventory.
     * A shift-click would move the whole stack, so a stack larger than one is picked up, one item is placed, and the rest goes back.
     */
    private static void returnOne(Minecraft minecraft, AbstractContainerMenu menu, Player player, Slot source) {
        int container = menu.containerId;
        if (source.getItem().getCount() == 1) {
            minecraft.gameMode.handleInventoryMouseClick(container, source.index, 0, ClickType.QUICK_MOVE, player);
            return;
        }
        int destination = storageFor(menu, player, source.getItem());
        if (destination < 0) {
            return;
        }
        minecraft.gameMode.handleInventoryMouseClick(container, source.index, 0, ClickType.PICKUP, player);
        minecraft.gameMode.handleInventoryMouseClick(container, destination, 1, ClickType.PICKUP, player);
        if (!menu.getCarried().isEmpty()) {
            minecraft.gameMode.handleInventoryMouseClick(container, source.index, 0, ClickType.PICKUP, player);
        }
    }

    private static boolean canTake(AbstractContainerMenu menu, Player player, List<Slot> grid, boolean wholeStack) {
        int count = menu.slots.size();
        ItemStack[] stacks = new ItemStack[count];
        int[] room = new int[count];
        List<Integer> empty = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            Slot slot = menu.getSlot(index);
            if (!isStorage(slot, player)) {
                continue;
            }
            ItemStack there = slot.getItem();
            if (there.isEmpty()) {
                empty.add(index);
                continue;
            }
            stacks[index] = there.copy();
            room[index] = slot.getMaxStackSize(there) - there.getCount();
        }
        for (Slot slot : grid) {
            int moving = wholeStack ? slot.getItem().getCount() : 1;
            if (!reserve(menu, stacks, room, empty, slot.getItem(), moving)) {
                return false;
            }
        }
        return true;
    }

    private static boolean reserve(AbstractContainerMenu menu, ItemStack[] stacks, int[] room, List<Integer> empty, ItemStack stack, int count) {
        int left = count;
        for (int index = 0; index < stacks.length && left > 0; index++) {
            ItemStack there = stacks[index];
            if (there == null || room[index] <= 0 || !ItemStack.isSameItemSameComponents(there, stack)) {
                continue;
            }
            int use = Math.min(room[index], left);
            room[index] -= use;
            left -= use;
        }
        while (left > 0) {
            int found = -1;
            for (int index = 0; index < empty.size(); index++) {
                if (menu.getSlot(empty.get(index)).mayPlace(stack)) {
                    found = index;
                    break;
                }
            }
            if (found < 0) {
                return false;
            }
            int slot = empty.remove(found);
            int space = menu.getSlot(slot).getMaxStackSize(stack);
            int use = Math.min(space, left);
            stacks[slot] = stack.copyWithCount(use);
            room[slot] = space - use;
            left -= use;
        }
        return true;
    }

    private static int storageFor(AbstractContainerMenu menu, Player player, ItemStack stack) {
        int empty = -1;
        for (Slot slot : menu.slots) {
            if (!isStorage(slot, player)) {
                continue;
            }
            ItemStack there = slot.getItem();
            if (there.isEmpty()) {
                if (empty < 0 && slot.mayPlace(stack)) {
                    empty = slot.index;
                }
                continue;
            }
            if (ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < slot.getMaxStackSize(there)) {
                return slot.index;
            }
        }
        return empty;
    }

    private static boolean isStorage(Slot slot, Player player) {
        return slot.container == player.getInventory() && slot.getContainerSlot() < Inventory.INVENTORY_SIZE;
    }
}
