package br.com.nexus.commands;

import br.com.nexus.NexusPlugin;
import br.com.nexus.panels.PanelService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class PainelCommand implements CommandExecutor {
    private final NexusPlugin plugin;

    public PainelCommand(NexusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("paineis.admin")) { sender.sendMessage(msg("sem-permissao")); return true; }
        if (args.length == 0) { sender.sendMessage(prefix()+"Uso: /painel <criar|deletar|listar|tp|info|refresh|realign> ..."); return true; }
        String sub = args[0].toLowerCase();
        if (sub.equals("criar")) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Apenas jogadores."); return true; }
            if (args.length < 2) { sender.sendMessage(prefix()+"Uso: /painel criar <global|time|guilda>"); return true; }
            PanelService.Type t = parse(args[1]); if (t==null) { sender.sendMessage(prefix()+"Tipo inválido."); return true; }
            Location loc = p.getLocation();
            var panel = plugin.panels().create(t, loc);
            plugin.panels().saveNow();
            sender.sendMessage(prefix()+msg("painel-criado").replace("%id%", panel.id));
            plugin.audit().log("panel.create", (sender instanceof org.bukkit.entity.Player pl)? pl.getUniqueId(): null, sender.getName(), "panel", panel.id, java.util.Map.of("type", t.name(), "x", loc.getX(), "y", loc.getY(), "z", loc.getZ()));
            return true;
        }
        if (sub.equals("criar-guilda")) {
            if (!(sender instanceof Player p)) { sender.sendMessage("Apenas jogadores."); return true; }
            if (args.length < 2) { sender.sendMessage(prefix()+"Uso: /painel criar-guilda <nome>"); return true; }
            String gname = args[1];
            Location loc = p.getLocation();
            var panel = plugin.panels().createGuildPanel(gname, loc);
            plugin.panels().saveNow();
            sender.sendMessage(prefix()+msg("painel-criado").replace("%id%", panel.id));
            plugin.audit().log("panel.create", p.getUniqueId(), p.getName(), "panel", panel.id, java.util.Map.of("type", "GUILDA", "guild", gname, "x", loc.getX(), "y", loc.getY(), "z", loc.getZ()));
            return true;
        }
        if (sub.equals("deletar")) {
            if (args.length<2) { sender.sendMessage(prefix()+"Uso: /painel deletar <id>"); return true; }
            boolean ok = plugin.panels().delete(args[1]);
            if (ok) { plugin.panels().saveNow(); sender.sendMessage(prefix()+msg("painel-deletado").replace("%id%", args[1]));}
            else sender.sendMessage(prefix()+"§cPainel não encontrado.");
            if (ok) plugin.audit().log("panel.delete", (sender instanceof org.bukkit.entity.Player pl)? pl.getUniqueId(): null, sender.getName(), "panel", args[1], java.util.Map.of());
            return true;
        }
        if (sub.equals("listar")) {
            sender.sendMessage(prefix()+"Painéis:");
            plugin.panels().all().forEach((id, p) -> {
                String extra = (p.type == PanelService.Type.GUILDA && p.guildName != null) ? (" guilda="+p.guildName) : "";
                sender.sendMessage("- "+id+" ("+p.type+")"+extra+" @ "+Math.round(p.loc.getX())+","+Math.round(p.loc.getY())+","+Math.round(p.loc.getZ()));
            });
            return true;
        }
        if (sub.equals("info")) {
            if (args.length < 2) { sender.sendMessage(prefix()+"Uso: /painel info <id>"); return true; }
            String id = args[1];
            var p = plugin.panels().all().get(id);
            if (p == null) { sender.sendMessage(prefix()+"§cPainel não encontrado: "+id); return true; }
            Location loc = p.loc;
            sender.sendMessage(prefix()+"Informações do painel:");
            sender.sendMessage(prefix()+"ID: "+id);
            sender.sendMessage(prefix()+"Tipo: "+p.type);
            if (p.type == PanelService.Type.GUILDA && p.guildName != null) sender.sendMessage(prefix()+"Guilda: "+p.guildName);
            sender.sendMessage(prefix()+"Mundo: "+loc.getWorld().getName());
            sender.sendMessage(prefix()+String.format("Posição: x=%.2f y=%.2f z=%.2f", loc.getX(), loc.getY(), loc.getZ()));
            sender.sendMessage(prefix()+String.format("Orientação: yaw=%.1f pitch=%.1f", loc.getYaw(), loc.getPitch()));
            // Entidades vinculadas (hologramas/TextDisplay) são gerenciadas internamente.
            return true;
        }
        if (sub.equals("refresh")) {
            plugin.panels().refreshAll();
            sender.sendMessage(prefix()+"§aPainéis atualizados.");
            return true;
        }
        // Simples: tp/info/realign podem ser implementados depois
        sender.sendMessage(prefix()+"Subcomando não implementado nesta versão.");
        return true;
    }

    private PanelService.Type parse(String s) {
        return switch (s.toLowerCase()) {
            case "global" -> PanelService.Type.GLOBAL;
            case "time" -> PanelService.Type.TIME;
            case "guilda" -> PanelService.Type.GUILDA;
            default -> null;
        };
    }

    private String prefix() { return plugin.getConfig().getString("mensagens.prefixo", ""); }
    private String msg(String k) { return plugin.getConfig().getString("mensagens."+k, k); }

    public static class Tab implements TabCompleter {
        private final NexusPlugin plugin;
        public Tab(NexusPlugin plugin) { this.plugin = plugin; }
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) return Arrays.asList("criar", "criar-guilda", "deletar", "listar", "tp", "info", "refresh", "realign");
            if (args.length == 2 && args[0].equalsIgnoreCase("criar")) return Arrays.asList("global", "time", "guilda");
            if (args.length == 2 && args[0].equalsIgnoreCase("criar-guilda")) return java.util.Collections.singletonList("<nome>");
            if (args.length == 2 && args[0].equalsIgnoreCase("deletar")) return new ArrayList<>(plugin.panels().all().keySet());
            return Collections.emptyList();
        }
    }
}
