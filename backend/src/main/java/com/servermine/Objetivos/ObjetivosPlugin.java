package com.servermine.Objetivos;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.servermine.storage.DatabaseManager;
import com.servermine.util.Messages;

import java.util.*;

/**
 * ObjetivosPlugin — versão corrigida e funcional mínima.
 * Melhorias aplicadas:
 * - pacote e imports corretos
 * - scheduler com ticks corretos (20 ticks = 1 segundo)
 * - persistência simples de progresso no config.yml
 * - comando /objetivos para listar objetivos ativos e ver progresso
 */
public class ObjetivosPlugin implements CommandExecutor {

    private final JavaPlugin plugin;

    public enum TipoObjetivo { RARO, EPICO, LENDARIO }

    private final Map<UUID, Map<TipoObjetivo, Integer>> progressoJogadores = new HashMap<>();
    private final List<Objetivo> objetivosAtivos = new ArrayList<>();
    private final Random random = new Random();
    private final DatabaseManager database;
    private final Messages messages;

    public ObjetivosPlugin(JavaPlugin plugin, DatabaseManager database, Messages messages) {
        this.plugin = plugin;
        this.database = database;
        this.messages = messages;
    }

    public void enable() {
        plugin.saveDefaultConfig();
        plugin.getLogger().info("ObjetivosPlugin ativado.");

        if (plugin.getCommand("objetivos") != null) plugin.getCommand("objetivos").setExecutor(this);
        // Carregar do banco (se existir)
        try {
            Map<UUID, Map<String, Integer>> all = database.loadAllObjetivos();
            for (Map.Entry<UUID, Map<String, Integer>> e : all.entrySet()) {
                EnumMap<TipoObjetivo, Integer> m = new EnumMap<>(TipoObjetivo.class);
                for (Map.Entry<String, Integer> t : e.getValue().entrySet()) {
                    try { TipoObjetivo tp = TipoObjetivo.valueOf(t.getKey().toUpperCase()); m.put(tp, t.getValue()); } catch (Exception ignored) {}
                }
                if (!m.isEmpty()) progressoJogadores.put(e.getKey(), m);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Falha ao carregar objetivos do DB: " + ex.getMessage());
            // fallback: carregar do config para compatibilidade
            carregarProgresso();
        }
        iniciarEventosAleatorios();
    }

    public void disable() {
        // dispatch save (não bloqueia aqui, MainPlugin aguardará saves em shutdown)
        saveState().exceptionally(e -> { plugin.getLogger().warning("Erro salvando objetivos no shutdown: " + e.getMessage()); return null; });
        plugin.getLogger().info("ObjetivosPlugin desativado.");
    }

    private void iniciarEventosAleatorios() {
        long ticksEntre = 20L * 60L * 5L; // 5 minutos em ticks
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            TipoObjetivo tipo = gerarTipoAleatorio();
            Objetivo obj = new Objetivo(tipo);
            objetivosAtivos.add(obj);
            String msg = messages.get("objetivos.broadcast.new", tipo.name());
            // avoid deprecated Bukkit.broadcastMessage — send directly to online players
            for (Player pl : plugin.getServer().getOnlinePlayers()) pl.sendMessage(msg);
        }, 0L, ticksEntre);
    }

    private TipoObjetivo gerarTipoAleatorio() {
        int rnd = random.nextInt(100) + 1;
        if (rnd <= 5) return TipoObjetivo.LENDARIO;
        if (rnd <= 35) return TipoObjetivo.EPICO;
        return TipoObjetivo.RARO;
    }

    public static class Objetivo {
        private final UUID id = UUID.randomUUID();
        private final TipoObjetivo tipo;
        private boolean completado = false;

        public Objetivo(TipoObjetivo tipo) { this.tipo = tipo; }

        public UUID getId() { return id; }
        public TipoObjetivo getTipo() { return tipo; }
        public boolean isCompletado() { return completado; }
        public void completar() { completado = true; }
    }

    /**
     * Registra progresso de um jogador para um tipo de objetivo.
     * Persistência é feita apenas em salvar (onDisable) — pode melhorar para salvar em tempo real.
     */
    public void registrarProgresso(Player p, TipoObjetivo tipo, int valor) {
        UUID id = p.getUniqueId();
        progressoJogadores.putIfAbsent(id, new EnumMap<>(TipoObjetivo.class));
        Map<TipoObjetivo, Integer> progresso = progressoJogadores.get(id);
        int newV = progresso.getOrDefault(tipo, 0) + valor;
        progresso.put(tipo, newV);
        // persistir imediatamente no DB (assíncrono)
        database.upsertObjetivoProgressAsync(id, tipo.name().toLowerCase(), newV)
            .exceptionally(e -> { plugin.getLogger().warning("Erro salvando progresso em DB: " + e.getMessage()); return null; });
        p.sendMessage(messages.get("objetivos.progress_registered", tipo.name(), progresso.get(tipo)));
    }

    /* --- Persistência simples usando config.yml --- */
    /**
     * Asynchronously persist all in-memory objectives progress and return a future that completes
     * once all underlying DB submissions finish. Does not block caller.
     */
    public java.util.concurrent.CompletableFuture<Void> saveState() {
        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<UUID, Map<TipoObjetivo, Integer>> e : progressoJogadores.entrySet()) {
            UUID id = e.getKey();
            for (Map.Entry<TipoObjetivo, Integer> p : e.getValue().entrySet()) {
                futures.add(database.upsertObjetivoProgressAsync(id, p.getKey().name().toLowerCase(), p.getValue())
                        .exceptionally(ex -> { plugin.getLogger().warning("Erro ao salvar progresso em DB: " + ex.getMessage()); return null; }));
            }
        }
        return java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]));
    }

    private void carregarProgresso() {
        if (!plugin.getConfig().contains("players")) return;
        for (String uuidStr : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
            UUID id;
            try { id = UUID.fromString(uuidStr); } catch (IllegalArgumentException ex) { continue; }
            Map<TipoObjetivo, Integer> map = new EnumMap<>(TipoObjetivo.class);
            for (TipoObjetivo t : TipoObjetivo.values()) {
                String path = "players." + uuidStr + "." + t.name().toLowerCase();
                if (plugin.getConfig().contains(path)) map.put(t, plugin.getConfig().getInt(path));
            }
            if (!map.isEmpty()) progressoJogadores.put(id, map);
        }
    }

    /* --- Comando /objetivos (listar) --- */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("objetivos")) return false;

        if (args.length == 0) {
            sender.sendMessage(messages.get("objetivos.list.title"));
            if (objetivosAtivos.isEmpty()) {
                sender.sendMessage(messages.get("objetivos.list.none"));
                return true;
            }
            for (Objetivo o : objetivosAtivos) {
                sender.sendMessage(messages.get("objetivos.list.item", o.getId(), o.getTipo(), o.isCompletado() ? messages.get("objetivos.completed") : ""));
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("progresso")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messages.get("objetivos.only_players"));
                return true;
            }
            Player p = (Player) sender;
            Map<TipoObjetivo, Integer> progresso = progressoJogadores.getOrDefault(p.getUniqueId(), Collections.emptyMap());
            sender.sendMessage(messages.get("objetivos.progress_header"));
            for (TipoObjetivo t : TipoObjetivo.values()) {
                sender.sendMessage(messages.get("objetivos.progress_line", t.name(), progresso.getOrDefault(t, 0)));
            }
            return true;
        }

        sender.sendMessage(messages.get("objetivos.usage"));
        return true;
    }
}