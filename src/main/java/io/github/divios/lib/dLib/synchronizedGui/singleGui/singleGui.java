package io.github.divios.lib.dLib.synchronizedGui.singleGui;

import io.github.divios.dailyShop.events.updateItemEvent;
import io.github.divios.lib.dLib.dInventory;
import io.github.divios.lib.dLib.dItem;
import io.github.divios.lib.dLib.dShop;
import org.bukkit.entity.Player;

/**
 * Contract of a singleGui
 */

public interface singleGui {

    static singleGui fromJson(String json, dShop shop) {
        return new singleGuiImpl(null, shop, dInventory.fromJson(json, shop));
    }

    static singleGui create(Player p, singleGui base, dShop shop) {
        return new singleGuiImpl(p, shop, base);
    }

    static singleGui create(Player p, dInventory inv, dShop shop) { return new singleGuiImpl(p, shop, inv); }

    void updateItem(dItem item, updateItemEvent.updatetype type);

    void renovate();

    Player getPlayer();

    dInventory getBase();

    dInventory getInventory();

    dShop getShop();

    void destroy();

    default String toJson() {
        return getInventory().toJson();
    }

    int hashCode();

}
