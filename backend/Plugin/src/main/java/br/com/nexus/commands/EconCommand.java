package br.com.nexus.commands;

import br.com.nexus.NexusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class EconCommand implements CommandExecutor {
    private final NexusPlugin plugin;

    public EconCommand(NexusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economia.admin")) { sender.sendMessage(msg("sem-permissao")); return true; }
        if (args.length < 2) { sender.sendMessage(prefix()+"Uso: /econ <freeze|audit|rollback> <jogador|id> ..."); return true; }
        String sub = args[0].toLowerCase();
        if (sub.equals("freeze")) {
            OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[1]);
            boolean newState = true;
            if (args.length >=3) newState = Boolean.parseBoolean(args[2]);
                plugin.economy().setFrozen(alvo.getUniqueId(), newState);
                plugin.audit().log("economia.freeze", null, sender.getName(), "player", String.valueOf(alvo.getUniqueId()),
                    java.util.Map.of("state", newState));
            sender.sendMessage(prefix()+ (newState? msg("econ-freeze-on"): msg("econ-freeze-off")).replace("%jogador%", alvo.getName()==null?"?":alvo.getName()));
            return true;
        }
        if (sub.equals("audit")) {
            OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[1]);
            sender.sendMessage(prefix()+"Histórico de "+(alvo.getName()==null?"?":alvo.getName())+":");
            plugin.transactions().recent(alvo.getUniqueId()).forEach(e -> sender.sendMessage("- "+e.id+" "+ (e.from.equals(alvo.getUniqueId())?"-":"+") + e.amount + " ("+e.note+")"));
            return true;
        }
        if (sub.equals("rollback")) {
            // Esqueleto: para simplificação, não implementa rollback por ID aqui.
            sender.sendMessage(prefix()+"Rollback por ID não implementado nesta versão.");
            return true;
        }
        sender.sendMessage(prefix()+"Uso: /econ <freeze|audit|rollback> ...");
        return true;
    }

    private String prefix() { return plugin.getConfig().getString("mensagens.prefixo", ""); }
    private String msg(String k) { return plugin.getConfig().getString("mensagens."+k, k); }

    public static class Tab implements TabCompleter {
        private final NexusPlugin plugin;
        public Tab(NexusPlugin plugin) { this.plugin = plugin; }
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) return Arrays.asList("freeze", "audit", "rollback");
            if (args.length == 2 && (args[0].equalsIgnoreCase("freeze") || args[0].equalsIgnoreCase("audit"))) {
                return Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).sorted().collect(Collectors.toList());
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("freeze")) return Arrays.asList("true", "false");
            return Collections.emptyList();
        }
    }
}
