package dev.bluerecipebook.client.category;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;

/** One side tab of the book. A crafting recipe is on it when its vanilla crafting category is {@link #craftingCategory()}. */
public final class BookCategory {
    private final String id;
    private final Component name;
    private final List<ItemStack> icons;
    private final CraftingBookCategory craftingCategory;

    public BookCategory(String id, Component name, List<ItemStack> icons, CraftingBookCategory craftingCategory) {
        this.id = id;
        this.name = name;
        this.icons = List.copyOf(icons);
        this.craftingCategory = craftingCategory;
    }

    public String id() {
        return this.id;
    }

    public Component name() {
        return this.name;
    }

    public List<ItemStack> icons() {
        return this.icons;
    }

    public CraftingBookCategory craftingCategory() {
        return this.craftingCategory;
    }
}
