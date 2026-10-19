package dev.bluerecipebook.client.strict;

import dev.bluerecipebook.BlueRecipeBook;
import dev.bluerecipebook.client.discover.DiscoveryFiles;
import dev.bluerecipebook.client.discover.MissingIds;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Items this client has had in the inventory for the current world and player, including armor, the offhand, and the cursor.
 * Vanilla statistics split pickup, crafting, and use, and none of those is this set.
 * Recorded whether or not strict discovery is on.
 */
final class HeldItems {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueRecipeBook.MOD_ID);
    private static final Set<Item> held = new LinkedHashSet<>();
    private static int loadedPlace = -1;
    private static boolean rewrite;
    private static boolean readFailed;
    private static boolean changed;

    private HeldItems() {
    }

    /**
     * Returns whether the open world has a place for this player's files.
     * Loads the saved set and adds what the player holds now.
     */
    static boolean ready() {
        if (!DiscoveryFiles.ready()) {
            return false;
        }
        boolean loaded = syncPlace();
        boolean grew = absorb();
        if ((loaded && rewrite) || grew) {
            save();
            rewrite = false;
        }
        if (loaded || grew) {
            changed = true;
        }
        return true;
    }

    static boolean changed() {
        return changed;
    }

    static boolean consumeChanged() {
        boolean value = changed;
        changed = false;
        return value;
    }

    static boolean has(Item item) {
        return held.contains(item);
    }

    /** Forgets every held item for this world and player. Items in the inventory right now count again on the next read. */
    static void clear() {
        if (!DiscoveryFiles.ready()) {
            return;
        }
        syncPlace();
        held.clear();
        changed = true;
        save();
    }

    private static boolean syncPlace() {
        int place = DiscoveryFiles.generation();
        if (place == loadedPlace) {
            return false;
        }
        loadedPlace = place;
        held.clear();
        rewrite = load();
        return true;
    }

    private static boolean absorb() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        Inventory inventory = minecraft.player.getInventory();
        boolean grew = false;
        grew |= addAll(inventory.items);
        grew |= addAll(inventory.armor);
        grew |= addAll(inventory.offhand);
        if (minecraft.player.containerMenu != null) {
            grew |= add(minecraft.player.containerMenu.getCarried());
        }
        return grew;
    }

    private static boolean addAll(List<ItemStack> stacks) {
        boolean grew = false;
        for (ItemStack stack : stacks) {
            grew |= add(stack);
        }
        return grew;
    }

    private static boolean add(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return held.add(stack.getItem());
    }

    /**
     * Loads the saved item ids into the held set and logs the ones the loaded game cannot use.
     *
     * @return true when the file still contains lines that were not kept
     */
    private static boolean load() {
        readFailed = false;
        List<ResourceLocation> missing = new ArrayList<>();
        int unreadable = 0;
        Path path = DiscoveryFiles.file("held-items.txt");
        try {
            for (String line : DiscoveryFiles.read(path)) {
                try {
                    ResourceLocation id = ResourceLocation.parse(line);
                    Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
                    if (item.isEmpty()) {
                        missing.add(id);
                    } else {
                        held.add(item.get());
                    }
                } catch (RuntimeException exception) {
                    unreadable++;
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Could not read held items from {}; leaving the file unchanged", path, exception);
            held.clear();
            readFailed = true;
            return false;
        }
        MissingIds.reportHeldItems(missing, unreadable);
        return !missing.isEmpty() || unreadable > 0;
    }

    private static void save() {
        // A thrown read left this file unread, so a later save must not replace it.
        if (readFailed) {
            return;
        }
        List<String> lines = new ArrayList<>();
        for (Item item : held) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) {
                lines.add(id.toString());
            }
        }
        DiscoveryFiles.write(DiscoveryFiles.file("held-items.txt"), lines);
    }
}
