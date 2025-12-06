package com.servermine.RankNivel;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RanksNiveisPlugin {
    private final JavaPlugin plugin;
    private final com.servermine.storage.DatabaseManager database;
    private final com.servermine.util.Messages messages;
    private Map<UUID, Integer> xpJogadores = new HashMap<>();
    private Map<UUID, Integer> nivelJogadores = new HashMap<>();

    public RanksNiveisPlugin(JavaPlugin plugin, com.servermine.storage.DatabaseManager database, com.servermine.util.Messages messages) {
        this.plugin = plugin;
        this.database = database;
        this.messages = messages;
    }

    public void enable() {
        plugin.saveDefaultConfig();
        plugin.getLogger().info("RanksNiveisPlugin ativado.");
        // carregar do DB
        try {
            Map<UUID, com.servermine.storage.DatabaseManager.Rank> all = database.loadAllRanks();
            for (Map.Entry<UUID, com.servermine.storage.DatabaseManager.Rank> e : all.entrySet()) {
                xpJogadores.put(e.getKey(), e.getValue().xp);
                nivelJogadores.put(e.getKey(), e.getValue().level);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Erro carregando ranks do DB: " + ex.getMessage());
        }
    }

    public void disable() {
        // salva todos os ranks em background (MainPlugin aguardará na desativação)
        saveState().exceptionally(ex -> { plugin.getLogger().warning("Erro salvando ranks no shutdown: " + ex.getMessage()); return null; });
        plugin.getLogger().info("RanksNiveisPlugin desativado.");
    }

    /**
     * Persiste todos os ranks para o banco de dados de forma assíncrona.
     */
    public java.util.concurrent.CompletableFuture<Void> saveState() {
        java.util.List<java.util.concurrent.CompletableFuture<Void>> futures = new java.util.ArrayList<>();
        for (Map.Entry<UUID, Integer> e : xpJogadores.entrySet()) {
            UUID id = e.getKey();
            int xp = e.getValue();
            int level = nivelJogadores.getOrDefault(id, 1);
            futures.add(database.upsertRankAsync(id, xp, level).exceptionally(ex -> { plugin.getLogger().warning("Erro ao salvar rank: " + ex.getMessage()); return null; }));
        }
        return java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]));
    }

    public void adicionarXP(Player jogador, int quantidade) {
        UUID id = jogador.getUniqueId();
        int xpAtual = xpJogadores.getOrDefault(id, 0);
        xpAtual += quantidade;
        xpJogadores.put(id, xpAtual);

        int nivel = nivelJogadores.getOrDefault(id, 1);
        int xpNecessario = calcularXPParaProximoNivel(nivel);
            if (xpAtual >= xpNecessario) {
                nivelJogadores.put(id, nivel + 1);
                xpJogadores.put(id, xpAtual - xpNecessario);
                jogador.sendMessage(messages.get("ranks.leveled_up", nivel + 1));
            }
            // persistir XP a cada alteração (assíncrono)
            database.upsertRankAsync(id, xpJogadores.get(id), nivelJogadores.getOrDefault(id, 1))
                    .exceptionally(ex -> { plugin.getLogger().warning("Erro salvando rank: " + ex.getMessage()); return null; });
    }

    private int calcularXPParaProximoNivel(int nivel) {
        return 100 + (nivel - 1) * 50; // XP cresce linearmente, pode ajustar para exponencial
    }

    public int getNivel(Player jogador) {
        return nivelJogadores.getOrDefault(jogador.getUniqueId(), 1);
    }

    public int getXP(Player jogador) {
        return xpJogadores.getOrDefault(jogador.getUniqueId(), 0);
    }
}