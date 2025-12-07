package br.com.nexus.commands;

import br.com.nexus.NexusPlugin;
import br.com.nexus.services.GuildService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class GuildCommand implements CommandExecutor {
    private final NexusPlugin plugin;

    public GuildCommand(NexusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Apenas jogadores."); return true; }
        if (!sender.hasPermission("nexus.guild")) { sender.sendMessage(msg("sem-permissao")); return true; }
        if (args.length == 0) {
            sender.sendMessage(prefix()+"Uso: /guild <criar|convidar|aceitar|sair> ...");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("criar")) {
            if (args.length < 2) { sender.sendMessage(prefix()+"Uso: /guild criar <nome>"); return true; }
            if (plugin.guilds().hasGuild(p.getUniqueId())) { sender.sendMessage(prefix()+msg("guild-ja-possui")); return true; }
            String nome = args[1];
            if (plugin.guilds().exists(nome)) { sender.sendMessage(prefix()+msg("guild-ja-existe")); return true; }
            plugin.guilds().create(nome, p.getUniqueId());
            plugin.guilds().saveNow();
            sender.sendMessage(prefix()+msg("guild-criada").replace("%nome%", nome));
            plugin.audit().log("guild.create", p.getUniqueId(), p.getName(), "guild", nome, java.util.Map.of());
            return true;
        }
        if (sub.equals("convidar")) {
            GuildService.Guild g = plugin.guilds().getByPlayer(p.getUniqueId());
            if (g == null) { sender.sendMessage(prefix()+msg("guild-sem-guilda")); return true; }
            if (!g.leader.equals(p.getUniqueId())) { sender.sendMessage(prefix()+"§cApenas o líder pode convidar."); return true; }
            if (args.length < 2) { sender.sendMessage(prefix()+"Uso: /guild convidar <jogador>"); return true; }
            OfflinePlayer alvo = Bukkit.getOfflinePlayer(args[1]);
            plugin.guilds().invite(alvo.getUniqueId(), g.name);
            sender.sendMessage(prefix()+msg("guild-convite-enviado").replace("%jogador%", alvo.getName()==null?"?":alvo.getName()));
            if (alvo.isOnline()) Objects.requireNonNull(alvo.getPlayer()).sendMessage(prefix()+msg("guild-convite-recebido").replace("%guild%", g.name));
            plugin.audit().log("guild.invite", p.getUniqueId(), p.getName(), "player", String.valueOf(alvo.getUniqueId()), java.util.Map.of("guild", g.name));
            return true;
        }
        if (sub.equals("aceitar")) {
            String pend = plugin.guilds().pendingInvite(p.getUniqueId());
            if (pend == null) { sender.sendMessage(prefix()+"§cVocê não possui convite pendente."); return true; }
            boolean ok = plugin.guilds().accept(p.getUniqueId());
            if (ok) { sender.sendMessage(prefix()+msg("guild-entrada").replace("%jogador%", p.getName())); plugin.guilds().saveNow(); plugin.audit().log("guild.accept", p.getUniqueId(), p.getName(), "guild", pend, java.util.Map.of()); }
            else sender.sendMessage(prefix()+"§cConvite inválido.");
            return true;
        }
        if (sub.equals("sair")) {
            boolean ok = plugin.guilds().leave(p.getUniqueId());
            if (ok) { sender.sendMessage(prefix()+msg("guild-saida").replace("%jogador%", p.getName())); plugin.guilds().saveNow(); plugin.audit().log("guild.leave", p.getUniqueId(), p.getName(), "player", p.getUniqueId().toString(), java.util.Map.of()); }
            else sender.sendMessage(prefix()+msg("guild-sem-guilda"));
            return true;
        }
        sender.sendMessage(prefix()+"Uso: /guild <criar|convidar|aceitar|sair> ...");
        return true;
    }

    private String prefix() { return plugin.getConfig().getString("mensagens.prefixo", ""); }
    private String msg(String k) { return plugin.getConfig().getString("mensagens."+k, k); }

    public static class Tab implements TabCompleter {
        private final NexusPlugin plugin;
        public Tab(NexusPlugin plugin) { this.plugin = plugin; }
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) return Arrays.asList("criar", "convidar", "aceitar", "sair");
            if (args.length == 2 && args[0].equalsIgnoreCase("convidar")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }
}
