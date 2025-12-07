package br.com.nexus.commands;

import br.com.nexus.NexusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PagarCommand implements CommandExecutor {
    private final NexusPlugin plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PagarCommand(NexusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Apenas jogadores."); return true; }
        if (!sender.hasPermission("nexus.pagar")) { sender.sendMessage(msg("sem-permissao")); return true; }
        if (args.length != 2) { sender.sendMessage(prefix()+"Uso: /pagar <jogador> <quantia>"); return true; }
        String alvoNome = args[0];
        OfflinePlayer alvo = null;
        Player online = Bukkit.getPlayerExact(alvoNome);
        if (online != null) {
            alvo = online;
        } else {
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() != null && op.getName().equalsIgnoreCase(alvoNome)) { alvo = op; break; }
            }
            if (alvo == null) alvo = Bukkit.getOfflinePlayer(alvoNome); // fallback
        }
        if (alvo == null || ( !alvo.hasPlayedBefore() && !alvo.isOnline())) { sender.sendMessage(prefix()+msg("jogador-nao-encontrado")); return true; }
        double quantia;
        try { quantia = Double.parseDouble(args[1].replace(",", ".")); } catch (Exception e) { sender.sendMessage(prefix()+msg("valor-invalido")); return true; }
        int limite = plugin.getConfig().getInt("limites.pagar.max-por-transacao", 500000);
        if (quantia <= 0 || quantia > limite) { sender.sendMessage(prefix()+msg("pagar-limite").replace("%limite%", String.valueOf(limite))); return true; }
        int cd = plugin.getConfig().getInt("limites.pagar.cooldown-segundos", 10);
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(p.getUniqueId(), 0L);
        long remain = cd*1000L - (now-last);
        if (remain > 0) { sender.sendMessage(prefix()+msg("pagar-cooldown").replace("%seg%", String.valueOf((int)Math.ceil(remain/1000.0)))); return true; }

        if (plugin.economy().withdraw(p, quantia)) {
            plugin.economy().deposit(alvo, quantia);
            cooldowns.put(p.getUniqueId(), now);
            plugin.transactions().add(p.getUniqueId(), alvo.getUniqueId(), quantia, "pagar");
            plugin.audit().log("economia.transfer", p.getUniqueId(), p.getName(), "player", String.valueOf(alvo.getUniqueId()),
                    Map.of("amount", quantia, "to", String.valueOf(alvo.getUniqueId())));
            String moeda = plugin.getConfig().getString("moeda-nome", "moedas");
            p.sendMessage(prefix()+msg("pagar-sucesso").replace("%valor%", String.format("%.2f", quantia)).replace("%alvo%", alvo.getName()==null?"?":alvo.getName()));
            if (alvo.isOnline()) Objects.requireNonNull(alvo.getPlayer()).sendMessage(prefix()+msg("pagar-recebido").replace("%valor%", String.format("%.2f", quantia)).replace("%origem%", p.getName()));
        } else {
            p.sendMessage(prefix()+"§cFalha ao debitar. Saldo insuficiente?");
        }
        return true;
    }

    private String prefix() { return plugin.getConfig().getString("mensagens.prefixo", ""); }
    private String msg(String k) { return plugin.getConfig().getString("mensagens."+k, k); }

    public static class Tab implements TabCompleter {
        private final NexusPlugin plugin;
        public Tab(NexusPlugin plugin) { this.plugin = plugin; }
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).sorted().collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }
}
