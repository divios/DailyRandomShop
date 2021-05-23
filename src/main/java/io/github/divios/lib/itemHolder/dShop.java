package io.github.divios.lib.itemHolder;

import io.github.divios.lib.storage.dataManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class dShop {

    private final String name;
    private final dShopT type;
    private Set<dItem> items = new LinkedHashSet<>();

    public dShop(String name, dShopT type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Gets the name of the shop
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the type of the shop
     * @return type of the shop (buy,sell)
     */
    public dShopT getType() {
        return type;
    }

    /**
     * Gets a copy the items in the shop
     * @return returns a List of dItems. Note that this list is a copy of the original,
     * any change made to it won't affect the original one
     */
    public Set<dItem> getItems() {
        return Collections.unmodifiableSet(items);
    }

    /**
     * Gets the item by uuid
     * @param uid the UUID to search
     * @return null if it does not exist
     */
    public dItem getItem(UUID uid) {
        for (dItem item : items)
            if (item.getUid().equals(uid))
                return item;
        return null;
    }

    /**
     * Checks if the shop has a particular item
     * @param uid the UUID to check
     * @return true if exits, false if not
     */
    public boolean hasItem(UUID uid) {
        return getItem(uid) != null;
    }

    public void updateItem(UUID uid, dItem newItem) {

    }

    /**
     * Sets the items of this shop
     */
    public void setItems(@NotNull HashSet<dItem> items) {
        this.items = items;
    }

    /**
     * Adds an item to this shop
     * @param item item to be added
     */
    public void addItem(@NotNull dItem item) {
        items.add(item);
        dataManager.getInstance().addItem(this.name, item);
    }

    /**
     * Removes an item from the shop
     * @param uid UUID of the item to be removed
     * @return true if the item was removed. False if not
     */
    public boolean removeItem(UUID uid) {
        boolean result = items.removeIf(dItem -> dItem.getUid().equals(uid));
        if (result)
            dataManager.getInstance().deleteItem(this.name, uid);
        return result;
    }


    public enum dShopT {
        buy,
        sell
    }

}
