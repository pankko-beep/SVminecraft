package br.com.nexus.commands;

import br.com.nexus.NexusPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class SaldoCommand implements CommandExecutor {
    private final NexusPlugin plugin;

    public SaldoCommand(NexusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Apenas jogadores.");
            return true;
        }
        if (!sender.hasPermission("nexus.saldo")) {
            sender.sendMessage(msg("sem-permissao"));
            return true;
        }
        double bal = plugin.economy().getBalance(p);
        String moeda = plugin.getConfig().getString("moeda-nome", "moedas");
        sender.sendMessage(prefix() + msg("saldo").replace("%valor%", String.format("%.2f", bal)).replace("%moeda%", moeda));
        return true;
    }

    private String prefix() { return plugin.getConfig().getString("mensagens.prefixo", ""); }
    private String msg(String k) { return plugin.getConfig().getString("mensagens."+k, k); }

    public static class Tab implements TabCompleter {
        public Tab(NexusPlugin plugin) {}
        @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { return Collections.emptyList(); }
    }
}
