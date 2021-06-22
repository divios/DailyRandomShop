package io.github.divios.dailyShop.economies;

import io.github.divios.dailyShop.DRShop;
import io.github.divios.dailyShop.hooks.hooksManager;
import me.realized.tokenmanager.api.TokenManager;
import org.bukkit.entity.Player;

public class tokenManagerE extends economy {

    public tokenManagerE() { this(""); }

    public tokenManagerE(String currency) { super(currency, "TokenManager"); }

    private transient static final TokenManager api = hooksManager.getInstance().getTokenManagerApi();

    @Override
    public void test() {
        api.getShop("aa");
    }

    @Override
    public boolean hasMoney(Player p, Double price) {
        return api.getTokens(p).getAsLong() >= price ;
    }

    @Override
    public void witchDrawMoney(Player p, Double price) {
        DRShop.getInstance().getLogger().info("" + price.longValue());
        api.removeTokens(p, price.longValue());
    }

    @Override
    public void depositMoney(Player p, Double price) {
        api.addTokens(p, price.longValue());
    }
}
