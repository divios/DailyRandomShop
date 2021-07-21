package io.github.divios.dailyShop.utils;

import com.cryptomorin.xseries.XMaterial;
import io.github.divios.core_lib.itemutils.ItemBuilder;
import io.github.divios.core_lib.misc.Msg;
import io.github.divios.core_lib.misc.timeStampUtils;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.lib.dLib.dShop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class utils {

    private static final DailyShop plugin = DailyShop.getInstance();

    public static void translateAllItemData(ItemStack recipient, ItemStack receiver) {
        try {
            receiver.setData(recipient.getData());
            receiver.setType(recipient.getType());
            receiver.setItemMeta(recipient.getItemMeta());
            receiver.setAmount(recipient.getAmount());
            receiver.setDurability(recipient.getDurability());
        } catch (IllegalArgumentException ignored) {}
    }

    public static void translateAllItemData(ItemStack recipient,
                                            ItemStack receiver, boolean dailyMetadata) {
        try {
            receiver.setData(recipient.getData());
            receiver.setType(recipient.getType());
            receiver.setItemMeta(recipient.getItemMeta());
            receiver.setAmount(recipient.getAmount());
            receiver.setDurability(recipient.getDurability());
            //if(dailyMetadata) dailyItem.transferDailyMetadata(recipient, receiver);
        } catch (IllegalArgumentException ignored) {}
    }

    public static ItemMeta getItemMeta(ItemStack item) {
        ItemMeta meta;
        if(item.hasItemMeta()) meta = item.getItemMeta();
        else meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        return meta;
    }

    public static List<String> getItemLore(ItemMeta meta) {
        List<String> lore = new ArrayList<>();
        if (meta.hasLore()) lore = meta.getLore();
        return lore;
    }

    public static void removeLore(ItemStack item, int n) {
        ItemMeta meta = getItemMeta(item);
        List<String> lore = getItemLore(meta);
        if (isEmpty(lore)) return;
        if (n == -1) lore.clear();
        else
            for (int i = 0; i < n; i++) {
                lore.remove(lore.size() - 1);
            }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType() == Material.AIR;
    }

    public static boolean isEmpty(String s) { return s == null || s.isEmpty(); }

    public static boolean isEmpty(List<String> s) { return s == null || s.isEmpty(); }


    public static void sendSound(Player p, Sound s) {
        try {
            p.playSound(p.getLocation(), s, 0.5F, 1);
        } catch (NoSuchFieldError Ignored) {
        }
    }

    public static int randomValue(int minValue, int maxValue) {

        return minValue + (int) (Math.random() * ((maxValue - minValue) + 1));
    }

    public static ItemStack getEntry(Map<ItemStack, Double> list, int index) {
        int i = 0;
        for (ItemStack item : list.keySet()) {
            if (index == i) return item;
            i++;
        }
        return null;
    }

    @Deprecated
    public static ItemStack getRedPane() {
        return new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE)
                .setName("&cOut of stock"); //TODO
    }

    public static void removeFlag(ItemStack i, ItemFlag f) {
        ItemMeta meta = i.getItemMeta();
        meta.removeItemFlags(f);
        i.setItemMeta(meta);
    }

    public static boolean isPotion(ItemStack item) {
        return item.getType().equals(XMaterial.POTION.parseMaterial()) ||
                item.getType().equals(XMaterial.SPLASH_POTION.parseMaterial());
    }


    /**
     *
     * @param inv
     * @return amount of free slots on inventory (excluding armor). If inventory is full returns 0
     */

    public static int inventoryFull (Inventory inv) {

        int freeSlots = 0;
        for (int i = 0; i < 36; i++) {

            if (utils.isEmpty(inv.getItem(i))) {
                freeSlots++;
            }
        }
        return freeSlots;
    }

    public static void noPerms(CommandSender p) {
        Msg.sendMsg((Player) p, plugin.configM.getLangYml().MSG_NOT_PERMS);
    }

    public static void noCmd(CommandSender p) {
        Msg.sendMsg((Player) p, "&7Console is no allow to do this command");
    }


    public static Double getPriceModifier(Player p) {
        AtomicReference<Double> modifier = new AtomicReference<>(1.0);

        p.getEffectivePermissions().forEach(perms -> {
            String perm = perms.getPermission();
            if (perm.startsWith("DailyShop.sellpricemodifier.")) {
                String[] splitStr = perm.split("DailyShop.sellpricemodifier.");
                if(splitStr.length == 1) return;
                double newValue;
                try{
                    newValue = Math.abs(Double.parseDouble(splitStr[1]));
                } catch (NumberFormatException e) { return; }
                if (newValue > modifier.get())
                    modifier.set(newValue);
            }
        });
        return modifier.get();
    }

    public static String getDisplayName(ItemStack item) {
        String name;

        if (item.getItemMeta().hasDisplayName()) name =
                item.getItemMeta().getDisplayName();

        else name = item.getType().toString();

        return name;
    }

    public static boolean isOperative(String pl) {
        return Bukkit.getPluginManager().getPlugin(pl) != null &&
                Bukkit.getPluginManager().getPlugin(pl).isEnabled();
    }

    public static double round(double d, int decimals) {
        return Math.round(d * Math.pow(10, decimals)) / Math.pow(10, decimals);
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean isShort(String s) {
        try {
            Short.parseShort(s);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static void sendMsg(Player p, String s) {
        Msg.sendMsg(p, s);
    }

    public static String getDiffActualTimer(dShop shop) {

        int timeInSeconds = (int) (shop.getTimer() - timeStampUtils.diff(shop.getTimestamp(),
                new Timestamp(System.currentTimeMillis())));

        int secondsLeft = timeInSeconds % 3600 % 60;
        int minutes = (int) Math.floor(timeInSeconds % 3600 / 60F);
        int hours = (int) Math.floor(timeInSeconds / 3600F);

        String HH = ((hours < 10) ? "0" : "") + hours;
        String MM = ((minutes < 10) ? "0" : "") + minutes;
        String SS = ((secondsLeft < 10) ? "0" : "") + secondsLeft;

        return HH + ":" + MM + ":" + SS;
    }

}
