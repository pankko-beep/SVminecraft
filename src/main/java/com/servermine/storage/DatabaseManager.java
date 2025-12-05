package com.servermine.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection conn;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "sv-db-worker"));
    private static final Gson GSON = new Gson();

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Test-friendly constructor which directly opens given JDBC url (eg. jdbc:sqlite::memory:)
     */
    public DatabaseManager(String jdbcUrl) throws SQLException {
        this.plugin = null;
        this.conn = DriverManager.getConnection(jdbcUrl);
        createTables();
    }

    public void init() throws SQLException {
        if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
        String dbFile = plugin.getDataFolder().getAbsolutePath() + "/svminecraft.db";
        // allow config override for filename
        String configured = plugin.getConfig().getString("database.file", "svminecraft.db");
        dbFile = plugin.getDataFolder().getAbsolutePath() + "/" + configured;
        String url = "jdbc:sqlite:" + dbFile;
        conn = DriverManager.getConnection(url);
        // apply PRAGMA tuning to improve concurrency and write performance
        try (Statement st = conn.createStatement()) {
            boolean wal = plugin.getConfig().getBoolean("database.wal", true);
            if (wal) st.execute("PRAGMA journal_mode = WAL;");
            String sync = plugin.getConfig().getString("database.synchronous", "NORMAL").toUpperCase();
            // clamp known values
            if (!("FULL".equals(sync) || "NORMAL".equals(sync) || "OFF".equals(sync))) sync = "NORMAL";
            st.execute("PRAGMA synchronous = " + sync + ";");
            int busy = plugin.getConfig().getInt("database.busy-timeout-ms", 5000);
            st.execute("PRAGMA busy_timeout = " + busy + ";");
        } catch (SQLException ex) {
            plugin.getLogger().warning("Não foi possível aplicar PRAGMA ao DB: " + ex.getMessage());
        }
        createTables();
    }

    private void createTables() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS objetivos_progress (player_uuid TEXT NOT NULL, tipo TEXT NOT NULL, progress INTEGER NOT NULL, PRIMARY KEY(player_uuid, tipo));");
            st.execute("CREATE TABLE IF NOT EXISTS ranks (player_uuid TEXT PRIMARY KEY, xp INTEGER NOT NULL, level INTEGER NOT NULL);");
            // lojas metadata and JSON items column (items stored as JSON text)
            st.execute("CREATE TABLE IF NOT EXISTS lojas (owner_uuid TEXT PRIMARY KEY, owner_name TEXT, items TEXT);");
        }
    }

    /**
     * Migration helper to import old config-based data into the DB.
     * Detects `players` section (used by previous Objetivos implementation) and inserts entries
     * into objetivos_progress if the table is currently empty.
     */
    public void migrateFromConfig(JavaPlugin plugin) {
        try {
            // only migrate if DB does not have any objetivos
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) as c FROM objetivos_progress"); ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("c") > 0) return; // already have data, skip
            }

            if (!plugin.getConfig().contains("players")) return;
            for (String uuidStr : plugin.getConfig().getConfigurationSection("players").getKeys(false)) {
                UUID id;
                try { id = UUID.fromString(uuidStr); } catch (IllegalArgumentException ex) { continue; }
                for (String tipo : plugin.getConfig().getConfigurationSection("players." + uuidStr).getKeys(false)) {
                    int val = plugin.getConfig().getInt("players." + uuidStr + "." + tipo);
                    try { upsertObjetivoProgress(id, tipo, val); } catch (SQLException ignore) {}
                }
            }

            // Optional: migrate lojas from config.lojas -> lojas.items JSON column (if present and DB empty)
            try (PreparedStatement psCheck = conn.prepareStatement("SELECT COUNT(*) c FROM lojas WHERE items IS NOT NULL"); ResultSet rsCheck = psCheck.executeQuery()) {
                if (rsCheck.next() && rsCheck.getInt("c") == 0 && plugin.getConfig().contains("lojas")) {
                    for (String ownerUuid : plugin.getConfig().getConfigurationSection("lojas").getKeys(false)) {
                        String items = plugin.getConfig().getString("lojas." + ownerUuid + ".items");
                        if (items != null) {
                            // parse legacy items string MATERIAL,amount;MATERIAL,amount
                            java.util.List<ItemRecord> list = new java.util.ArrayList<>();
                            String[] parts = items.split(";");
                            int slot = 0;
                            for (String p : parts) {
                                if (p.isBlank()) continue;
                                String[] a = p.split(",");
                                try { String mat = a[0]; int amt = a.length > 1 ? Integer.parseInt(a[1]) : 1; list.add(new ItemRecord(slot++, mat, amt)); } catch (Exception ignored) {}
                            }
                            try { saveLojaItems(UUID.fromString(ownerUuid), list); } catch (SQLException ignore) {}
                        }
                    }
                }
            }
            // remove migrated sections to avoid duplications
            plugin.getConfig().set("players", null);
            plugin.getConfig().set("lojas", null);
            plugin.saveConfig();
        } catch (Exception ex) {
            plugin.getLogger().warning("Erro na migração config->DB: " + ex.getMessage());
        }
    }

    // Objetivos
    public Map<String, Integer> loadObjetivosFor(UUID player) throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT tipo, progress FROM objetivos_progress WHERE player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("tipo"), rs.getInt("progress"));
                }
            }
        }
        return result;
    }

    public Map<UUID, Map<String, Integer>> loadAllObjetivos() throws SQLException {
        Map<UUID, Map<String, Integer>> out = new HashMap<>();
        String sql = "SELECT player_uuid, tipo, progress FROM objetivos_progress";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_uuid"));
                String tipo = rs.getString("tipo");
                int progress = rs.getInt("progress");
                out.computeIfAbsent(id, k -> new HashMap<>()).put(tipo, progress);
            }
        }
        return out;
    }

    public void upsertObjetivoProgress(UUID player, String tipo, int progress) throws SQLException {
        String sql = "INSERT INTO objetivos_progress(player_uuid, tipo, progress) VALUES(?,?,?) ON CONFLICT(player_uuid, tipo) DO UPDATE SET progress=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setString(2, tipo);
            ps.setInt(3, progress);
            ps.setInt(4, progress);
            ps.executeUpdate();
        }
    }

    // Loja persistence (itemsStr is a compact representation of items: MATERIAL:amount;MATERIAL:amount)
    /**
    * Legacy compatibility: still support inserting a serialized items string into lojas.owner_name column
    * new format stores items as JSON in lojas.items column.
     */
    public void upsertLoja(UUID owner, String ownerName) throws SQLException {
        String sql = "INSERT INTO lojas(owner_uuid, owner_name) VALUES(?,?) ON CONFLICT(owner_uuid) DO UPDATE SET owner_name=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setString(2, ownerName);
            ps.setString(3, ownerName);
            ps.executeUpdate();
        }
    }

    public void saveLojaItems(UUID owner, java.util.List<ItemRecord> items) throws SQLException {
        // serialize items to JSON and store in lojas.items
        String json = GSON.toJson(items);
        String sql = "INSERT INTO lojas(owner_uuid, owner_name, items) VALUES(?,?,?) ON CONFLICT(owner_uuid) DO UPDATE SET items=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setString(2, null);
            ps.setString(3, json);
            ps.setString(4, json);
            ps.executeUpdate();
        }
    }

    public java.util.List<ItemRecord> loadLojaItems(UUID owner) throws SQLException {
        String sql = "SELECT items FROM lojas WHERE owner_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("items");
                    if (json == null || json.isBlank()) return new java.util.ArrayList<>();
                    java.lang.reflect.Type t = new TypeToken<java.util.List<ItemRecord>>(){}.getType();
                    return GSON.fromJson(json, t);
                }
            }
        }
        return new java.util.ArrayList<>();
    }

    public Map<UUID, java.util.List<ItemRecord>> loadAllLojasItems() throws SQLException {
        Map<UUID, java.util.List<ItemRecord>> out = new HashMap<>();
        String sql = "SELECT owner_uuid, items FROM lojas WHERE items IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            java.lang.reflect.Type t = new TypeToken<java.util.List<ItemRecord>>(){}.getType();
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("owner_uuid"));
                String json = rs.getString("items");
                if (json == null || json.isBlank()) continue;
                java.util.List<ItemRecord> list = GSON.fromJson(json, t);
                out.put(id, list == null ? new java.util.ArrayList<>() : list);
            }
        }
        return out;
    }

    public Map<UUID, String> loadAllLojasMeta() throws SQLException {
        Map<UUID, String> out = new HashMap<>();
        String sql = "SELECT owner_uuid, owner_name FROM lojas";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.put(UUID.fromString(rs.getString("owner_uuid")), rs.getString("owner_name"));
        }
        return out;
    }

    // Ranks
    public Rank loadRank(UUID player) throws SQLException {
        String sql = "SELECT xp, level FROM ranks WHERE player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new Rank(rs.getInt("xp"), rs.getInt("level"));
            }
        }
        return null;
    }

    public Map<UUID, Rank> loadAllRanks() throws SQLException {
        Map<UUID, Rank> out = new HashMap<>();
        String sql = "SELECT player_uuid, xp, level FROM ranks";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("player_uuid"));
                out.put(id, new Rank(rs.getInt("xp"), rs.getInt("level")));
            }
        }
        return out;
    }

    public void upsertRank(UUID player, int xp, int level) throws SQLException {
        String sql = "INSERT INTO ranks(player_uuid, xp, level) VALUES(?,?,?) ON CONFLICT(player_uuid) DO UPDATE SET xp=?, level=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.toString());
            ps.setInt(2, xp);
            ps.setInt(3, level);
            ps.setInt(4, xp);
            ps.setInt(5, level);
            ps.executeUpdate();
        }
    }

    public void close() {
        try { if (conn != null && !conn.isClosed()) conn.close(); } catch (SQLException ignore) {}
    }

    public CompletableFuture<Void> upsertObjetivoProgressAsync(UUID player, String tipo, int progress) {
        return CompletableFuture.runAsync(() -> {
            try { upsertObjetivoProgress(player, tipo, progress); } catch (SQLException ex) { throw new RuntimeException(ex); }
        }, executor);
    }

    public CompletableFuture<Void> upsertRankAsync(UUID player, int xp, int level) {
        return CompletableFuture.runAsync(() -> {
            try { upsertRank(player, xp, level); } catch (SQLException ex) { throw new RuntimeException(ex); }
        }, executor);
    }

    public CompletableFuture<Void> upsertLojaAsync(UUID owner, String itemsStr) {
        // legacy async upsert using owner name
        return CompletableFuture.runAsync(() -> {
            try { upsertLoja(owner, itemsStr); } catch (SQLException ex) { throw new RuntimeException(ex); }
        }, executor);
    }

    public CompletableFuture<Void> saveLojaItemsAsync(UUID owner, java.util.List<ItemRecord> items) {
        return CompletableFuture.runAsync(() -> {
            try { saveLojaItems(owner, items); } catch (SQLException ex) { throw new RuntimeException(ex); }
        }, executor);
    }

    public CompletableFuture<Map<String, Integer>> loadObjetivosForAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            try { return loadObjetivosFor(player); } catch (SQLException ex) { throw new RuntimeException(ex); }
        }, executor);
    }

    public void shutdownExecutor() {
        executor.shutdown();
        int wait = 8;
        if (plugin != null) wait = plugin.getConfig().getInt("performance.shutdown-wait-seconds", 8);
        try { executor.awaitTermination(wait, TimeUnit.SECONDS); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
    }

    public static class Rank {
        public final int xp; public final int level;
        public Rank(int xp, int level) { this.xp = xp; this.level = level; }
    }

    public static class ItemRecord {
        public final int slot;
        public final String material;
        public final int amount;

        public ItemRecord(int slot, String material, int amount) {
            this.slot = slot;
            this.material = material;
            this.amount = amount;
        }
    }
}
