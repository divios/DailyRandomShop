package io.github.divios.dailyrandomshop.guis.settings;

import io.github.divios.core_lib.XCore.XMaterial;
import io.github.divios.core_lib.inventory.dynamicGui;
import io.github.divios.core_lib.itemutils.ItemBuilder;
import io.github.divios.core_lib.misc.FormatUtils;
import io.github.divios.dailyrandomshop.DRShop;
import io.github.divios.dailyrandomshop.conf_msg;
import io.github.divios.dailyrandomshop.guis.confirmIH;
import io.github.divios.dailyrandomshop.guis.customizerguis.customizerMainGuiIH;
import io.github.divios.dailyrandomshop.utils.utils;
import io.github.divios.lib.itemHolder.dItem;
import io.github.divios.lib.itemHolder.dShop;
import io.github.divios.lib.managers.shopsManager;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class shopGui {

    private static final DRShop plugin = DRShop.getInstance();
    private static final shopsManager sManager = shopsManager.getInstance();

    public static void open(Player p, String shop) {
        sManager.getShop(shop).ifPresent(shop1 -> open(p, shop1));
    }

    public static void open(Player p, dShop shop) {
        new dynamicGui.Builder()
                .contents(() -> contents(shop))
                .addItems((inv, i) -> addItems(inv))
                .contentAction(e -> contentAction(e, shop))
                .nonContentAction((i, p1) -> nonContentAction(i, p1, shop))
                .setSearch(false)
                .back(p1 -> shopsManagerGui.open(p))
                .title(i -> FormatUtils.color("&f&lShop Manager"))
                .plugin(plugin)
                .open(p);
    }


    private static List<ItemStack> contents (dShop shop) {
        List<ItemStack> items = new ArrayList<>();
        shop.getItems().forEach(dItem -> items.add(dItem.getItem()));

        return items;
    }

    private static void addItems(Inventory inv) {
        ItemStack addItems = new ItemBuilder(XMaterial.ANVIL.parseItem())
                .setName("&f&lAdd new item");

        inv.setItem(52, addItems);
    }

    private static dynamicGui.Response contentAction(InventoryClickEvent e, dShop shop) {
        e.setCancelled(true);

        if (utils.isEmpty(e.getCurrentItem())) return dynamicGui.Response.nu();

        Player p = (Player) e.getWhoClicked();
        UUID uid = dItem.getUid(e.getCurrentItem());

        if (e.isLeftClick())
            customizerMainGuiIH.open((Player) e.getWhoClicked(),
                    shop.getItem(uid).get(), shop);

        else if (e.isRightClick())
            new confirmIH(p, (player, aBoolean) -> {
                if (aBoolean)
                    shop.removeItem(uid);
                open(p, shop.getName());
            }, e.getCurrentItem(),
                    conf_msg.CONFIRM_GUI_NAME, conf_msg.CONFIRM_MENU_YES, conf_msg.CONFIRM_MENU_NO);

        return dynamicGui.Response.nu();
    }

    private static dynamicGui.Response nonContentAction(int slot, Player p, dShop shop) {
        if (slot == 52) {
            addDailyItemGuiIH.open(p, itemStack -> {
                shop.addItem(new dItem(itemStack));
                open(p, shop.getName());
            });
        }
        return dynamicGui.Response.nu();
    }


}
