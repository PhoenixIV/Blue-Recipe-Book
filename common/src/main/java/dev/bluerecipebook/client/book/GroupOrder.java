package dev.bluerecipebook.client.book;

import dev.bluerecipebook.client.discover.DiscoveryOrder;
import dev.bluerecipebook.client.strict.StrictDiscovery;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The order of one page slot, taken from the current pin and craftable set.
 * A place updates the pin before the next frame, including a locked recipe that does not rebuild
 * the page, so this is not stored.
 */
public final class GroupOrder {
    private final List<List<RecipeHolder<?>>> cells;
    private final List<RecipeHolder<?>> members;
    private final List<RecipeHolder<?>> reps;
    private final Set<ResourceLocation> craftable;
    private final boolean grouped;
    private final float iconTime;

    private GroupOrder(List<RecipeHolder<?>> recipes, Set<ResourceLocation> craftable, boolean grouped, float iconTime) {
        this.craftable = craftable;
        this.grouped = grouped;
        this.iconTime = iconTime;
        this.cells = this.arrange(recipes);
        this.members = new ArrayList<>();
        for (List<RecipeHolder<?>> cell : this.cells) {
            this.members.addAll(cell);
        }
        this.reps = new ArrayList<>();
        for (List<RecipeHolder<?>> cell : this.cells) {
            this.reps.add(cellRecipe(cell, craftable, grouped));
        }
    }

    public static GroupOrder of(RecipeGroup group, Set<ResourceLocation> craftable, boolean grouped, float iconTime) {
        return new GroupOrder(group.recipes(), craftable, grouped, iconTime);
    }

    /**
     * Returns the recipes in lightbox order: a pinned item, then craftable items, then newest first.
     * The page's order button does not change this.
     */
    public List<RecipeHolder<?>> members() {
        return this.members;
    }

    /** Returns one cell per item, in {@link #members()} order. A cell holds every recipe that makes that item. */
    public List<List<RecipeHolder<?>>> cells() {
        return this.cells;
    }

    /** Returns one recipe per item. The page icon walks these, so one item never cycles through its own recipes. */
    public List<RecipeHolder<?>> reps() {
        return this.reps;
    }

    /** Returns the icon drawn on top. A pinned type stays here. Otherwise this is the current step of {@code unpinnedCycle}. */
    public RecipeHolder<?> front() {
        List<RecipeHolder<?>> shown = this.reps;
        if (shown.size() > 1) {
            RecipeHolder<?> pinned = this.choice(shown);
            if (pinned != null) {
                return pinned;
            }
            List<RecipeHolder<?>> cycle = this.unpinnedCycle(shown);
            return cycle.get(this.iconIndex(cycle.size()));
        }
        return shown.get(this.iconIndex(shown.size()));
    }

    /**
     * Returns the icon behind the front one, or null when the group has fewer than two items.
     * The front type is skipped, and this icon walks the rest of the group, ghosts included.
     * With nothing craftable, this is the next step of that full walk.
     */
    public RecipeHolder<?> rear() {
        List<RecipeHolder<?>> shown = this.reps;
        if (shown.size() < 2) {
            return null;
        }
        RecipeHolder<?> front = this.front();
        boolean locked = this.choice(shown) != null;
        if (!locked && this.craftableOf(shown).size() < 2) {
            List<RecipeHolder<?>> cycle = this.unpinnedCycle(shown);
            return cycle.get((this.iconIndex(cycle.size()) + 1) % cycle.size());
        }
        int others = shown.size() - 1;
        int step = this.iconIndex(others);
        int seen = 0;
        for (RecipeHolder<?> holder : shown) {
            if (holder.id().equals(front.id())) {
                continue;
            }
            if (seen == step) {
                return holder;
            }
            seen++;
        }
        return null;
    }

    /**
     * Returns the recipe a click on this item places: the one placed last when it is still craftable, otherwise the first.
     * The cell must already be sorted craftable first, so the first is craftable whenever any recipe of the item is.
     */
    public static RecipeHolder<?> cellRecipe(List<RecipeHolder<?>> cell, Set<ResourceLocation> craftable, boolean grouped) {
        if (cell.size() == 1) {
            return cell.getFirst();
        }
        ResourceLocation remembered = LastGroupUse.get(RecipeGroup.key(cell.getFirst(), grouped));
        for (RecipeHolder<?> holder : cell) {
            if (holder.id().equals(remembered) && craftable.contains(holder.id())) {
                return holder;
            }
        }
        return cell.getFirst();
    }

    /**
     * Returns the unlock rank of {@code id}, or its strict-discovery rank when that is on.
     * A higher rank is newer, and -1 means the id is not in the list.
     */
    static int rank(ResourceLocation id) {
        if (StrictDiscovery.enabled()) {
            return StrictDiscovery.rank(id);
        }
        return DiscoveryOrder.rank(id);
    }

    private List<List<RecipeHolder<?>>> arrange(List<RecipeHolder<?>> recipes) {
        List<List<RecipeHolder<?>>> cells = cellsOf(recipes);
        Comparator<RecipeHolder<?>> recipeOrder = Comparator
                .comparingInt((RecipeHolder<?> holder) -> this.craftable.contains(holder.id()) ? 0 : 1)
                .thenComparing(Comparator.comparingInt((RecipeHolder<?> holder) -> rank(holder.id())).reversed());
        for (List<RecipeHolder<?>> cell : cells) {
            cell.sort(recipeOrder);
        }
        List<RecipeHolder<?>> reps = new ArrayList<>();
        for (List<RecipeHolder<?>> cell : cells) {
            reps.add(cellRecipe(cell, this.craftable, this.grouped));
        }
        RecipeHolder<?> pinned = this.choice(reps);
        cells.sort(Comparator
                .comparingInt((List<RecipeHolder<?>> cell) -> cellHas(cell, pinned) ? 0 : 1)
                .thenComparingInt((List<RecipeHolder<?>> cell) -> this.cellCraftable(cell) ? 0 : 1)
                .thenComparing(Comparator.comparingInt(this::cellRank).reversed()));
        if (pinned != null) {
            for (List<RecipeHolder<?>> cell : cells) {
                if (cellHas(cell, pinned)) {
                    cell.sort(Comparator
                            .comparingInt((RecipeHolder<?> holder) -> holder.id().equals(pinned.id()) ? 0 : 1)
                            .thenComparing(recipeOrder));
                    break;
                }
            }
        }
        return cells;
    }

    /**
     * Groups recipes by their result, keeping their order.
     * Results of one item with different components get separate cells.
     */
    private static List<List<RecipeHolder<?>>> cellsOf(List<RecipeHolder<?>> recipes) {
        List<List<RecipeHolder<?>>> cells = new ArrayList<>();
        List<ItemStack> results = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes) {
            ItemStack result = RecipeGroup.result(holder);
            int found = -1;
            for (int index = 0; index < results.size(); index++) {
                if (ItemStack.isSameItemSameComponents(results.get(index), result)) {
                    found = index;
                    break;
                }
            }
            if (found < 0) {
                results.add(result);
                cells.add(new ArrayList<>());
                found = cells.size() - 1;
            }
            cells.get(found).add(holder);
        }
        return cells;
    }

    /**
     * Returns the type kept in front, or null when the front icon cycles.
     * One craftable member is that member. Several keep the member placed earlier this session when it is still craftable.
     */
    private RecipeHolder<?> choice(List<RecipeHolder<?>> shown) {
        List<RecipeHolder<?>> available = this.craftableOf(shown);
        if (available.size() == 1) {
            return available.getFirst();
        }
        if (available.size() < 2) {
            return null;
        }
        ResourceLocation remembered = LastGroupUse.get(RecipeGroup.key(shown.getFirst(), this.grouped));
        if (remembered == null) {
            return null;
        }
        for (RecipeHolder<?> holder : available) {
            if (holder.id().equals(remembered)) {
                return holder;
            }
        }
        return null;
    }

    /** Returns the walk when nothing is pinned. Two or more craftable members: only those. Otherwise the whole group. */
    private List<RecipeHolder<?>> unpinnedCycle(List<RecipeHolder<?>> shown) {
        List<RecipeHolder<?>> available = this.craftableOf(shown);
        if (available.size() >= 2) {
            return available;
        }
        return shown;
    }

    private List<RecipeHolder<?>> craftableOf(List<RecipeHolder<?>> shown) {
        List<RecipeHolder<?>> available = new ArrayList<>();
        for (RecipeHolder<?> holder : shown) {
            if (this.craftable.contains(holder.id())) {
                available.add(holder);
            }
        }
        return available;
    }

    private static boolean cellHas(List<RecipeHolder<?>> cell, RecipeHolder<?> holder) {
        if (holder == null) {
            return false;
        }
        for (RecipeHolder<?> member : cell) {
            if (member.id().equals(holder.id())) {
                return true;
            }
        }
        return false;
    }

    private boolean cellCraftable(List<RecipeHolder<?>> cell) {
        for (RecipeHolder<?> holder : cell) {
            if (this.craftable.contains(holder.id())) {
                return true;
            }
        }
        return false;
    }

    private int cellRank(List<RecipeHolder<?>> cell) {
        int rank = -1;
        for (RecipeHolder<?> holder : cell) {
            rank = Math.max(rank, rank(holder.id()));
        }
        return rank;
    }

    /** Returns the current step of a walk over {@code size} icons, one step every 30 render ticks. */
    private int iconIndex(int size) {
        if (size <= 1) {
            return 0;
        }
        int index = Mth.floor(this.iconTime / 30.0f) % size;
        return index < 0 ? index + size : index;
    }
}
