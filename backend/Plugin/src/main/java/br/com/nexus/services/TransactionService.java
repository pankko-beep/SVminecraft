package br.com.nexus.services;

import br.com.nexus.NexusPlugin;
import org.bukkit.Bukkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TransactionService {
    public static class Entry {
        public String id;
        public UUID from;
        public UUID to;
        public double amount;
        public long time;
        public String note;
    }

    private final NexusPlugin plugin;
    private final Map<UUID, Deque<Entry>> history = new HashMap<>();
    private final File file;

    public TransactionService(NexusPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "transactions.yml");
        load();
        int interval = plugin.getConfig().getInt("auditoria.salvar-intervalo-segundos", 60);
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveSafe, interval * 20L, interval * 20L);
    }

    public void add(UUID from, UUID to, double amount, String note) {
        Entry e = new Entry();
        e.id = UUID.randomUUID().toString();
        e.from = from; e.to = to; e.amount = amount; e.time = System.currentTimeMillis(); e.note = note;
        push(from, e); push(to, e);
        // Persistir no DB (auditoria transacional simples)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = plugin.database().getConnection();
                 PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS transactions (id TEXT PRIMARY KEY, ts BIGINT, from_uuid TEXT, to_uuid TEXT, amount DOUBLE, note TEXT)")) {
                ps.executeUpdate();
            } catch (SQLException ignored) {}
            try (Connection c = plugin.database().getConnection();
                 PreparedStatement ps = c.prepareStatement("INSERT OR REPLACE INTO transactions (id, ts, from_uuid, to_uuid, amount, note) VALUES (?,?,?,?,?,?)")) {
                ps.setString(1, e.id);
                ps.setLong(2, e.time);
                ps.setString(3, e.from.toString());
                ps.setString(4, e.to.toString());
                ps.setDouble(5, e.amount);
                ps.setString(6, e.note);
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Falha ao gravar transação em DB: "+ex.getMessage());
            }
        });
    }

    private void push(UUID who, Entry e) {
        history.computeIfAbsent(who, k -> new ArrayDeque<>());
        Deque<Entry> dq = history.get(who);
        dq.addFirst(e);
        int max = plugin.getConfig().getInt("auditoria.historico-por-jogador", 30);
        while (dq.size() > max) dq.removeLast();
    }

    public List<Entry> recent(UUID who) {
        Deque<Entry> dq = history.getOrDefault(who, new ArrayDeque<>());
        return new ArrayList<>(dq);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            List<Map<?, ?>> list = cfg.getMapList(key);
            Deque<Entry> dq = new ArrayDeque<>();
            for (Map<?, ?> m : list) {
                Entry e = new Entry();
                e.id = (String) m.get("id");
                e.from = UUID.fromString((String) m.get("from"));
                e.to = UUID.fromString((String) m.get("to"));
                e.amount = ((Number) m.get("amount")).doubleValue();
                e.time = ((Number) m.get("time")).longValue();
                e.note = (String) m.get("note");
                dq.add(e);
            }
            history.put(uuid, dq);
        }
    }

    public void saveAsyncNow() { Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveSafe); }

    public void saveNow() { saveSafe(); }

    private void saveSafe() {
        try {
            save();
        } catch (Exception e) {
            plugin.getLogger().warning("Falha ao salvar transactions.yml: " + e.getMessage());
        }
    }

    private void save() throws IOException {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Deque<Entry>> en : history.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Entry e : en.getValue()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", e.id);
                m.put("from", e.from.toString());
                m.put("to", e.to.toString());
                m.put("amount", e.amount);
                m.put("time", e.time);
                m.put("note", e.note);
                list.add(m);
            }
            cfg.set(en.getKey().toString(), list);
        }
        cfg.save(file);
    }
}
