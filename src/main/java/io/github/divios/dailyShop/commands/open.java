package io.github.divios.dailyShop.commands;

import io.github.divios.core_lib.commands.abstractCommand;
import io.github.divios.core_lib.commands.cmdTypes;
import io.github.divios.core_lib.misc.FormatUtils;
import io.github.divios.core_lib.misc.Msg;
import io.github.divios.core_lib.utils.Log;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.lib.dLib.dShop;
import io.github.divios.lib.managers.shopsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class open extends abstractCommand {

    private static final DailyShop plugin = DailyShop.get();

    public open() {
        super(cmdTypes.BOTH);
    }

    @Override
    public String getName() {
        return "open";
    }

    @Override
    public boolean validArgs(List<String> args) {

        if (args.isEmpty()) return DailyShop.get().getShopsManager().getDefaultShop().isPresent();

        if (args.size() == 1) return DailyShop.get().getShopsManager().getShop(args.get(0)).isPresent();

        else {
            return DailyShop.get().getShopsManager().getShop(args.get(0)).isPresent() &&
                    Bukkit.getPlayer(args.get(1)) != null;
        }
    }

    @Override
    public String getHelp() {
        return FormatUtils.color("&8- &6/rdshop open [shop] [player]&8 " +
                "- &7Opens a gui for yourself or for the given player");
    }

    @Override
    public List<String> getPerms() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getTabCompletition(List<String> args) {
        if (args.size() == 1)
            return DailyShop.get().getShopsManager().getShops()
                    .stream()
                    .map(dShop::getName)
                    .collect(Collectors.toList());
        else if (args.size() == 2)
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
        return null;
    }

    @Override
    public void run(CommandSender sender, List<String> args) {

        Log.info("oke");
        if (args.size() == 2) {
            if (!sender.hasPermission("DailyRandomShop.open.others")) {
                sender.sendMessage("You dont have permission to open shops for others");
                return;
            }
        }

        if (!(sender instanceof Player) && args.size() == 1) {
            sender.sendMessage("DailyShop > You cannot open a shop to yourself from console");
            return;
        }

        Optional<dShop> shop = args.isEmpty() ?
                DailyShop.get().getShopsManager().getDefaultShop() :
                DailyShop.get().getShopsManager().getShop(args.get(0));

        if (!sender.hasPermission("dailyrandomshop.open." + shop.get().getName())) {
            Msg.sendMsg((Player) sender, plugin.configM.getLangYml().MSG_PERMS_OPEN_SHOP);
            return;
        }

        shop.ifPresent(shop1 -> shop1.openShop(args.size() == 2 ? Bukkit.getPlayer(args.get(1)): (Player) sender));

    }
}
