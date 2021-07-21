package io.github.divios.dailyShop.hooks;

import io.github.divios.core_lib.misc.timeStampUtils;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.utils.utils;
import io.github.divios.lib.dLib.dShop;
import io.github.divios.lib.managers.shopsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.sql.Timestamp;

class placeholderApiHook extends PlaceholderExpansion {

    private static final DailyShop plugin = DailyShop.getInstance();
    private static placeholderApiHook instance = null;

    private placeholderApiHook() {
    }

    public static placeholderApiHook getInstance() {
        if (instance == null) {
            instance = new placeholderApiHook();
            instance.register();
            plugin.getLogger().info("Hooked to PlaceholderAPI");
        }
        return instance;
    }

    /**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister() {
        return true;
    }

    /**
     * The name of the person who created this expansion should go here.
     * <br>For convienience do we return the author from the plugin.yml
     *
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * <br>This is what tells PlaceholderAPI to call our onRequest
     * method to obtain a value if a placeholder starts with our
     * identifier.
     * <br>The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier() {
        return "DailyShop";
    }

    /**
     * This is the version of the expansion.
     * <br>You don't have to use numbers, since it is set as a String.
     * <p>
     * For convienience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /**
     * This is the method called when a placeholder with our identifier
     * is found and needs a value.
     * <br>We specify the value identifier in this method.
     * <br>Since version 2.9.1 can you use OfflinePlayers in your requests.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {

        dShop shop = null;

        for (dShop _shop : shopsManager.getInstance().getShops()) {

            if (identifier.replace("time_", "")
                    .equals(_shop.getName())) {
                shop = _shop;
                break;
            }
        }

        if (shop == null) return null;

        return utils.getDiffActualTimer(shop);

    }

}
