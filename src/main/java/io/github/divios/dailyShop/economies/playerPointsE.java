package io.github.divios.dailyShop.economies;


import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;

public class playerPointsE extends economy {

    public playerPointsE() {this(""); }

    public playerPointsE(String currency) { super(currency, "PlayerPoints"); }

    private transient static final PlayerPointsAPI api = PlayerPoints.getInstance().getAPI();

    @Override
    public void test() {
        api.getClass().getName();
    }

    @Override
    public boolean hasMoney(Player p, Double price) {
        return api.look(p.getUniqueId()) >= (int) Math.round(price);
    }

    @Override
    public void witchDrawMoney(Player p, Double price) {
        api.takeAsync(p.getUniqueId(), (int) Math.round(price));
    }

    @Override
    public void depositMoney(Player p, Double price) {
        api.giveAsync(p.getUniqueId(), (int) Math.round(price));
    }
}
