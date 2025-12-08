package br.com.nexus.services;

import br.com.nexus.NexusPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseService {
    private final NexusPlugin plugin;
    private HikariDataSource ds;

    public DatabaseService(NexusPlugin plugin) {
        this.plugin = plugin;
        init();
        migrate();
    }

    private void init() {
        String tipo = plugin.getConfig().getString("storage.tipo", "sqlite");
        HikariConfig cfg = new HikariConfig();
        cfg.setMaximumPoolSize(5);
        cfg.setPoolName("NexusPool");
        if (tipo.equalsIgnoreCase("mysql")) {
            String host = plugin.getConfig().getString("storage.mysql.host", "127.0.0.1");
            int porta = plugin.getConfig().getInt("storage.mysql.porta", 3306);
            String db = plugin.getConfig().getString("storage.mysql.database", "nexus");
            String user = plugin.getConfig().getString("storage.mysql.usuario", "root");
            String pass = plugin.getConfig().getString("storage.mysql.senha", "");
            String params = plugin.getConfig().getString("storage.mysql.parametros", "useSSL=false");
            String jdbcUrl = "jdbc:mysql://%s:%d/%s?%s".formatted(host, porta, db, params);
            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(user);
            cfg.setPassword(pass);
        } else {
            String fileName = plugin.getConfig().getString("storage.sqlite.arquivo", "database.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);
            if (!dbFile.getParentFile().exists()) dbFile.getParentFile().mkdirs();
            cfg.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        }
        ds = new HikariDataSource(cfg);
    }

    private void migrate() {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS schema_version (version INTEGER)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS audit_events (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "ts BIGINT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "actor_uuid TEXT, " +
                    "actor_name TEXT, " +
                    "target_type TEXT, " +
                    "target_id TEXT, " +
                    "data_json TEXT)"
            );
            // Indexes úteis
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_audit_ts ON audit_events(ts)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_audit_type ON audit_events(type)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Falha em migração do banco: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException { return ds.getConnection(); }

    public void close() {
        if (ds != null) ds.close();
    }
}
