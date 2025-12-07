package br.com.nexus.services;

import br.com.nexus.NexusPlugin;
import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.sql.ResultSet;

public class AuditService {
    private final NexusPlugin plugin;
    private final DatabaseService db;
    private final Gson gson = new Gson();

    public AuditService(NexusPlugin plugin, DatabaseService db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void log(String type, UUID actorUuid, String actorName, String targetType, String targetId, Map<String, Object> data) {
        String json = data == null ? null : gson.toJson(data);
        long now = System.currentTimeMillis();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection c = db.getConnection();
                 PreparedStatement ps = c.prepareStatement("INSERT INTO audit_events (ts, type, actor_uuid, actor_name, target_type, target_id, data_json) VALUES (?,?,?,?,?,?,?)")) {
                ps.setLong(1, now);
                ps.setString(2, type);
                ps.setString(3, actorUuid == null ? null : actorUuid.toString());
                ps.setString(4, actorName);
                ps.setString(5, targetType);
                ps.setString(6, targetId);
                ps.setString(7, json);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().warning("Falha ao gravar audit: " + e.getMessage());
            }
        });
    }

        public Map<String, Integer> countByType(long afterMillis, int limit) {
            Map<String, Integer> out = new LinkedHashMap<>();
            String sql = "SELECT type, COUNT(*) c FROM audit_events WHERE ts>=? GROUP BY type ORDER BY c DESC LIMIT ?";
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, afterMillis);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) out.put(rs.getString(1), rs.getInt(2));
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Falha ao consultar contagem de auditoria: "+e.getMessage());
            }
            return out;
        }

        public Map<String, Integer> countForTeam(String teamName, long afterMillis, int limit) {
            Map<String,Integer> out = new LinkedHashMap<>();
            String sql = "SELECT type, COUNT(*) c FROM audit_events WHERE ts>=? AND data_json LIKE ? GROUP BY type ORDER BY c DESC LIMIT ?";
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, afterMillis);
                ps.setString(2, "%\"team\":\""+teamName+"\"%");
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.put(rs.getString(1), rs.getInt(2)); }
            } catch (Exception e) { plugin.getLogger().warning("Falha contagem por time: "+e.getMessage()); }
            return out;
        }

        public Map<String, Integer> countForGuild(String guildName, long afterMillis, int limit) {
            Map<String,Integer> out = new LinkedHashMap<>();
            String sql = "SELECT type, COUNT(*) c FROM audit_events WHERE ts>=? AND (target_type='guild' AND target_id=?) GROUP BY type ORDER BY c DESC LIMIT ?";
            try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setLong(1, afterMillis);
                ps.setString(2, guildName);
                ps.setInt(3, limit);
                try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.put(rs.getString(1), rs.getInt(2)); }
            } catch (Exception e) { plugin.getLogger().warning("Falha contagem por guilda: "+e.getMessage()); }
            return out;
        }
}
