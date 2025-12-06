package com.servermine.TimeGuilda;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class TimesGuildasPlugin implements CommandExecutor {

    private final JavaPlugin plugin;
    private final com.servermine.util.Messages messages;
    private Map<UUID, String> jogadorTime = new HashMap<>();
    private Map<String, List<UUID>> times = new HashMap<>();
    private Map<String, Guilda> guildas = new HashMap<>();

    public TimesGuildasPlugin(JavaPlugin plugin, com.servermine.util.Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void enable() {
        plugin.saveDefaultConfig();
        plugin.getLogger().info("TimesGuildasPlugin ativado.");
        times.put("COMETA", new ArrayList<>());
        times.put("ECLIPSE", new ArrayList<>());
    }

    public void disable() {
        plugin.getLogger().info("TimesGuildasPlugin desativado.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("times.only_players"));
            return true;
        }
        Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("time")) {
            if (args.length != 1) {
                p.sendMessage(messages.get("times.usage_time"));
                return true;
            }
            String escolha = args[0].toUpperCase();
            if (!times.containsKey(escolha)) {
                p.sendMessage(messages.get("times.invalid_time"));
                return true;
            }

            // Checar desequilíbrio
            String outro = escolha.equals("COMETA") ? "ECLIPSE" : "COMETA";
            if (times.get(escolha).size() - times.get(outro).size() > 10) {
                p.sendMessage(messages.get("times.team_full"));
                return true;
            }

            String antigo = jogadorTime.get(p.getUniqueId());
            if (antigo != null) times.get(antigo).remove(p.getUniqueId());

            jogadorTime.put(p.getUniqueId(), escolha);
            times.get(escolha).add(p.getUniqueId());
            p.sendMessage(messages.get("times.joined_team", escolha));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("guilda")) {
            if (args.length < 1) {
                p.sendMessage(messages.get("times.usage_guild"));
                return true;
            }

            if (args[0].equalsIgnoreCase("criar")) {
                if (!jogadorTime.containsKey(p.getUniqueId())) {
                    p.sendMessage(messages.get("times.choose_team_before_guild"));
                    return true;
                }
                String nome = args[1];
                if (guildas.containsKey(nome)) {
                    p.sendMessage(messages.get("times.guild_exists"));
                    return true;
                }

                // Limite de guildas por time
                long qtdGuildasTime = guildas.values().stream()
                        .filter(g -> g.getTime().equals(jogadorTime.get(p.getUniqueId())))
                        .count();
                if (qtdGuildasTime >= 6) {
                    p.sendMessage(messages.get("times.team_guild_limit"));
                    return true;
                }

                Guilda nova = new Guilda(nome, jogadorTime.get(p.getUniqueId()), p.getUniqueId());
                guildas.put(nome, nova);
                p.sendMessage(messages.get("times.guild_created", nome));
                return true;
            }

            if (args[0].equalsIgnoreCase("entrar")) {
                String nome = args[1];
                Guilda g = guildas.get(nome);
                if (g == null) {
                    p.sendMessage(messages.get("times.guild_not_found"));
                    return true;
                }
                if (!g.getTime().equals(jogadorTime.get(p.getUniqueId()))) {
                    p.sendMessage(messages.get("times.cannot_join_other_time"));
                    return true;
                }
                g.adicionarMembro(p.getUniqueId());
                p.sendMessage(messages.get("times.joined_guild", nome));
                return true;
            }
        }

        return false;
    }

    public static class Guilda {
        private String nome;
        private String time;
        private UUID lider;
        private Set<UUID> membros = new HashSet<>();

        public Guilda(String nome, String time, UUID lider) {
            this.nome = nome;
            this.time = time;
            this.lider = lider;
            this.membros.add(lider);
        }

        public String getNome() { return nome; }
        public String getTime() { return time; }
        public UUID getLider() { return lider; }
        public Set<UUID> getMembros() { return membros; }

        public void adicionarMembro(UUID jogador) { membros.add(jogador); }
        public void removerMembro(UUID jogador) { membros.remove(jogador); }
    }
}