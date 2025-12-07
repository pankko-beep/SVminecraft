package br.com.nexus.services;

import br.com.nexus.NexusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataService {
    public static class PlayerRecord {
        public String name;
        public long firstSeen;
        public long lastSeen;
    }

    private final NexusPlugin plugin;
    private final Map<UUID, PlayerRecord> data = new HashMap<>();
    private final File file;

    public PlayerDataService(NexusPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
        int interval = plugin.getConfig().getInt("auditoria.salvar-intervalo-segundos", 60);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveSafe, interval * 20L, interval * 20L);
    }

    public void touch(UUID uuid, String currentName, long now) {
        PlayerRecord r = data.computeIfAbsent(uuid, k -> {
            PlayerRecord pr = new PlayerRecord();
            pr.firstSeen = now;
            return pr;
        });
        r.name = currentName;
        r.lastSeen = now;
    }

    public PlayerRecord get(UUID uuid) { return data.get(uuid); }

    public void saveNow() { saveSafe(); }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String k : cfg.getKeys(false)) {
            UUID uuid = UUID.fromString(k);
            PlayerRecord r = new PlayerRecord();
            r.name = cfg.getString(k + ".name", "?");
            r.firstSeen = cfg.getLong(k + ".firstSeen", System.currentTimeMillis());
            r.lastSeen = cfg.getLong(k + ".lastSeen", r.firstSeen);
            data.put(uuid, r);
        }
    }

    private void saveSafe() {
        try { save(); } catch (Exception e) {
            plugin.getLogger().warning("Falha ao salvar players.yml: " + e.getMessage());
        }
    }

    private void save() throws IOException {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerRecord> e : data.entrySet()) {
            String key = e.getKey().toString();
            PlayerRecord r = e.getValue();
            cfg.set(key + ".name", r.name);
            cfg.set(key + ".firstSeen", r.firstSeen);
            cfg.set(key + ".lastSeen", r.lastSeen);
        }
        cfg.save(file);
    }
}
