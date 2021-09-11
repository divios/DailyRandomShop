package io.github.divios.lib.dLib;

import com.google.common.collect.Sets;
import io.github.divios.core_lib.Events;
import io.github.divios.core_lib.event.SingleSubscription;
import io.github.divios.core_lib.event.Subscription;
import io.github.divios.core_lib.inventory.inventoryUtils;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.core_lib.misc.Pair;
import io.github.divios.core_lib.misc.WeightedRandom;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.events.updateItemEvent;
import io.github.divios.dailyShop.lorestategy.loreStrategy;
import io.github.divios.dailyShop.lorestategy.shopItemsLore;
import io.github.divios.dailyShop.transaction.sellTransaction;
import io.github.divios.dailyShop.transaction.transaction;
import io.github.divios.dailyShop.utils.utils;
import io.github.divios.lib.dLib.stock.dStock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class dInventory {

    protected static final DailyShop plugin = DailyShop.getInstance();

    protected String title;       // for some reason is throwing noSuchMethod
    protected Inventory inv;
    protected final dShop shop;

    protected final Set<Integer> openSlots = Sets.newConcurrentHashSet();
    protected final Map<Integer, dItem> buttons = new ConcurrentHashMap<>();

    private final Set<SingleSubscription> listeners = new HashSet<>();
    protected final loreStrategy strategy;

    public dInventory(String title, Inventory inv, dShop shop) {
        this.title = title;
        this.inv = inv;
        this.shop = shop;

        this.strategy = new shopItemsLore();

        IntStream.range(0, inv.getSize()).forEach(openSlots::add);  //initializes slots
        ready();
    }

    public dInventory(String title, int size, dShop shop) {
        this(title, Bukkit.createInventory(null, size, title), shop);
    }

    public dInventory(String base64, dShop shop) {
        this.shop = shop;
        this.strategy = new shopItemsLore();
        _deserialize(base64);

        ready();
    }

    public dInventory(dShop shop) {
        this(shop.getName(), 27, shop);
    }

    /**
     * Opens the inventory for a player
     *
     * @param p The player to open the inventory
     */
    public void open(Player p) {
        p.openInventory(inv);
    }

    /**
     * Returns the title of this gui
     *
     * @return The String representing the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the inventory title
     *
     * @param title
     */
    public void setTitle(String title) {
        this.title = title;
        Inventory temp = Bukkit.createInventory(null, inv.getSize(), title);
        inventoryUtils.translateContents(inv, temp);
        inv = temp;
    }

    /**
     * Returns the inventory
     *
     * @return The inventory this object holds
     */
    public Inventory getInventory() {
        return inv;
    }

    public int getSize() {
        return inv.getSize();
    }

    public boolean addRow() {
        if (inv.getSize() == 54) return false;

        Inventory aux = Bukkit.createInventory(null, inv.getSize() + 9, title);
        inventoryUtils.translateContents(inv, aux);

        IntStream.range(inv.getSize(), inv.getSize() + 9).forEach(openSlots::add);
        inv = aux;

        return true;
    }

    public boolean removeRow() {
        if (inv.getSize() == 9) return false;

        Inventory aux = Bukkit.createInventory(null, inv.getSize() - 9, title);
        inventoryUtils.translateContents(inv, aux);

        buttons.entrySet().removeIf(entry -> entry.getKey() >= aux.getSize());
        IntStream.range(inv.getSize() - 9, inv.getSize()).forEach(openSlots::remove);
        inv = aux;

        return true;
    }

    /**
     * Adds a button to the inventory
     *
     * @param item The item to add
     * @param slot The slot where the item 'll be added
     */
    public void addButton(dItem item, int slot) {
        dItem cloned = item.clone();
        cloned.generateNewBuyPrice();   // Generate new prices
        cloned.generateNewSellPrice();
        cloned.setSlot(slot);

        buttons.put(slot, cloned);

        openSlots.remove(slot);
        inv.setItem(slot, cloned.getItem());
    }

    /**
     * Removes a slot from the inventory
     *
     * @param slot The slot which wants to be clear
     * @return true if an item was successfully removed
     */
    public dItem removeButton(int slot) {
        dItem result = buttons.remove(slot);
        if (result != null) {
            openSlots.add(slot);
            inv.clear(slot);
        }
        return result;
    }

    /**
     * Gets the button in this object
     *
     * @return An unmodifiable view of the buttons set
     */
    public Map<Integer, dItem> getButtons() {
        return Collections.unmodifiableMap(buttons);
    }

    /**
     * Gets the openSlots in this object
     *
     * @return An unmodifiable view of the open Slots
     */
    public Set<Integer> getOpenSlots() {
        return Collections.unmodifiableSet(openSlots);
    }

    /**
     * Renovates daily items on the openSlots
     */
    public void renovate(Player p) {

        //shop.getItems().forEach(dItem -> Log.info(dItem.getDisplayName() + ": " + dItem.getRarity() + " - " + dItem.getRarity().getWeight()));
        //Log.info("---------------------------------------\n");

        buttons.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isAIR())
                .forEach(entry -> inv.clear(entry.getKey()));  // Clear AIR items

        WeightedRandom<dItem> RRM = WeightedRandom.fromCollection(      // create weighted random
                shop.getItems().stream()
                        .filter(dItem -> dItem.getRarity().getWeight() != 0)
                        .filter(dItem -> !(dItem.getBuyPrice().get().getPrice() < 0 &&
                                dItem.getSellPrice().get().getPrice() < 0))
                        .collect(Collectors.toList()),  // remove unAvailable
                dItem::clone,
                value -> value.getRarity().getWeight()     // Get weights depending if rarity enable
        );

        clearDailyItems();

        int addedButtons = 0;
        for (int i : openSlots.stream().sorted().collect(Collectors.toList())) {

            if (i >= inv.getSize()) continue;
            inv.clear(i);

            if (addedButtons >= shop.getItems().size()) break;

            dItem rolled = RRM.roll();
            if (rolled == null) break;

            rolled.generateNewBuyPrice();
            rolled.generateNewSellPrice();
            _renovate(p, rolled, i);
            RRM.remove(rolled);
            /*Log.info("---------------------------------------\n");
            Log.info("Rolled " + rolled.getDisplayName());
            RRM.getPercentages().forEach((dItem, aDouble) -> {
                Log.info(dItem.getDisplayName() + " Percentage: " + aDouble);
            }); */
            addedButtons++;
        }

    }

    protected void _renovate(Player p, dItem newItem, int slot) {
        newItem.setSlot(slot);
        buttons.put(slot, newItem);
        inv.setItem(slot, new shopItemsLore().applyLore(newItem.getItem().clone(), p));
    }

    public void updateItem(Player own, updateItemEvent o) {

        if (buttons.values().stream().noneMatch(dItem -> dItem.getUid().equals(o.getItem().getUid()))) return;

        buttons.values().stream()
                .filter(dItem -> dItem.getUid().equals(o.getItem().getUid()))
                .findFirst()
                .ifPresent(dItem -> {

                    int slot = dItem.getSlot();
                    if (o.getType().equals(updateItemEvent.updatetype.UPDATE_ITEM)) {

                        o.getItem().setSlot(slot);
                        buttons.put(slot, o.getItem().clone());
                        inv.setItem(slot, strategy.applyLore(o.getItem().getItem().clone(), own));

                    } else if (o.getType().equals(updateItemEvent.updatetype.NEXT_AMOUNT)) {

                        dStock stock = dItem.getStock();
                        stock.decrement(o.getPlayer(), o.getAmount());

                        if (stock.get(o.getPlayer()) <= 0) {
                            stock.set(o.getPlayer(), -1);
                        }

                        Bukkit.getPluginManager().callEvent(new updateItemEvent(dItem, o.getAmount(), updateItemEvent.updatetype.UPDATE_ITEM, o.getShop()));


                    } else if (o.getType().equals(updateItemEvent.updatetype.DELETE_ITEM)) {

                        dItem removed = removeButton(slot);
                        if (removed != null) inv.setItem(slot, utils.getRedPane());
                    }
                });
    }

    /**
     * Clears all the slots corresponding to daily Items
     */

    private void clearDailyItems() {

        for (Iterator<Map.Entry<Integer, dItem>> it = buttons.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, dItem> entry = it.next();

            if (openSlots.contains(entry.getKey())) {
                it.remove();
                inv.clear(entry.getKey());
            }

        }
    }


    protected void ready() {

        listeners.add(
                Events.subscribe(InventoryClickEvent.class, EventPriority.HIGHEST)
                        .filter(e -> e.getInventory().equals(inv))
                        .filter(e -> !ItemUtils.isEmpty(e.getCurrentItem()))
                        .handler(e -> {

                            e.setCancelled(true);

                            if (e.getSlot() != e.getRawSlot()) return;

                            if (openSlots.contains(e.getSlot())) {

                                if (e.isLeftClick())
                                    transaction.init((Player) e.getWhoClicked(), buttons.get(e.getSlot()), shop);
                                else
                                    sellTransaction.init((Player) e.getWhoClicked(), buttons.get(e.getSlot()), shop);

                            } else {
                                dItem item = buttons.get(e.getSlot());
                                if (item != null)
                                    item.getAction().stream((dAction, s) -> dAction.run((Player) e.getWhoClicked(), s));

                            }

                        })
        );

        listeners.add(
                Events.subscribe(InventoryDragEvent.class)
                        .filter(o -> o.getInventory().equals(inv))
                        .handler(e -> e.setCancelled(true))
        );

        if (!utils.isOperative("PlaceholderAPI")) return;       // If placeholderApi is not available, break

    }

    /**
     * Destroys the inventory. In summary, unregisters all the listeners
     */
    public void destroy() {
        listeners.forEach(Subscription::unregister);
        listeners.clear();
    }

    public String toBase64() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
                // Serialize inventory
                dataOutput.writeObject(inventoryUtils.serialize(inv, title));
                // Serialize openSlots
                dataOutput.writeObject(openSlots);
                // Serialize buttons
                dataOutput.writeObject(buttons);
                return Base64Coder.encodeLines(outputStream.toByteArray());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize inventory.", e);
        }
    }

    private void _deserialize(String base64) {
        try (ByteArrayInputStream InputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64))) {
            try (BukkitObjectInputStream dataInput = new BukkitObjectInputStream(InputStream)) {

                Pair<String, Inventory> s = inventoryUtils.deserialize((String) dataInput.readObject());
                title = s.get1();
                inv = s.get2();

                openSlots.addAll((Set<Integer>) dataInput.readObject());

                Object o = dataInput.readObject();
                if (o instanceof Set) ((Set<dItem>) o).forEach(dItem -> buttons.put(dItem.getSlot(), dItem));
                else buttons.putAll((Map<Integer, dItem>) o);
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Unable to deserialize gui of shop "
                    + shop.getName() + ", setting it to default");

            e.printStackTrace();
            buttons.clear();
            openSlots.clear();
            this.title = shop.getName();
            this.inv = Bukkit.createInventory(null, 27, title);
            IntStream.range(0, inv.getSize()).forEach(openSlots::add);


        }
    }

    public static dInventory fromBase64(String base64, dShop shop) {
        return new dInventory(base64, shop);
    }

    // Returns a clone of this gui without the daily items
    public dInventory skeleton() {
        dInventory cloned = fromBase64(this.toBase64(), shop);
        cloned.clearDailyItems();

        cloned.getButtons().entrySet().stream()   // gets the AIR buttons back
                .filter(entry -> entry.getValue().isAIR())
                .forEach(entry -> cloned.inv
                        .setItem(entry.getKey(), entry.getValue().getItem()));

        return cloned;
    }

    public dInventory clone() {
        return fromBase64(toBase64(), shop);
    }

}
