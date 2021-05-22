package io.github.divios.dailyrandomshop.hooks;

import io.github.divios.dailyrandomshop.DRShop;
import me.realized.tokenmanager.api.TokenManager;
import org.bukkit.Bukkit;

public class tokenManagerHook {

    private static final DRShop main = DRShop.getInstance();
    static TokenManager api = null;

    public static void hook() {
        main.getLogger().info("Hooked to TokenManager");
        api = (TokenManager) Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
    }

    public static TokenManager getApi() {
        return api;
    }

}
