package io.github.divios.dailyShop.economies;

import com.vk2gpz.tokenenchant.api.TokenEnchantAPI;
import io.github.divios.dailyShop.hooks.hooksManager;
import org.bukkit.entity.Player;

public class tokenEnchantsE extends economy {

    public tokenEnchantsE() { this(""); }

    public tokenEnchantsE(String currency) { super(currency, "TokenEnchants"); }

    private transient final static TokenEnchantAPI api = hooksManager.getInstance().getTokenEnchantApi();

    @Override
    public void test() {
        api.getBalanceTop();
    }

    @Override
    public boolean hasMoney(Player p, Double price) {
        return api.getTokens(p) >= price ;
    }

    @Override
    public void witchDrawMoney(Player p, Double price) {
        api.removeTokens(p, price);
    }

    @Override
    public void depositMoney(Player p, Double price) {
        api.addTokens(p, price);
    }
}
