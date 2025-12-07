package br.com.nexus.commands;

import br.com.nexus.NexusPlugin;
import br.com.nexus.services.TransactionService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.*;

public class HistoricoCommand implements CommandExecutor {
    private final NexusPlugin plugin;

    public HistoricoCommand(NexusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Apenas jogadores."); return true; }
        if (!sender.hasPermission("nexus.historico")) { sender.sendMessage(msg("sem-permissao")); return true; }
        List<TransactionService.Entry> list = plugin.transactions().recent(p.getUniqueId());
        sender.sendMessage(prefix()+"§7Últimas transações: ");
        SimpleDateFormat df = new SimpleDateFormat("dd/MM HH:mm");
        for (TransactionService.Entry e : list) {
            String dir = e.from.equals(p.getUniqueId()) ? "-" : "+";
            String other = e.from.equals(p.getUniqueId()) ? e.to.toString() : e.from.toString();
            sender.sendMessage(String.format("§7[%s] §f%s §e%.2f §7(%s)", df.format(new Date(e.time)), dir, e.amount, e.note));
        }
        return true;
    }

    private String prefix() { return plugin.getConfig().getString("mensagens.prefixo", ""); }
    private String msg(String k) { return plugin.getConfig().getString("mensagens."+k, k); }

    public static class Tab implements TabCompleter {
        private final NexusPlugin plugin;
        public Tab(NexusPlugin plugin) { this.plugin = plugin; }
        @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { return Collections.emptyList(); }
    }
}
