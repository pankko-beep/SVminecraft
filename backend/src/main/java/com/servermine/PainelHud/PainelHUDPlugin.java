package com.servermine;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

public class PainelHUDPlugin {

    private final JavaPlugin plugin;

    public PainelHUDPlugin(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        plugin.getLogger().info("PainelHUDPlugin ativado.");
    }

    public void disable() {
        plugin.getLogger().info("PainelHUDPlugin desativado.");
    }

    public void criarHUD(Player jogador, String servidor, String time, int moedas, boolean vip, int proxObjetivoMinutos) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard board = manager.getNewScoreboard();
        Objective obj = board.registerNewObjective("HUD", "dummy", ChatColor.AQUA + "=== " + servidor + " ===");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        Score s1 = obj.getScore(ChatColor.YELLOW + "Time: " + ChatColor.WHITE + time);
        s1.setScore(6);

        Score s2 = obj.getScore(ChatColor.YELLOW + "Moedas: " + ChatColor.WHITE + moedas);
        s2.setScore(5);

        Score s3 = obj.getScore(ChatColor.YELLOW + "VIP: " + ChatColor.WHITE + (vip ? "SIM" : "NÃO"));
        s3.setScore(4);

        Score s4 = obj.getScore(ChatColor.YELLOW + "Próx Objetivo: " + ChatColor.WHITE + proxObjetivoMinutos + " min");
        s4.setScore(3);

        Score s5 = obj.getScore(ChatColor.DARK_GRAY + "Jogadores Online: " + Bukkit.getOnlinePlayers().size());
        s5.setScore(2);

        Score s6 = obj.getScore(ChatColor.GRAY + "IP: seu.ip.aqui");
        s6.setScore(1);

        jogador.setScoreboard(board);
    }
}