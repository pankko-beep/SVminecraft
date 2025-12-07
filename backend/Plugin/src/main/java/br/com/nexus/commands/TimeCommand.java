package br.com.nexus.commands;

import br.com.nexus.NexusPlugin;
import br.com.nexus.services.TeamService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class TimeCommand implements CommandExecutor {
    private final NexusPlugin plugin;

    public TimeCommand(NexusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Apenas jogadores."); return true; }
        if (!sender.hasPermission("nexus.time")) { sender.sendMessage(msg("sem-permissao")); return true; }
        if (args.length == 0) { sender.sendMessage(prefix()+msg("time-escolher-uso")); return true; }
        String sub = args[0].toLowerCase();
        if (sub.equals("escolher")) {
            if (args.length < 2) { sender.sendMessage(prefix()+msg("time-escolher-uso")); return true; }
            if (plugin.teams().hasTeam(p.getUniqueId())) { sender.sendMessage(prefix()+msg("time-ja-definido")); return true; }
            TeamService.Team team = parseTeam(args[1]);
            if (team == null) { sender.sendMessage(prefix()+msg("time-escolher-uso")); return true; }
            plugin.teams().set(p.getUniqueId(), team);
            applyNameColor(p, team);
            String color = colorCode(team);
            sender.sendMessage(prefix()+msg("time-definido").replace("%time%", team.name()).replace("%time_color%", color));
            plugin.teams().saveNow();
            plugin.audit().log("time.choose", p.getUniqueId(), p.getName(), "team", team.name(), java.util.Map.of());
            return true;
        }
        if (sub.equals("trocar")) {
            if (args.length == 1) {
                int custo = plugin.getConfig().getInt("limites.time.custo-troca", 1000000);
                sender.sendMessage(prefix()+msg("time-troca-custo").replace("%custo%", String.valueOf(custo)));
                return true;
            }
            if (!args[1].equalsIgnoreCase("confirmar")) { sender.sendMessage(prefix()+"Use: /time trocar confirmar"); return true; }
            int custo = plugin.getConfig().getInt("limites.time.custo-troca", 1000000);
            if (plugin.economy().withdraw(p, custo)) {
                TeamService.Team cur = plugin.teams().get(p.getUniqueId());
                TeamService.Team novo = (cur == TeamService.Team.SOLAR) ? TeamService.Team.LUNAR : TeamService.Team.SOLAR;
                plugin.teams().set(p.getUniqueId(), novo);
                applyNameColor(p, novo);
                plugin.teams().saveNow();
                String color = colorCode(novo);
                sender.sendMessage(prefix()+msg("time-trocado").replace("%time%", novo.name()).replace("%time_color%", color));
                plugin.audit().log("time.swap", p.getUniqueId(), p.getName(), "team", novo.name(), java.util.Map.of("cost", custo));
            } else {
                sender.sendMessage(prefix()+msg("time-sem-saldo"));
            }
            return true;
        }
        sender.sendMessage(prefix()+msg("time-escolher-uso"));
        return true;
    }

    private TeamService.Team parseTeam(String s) {
        if (s.equalsIgnoreCase("solar")) return TeamService.Team.SOLAR;
        if (s.equalsIgnoreCase("lunar")) return TeamService.Team.LUNAR;
        return null;
    }

    private String prefix() { return plugin.getConfig().getString("mensagens.prefixo", ""); }
    private String msg(String k) { return plugin.getConfig().getString("mensagens."+k, k); }

    private String colorCode(TeamService.Team t) {
        if (t == TeamService.Team.SOLAR) return plugin.getConfig().getString("mensagens.time-cor-solar", "§e");
        return plugin.getConfig().getString("mensagens.time-cor-lunar", "§5");
    }

    private void applyNameColor(Player p, TeamService.Team t) {
        String base = p.getName();
        String color = colorCode(t);
        String colored = color + base + "§r";
        try { p.setDisplayName(colored); } catch (Throwable ignored) {}
        try { p.setPlayerListName(colored); } catch (Throwable ignored) {}
    }

    public static class Tab implements TabCompleter {
        private final NexusPlugin plugin;
        public Tab(NexusPlugin plugin) { this.plugin = plugin; }
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) return Arrays.asList("escolher", "trocar");
            if (args.length == 2 && args[0].equalsIgnoreCase("escolher")) return Arrays.asList("Solar", "Lunar");
            if (args.length == 2 && args[0].equalsIgnoreCase("trocar")) return Collections.singletonList("confirmar");
            return Collections.emptyList();
        }
    }
}
