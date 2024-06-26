package net.duart.virtualstorage;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final VirtualBackpack virtualBackpack;

    public CommandManager(VirtualBackpack virtualBackpack) {
        this.virtualBackpack = virtualBackpack;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("backpack")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be executed by a player.");
                return true;
            }

            for (int i = 1; i <= 999; i++) {
                if (player.hasPermission("virtualstorages.use." + i)) {
                    virtualBackpack.openBackpack(player);
                    return true;
                }
            }

            player.sendMessage(ChatColor.RED + "You are not allowed to use the backpack.");
            return true;
        } else if (command.getName().equalsIgnoreCase("vsreload")) {
            if (sender.hasPermission("virtualstorages.admin.reload")) {
                virtualBackpack.reloadVirtualStorages();
                sender.sendMessage(ChatColor.GREEN + "VirtualStorages permissions reloaded and backpacks updated.");
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("backpack");
            completions.add("vsreload");
        }
        return completions;
    }
}