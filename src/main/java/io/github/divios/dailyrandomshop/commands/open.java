package io.github.divios.dailyrandomshop.commands;

import io.github.divios.core_lib.commands.abstractCommand;
import io.github.divios.core_lib.commands.cmdTypes;
import io.github.divios.lib.itemHolder.dShop;
import io.github.divios.lib.managers.shopsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class open extends abstractCommand {

    public open() {
        super(cmdTypes.BOTH);
    }

    @Override
    public String getName() {
        return "open";
    }

    @Override
    public boolean validArgs(List<String> args) {

        if (args.size() > 0) {
            return shopsManager.getInstance().getShop(args.get(0)).isPresent();
        }
        return false;
    }

    @Override
    public String getHelp() {
        return "DailyRandomShop.open.others";
    }

    @Override
    public List<String> getPerms() {
        return Collections.singletonList("DailyRandomShop.open");
    }

    @Override
    public List<String> getTabCompletition(List<String> args) {
        if (args.size() == 1)
            return shopsManager.getInstance().getShops()
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

        if (args.size() == 2) {
            if (!sender.hasPermission("DailyRandomShop.open.others")) {
                sender.sendMessage("You dont have permission to open shops for others");
                return;
            }
            else if (Bukkit.getPlayer(args.get(1)) == null)
                return;
        }

        shopsManager.getInstance().getShop(args.get(0))
                .get().getGui()
                .open(args.size() == 2 ? Bukkit.getPlayer(args.get(1)): (Player) sender);

    }
}
