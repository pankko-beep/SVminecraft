package br.com.nexus.commands;

import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class FlyCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Apenas jogadores."); return true; }
        if (!sender.hasPermission("server.fly")) { sender.sendMessage("§cVocê não tem permissão."); return true; }
        boolean enable = !p.getAllowFlight();
        p.setAllowFlight(enable);
        p.setFlying(enable);
        p.sendMessage(enable ? "§aVoo ativado." : "§cVoo desativado.");
        return true;
    }

    public static class Tab implements TabCompleter {
        @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { return Collections.emptyList(); }
    }
}
