package io.github.divios.dailyrandomshop.guis.settings;

import io.github.divios.dailyrandomshop.builders.dynamicGui;
import io.github.divios.dailyrandomshop.builders.factory.dailyItem;
import io.github.divios.dailyrandomshop.conf_msg;
import io.github.divios.dailyrandomshop.database.dataManager;
import io.github.divios.dailyrandomshop.guis.customizerguis.changeBundleItem;
import io.github.divios.dailyrandomshop.guis.customizerguis.customizerMainGuiIH;
import io.github.divios.dailyrandomshop.listeners.dynamicItemListener;
import io.github.divios.dailyrandomshop.lorestategy.bundleSettingsLore;
import io.github.divios.dailyrandomshop.lorestategy.loreStrategy;
import io.github.divios.dailyrandomshop.utils.utils;
import io.github.divios.dailyrandomshop.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.stream.Collectors;

public class addDailyItemGuiIH implements InventoryHolder, Listener {

    private static final io.github.divios.dailyrandomshop.main main = io.github.divios.dailyrandomshop.main.getInstance();
    private static addDailyItemGuiIH instance = null;
    private static Inventory inv;

    private addDailyItemGuiIH() { }

    public static void openInventory(Player p) {
        if (instance == null) {
            instance = new addDailyItemGuiIH();
            Bukkit.getPluginManager().registerEvents(instance, main);
            instance.init();
        }
        p.openInventory(instance.getInventory());
    }
    
    private void init() {

        inv = Bukkit.createInventory(this, 27, conf_msg.ADD_ITEMS_TITLE);

        ItemStack fromZero = XMaterial.REDSTONE_TORCH.parseItem();
        utils.setDisplayName(fromZero, conf_msg.ADD_ITEMS_FROM_ZERO);
        utils.setLore(fromZero, conf_msg.ADD_ITEMS_FROM_ZERO_LORE);

        ItemStack fromItem = XMaterial.HOPPER.parseItem();
        utils.setDisplayName(fromItem, conf_msg.ADD_ITEMS_FROM_EXISTING);
        utils.setLore(fromItem, conf_msg.ADD_ITEMS_FROM_EXISTING_LORE);

        ItemStack bundleItem = XMaterial.CHEST_MINECART.parseItem();
        utils.setDisplayName(bundleItem, "&6&lCreate bundle");
        utils.setLore(bundleItem, Arrays.asList("&7Create bundle"));

        ItemStack returnItem = XMaterial.OAK_SIGN.parseItem();
        utils.setDisplayName(returnItem, conf_msg.ADD_ITEMS_RETURN);
        utils.setLore(returnItem, conf_msg.ADD_ITEMS_RETURN_LORE);


        inv.setItem(11, fromZero);
        inv.setItem(15, fromItem);
        inv.setItem(13, bundleItem);
        inv.setItem(22, returnItem);

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (utils.isEmpty(item)) {
                inv.setItem(i, XMaterial.GRAY_STAINED_GLASS_PANE.parseItem());
            }
        }
    }

    public static void reload() {
        if(instance == null) return;
        try { inv.getViewers().forEach(HumanEntity::closeInventory); }
        catch (ConcurrentModificationException ignored) {};
        instance.init();
    }

    private static void unRegisterAll() {
        InventoryClickEvent.getHandlerList().unregister(instance);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    @EventHandler
    private void onClick(InventoryClickEvent e) {

        if (e.getView().getTopInventory().getHolder() != this) return;

        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();

        if (e.getSlot() != e.getRawSlot()) return;

        if (e.getSlot() == 22) {    //return
            dailyGuiSettings.openInventory(p);
        }

        if (e.getSlot() == 11) { //from zero
            customizerMainGuiIH.openInventory(p, XMaterial.GRASS.parseItem());
        }

        else if (e.getSlot() == 15) {  //from item
            new dynamicItemListener(p, (player, itemStack) ->
                    customizerMainGuiIH.openInventory(p, itemStack));
            p.closeInventory();
        }

        else if (e.getSlot() == 13) {  //bundle item

            new changeBundleItem(
                    p,
                    XMaterial.CHEST_MINECART.parseItem(),
                    (player, itemStack) ->
                            customizerMainGuiIH.openInventory(p,
                            new dailyItem(itemStack).craft()),
                    player -> openInventory(p));
        }
    }

}