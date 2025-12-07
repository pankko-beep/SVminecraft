package br.com.nexus.services;

import br.com.nexus.NexusPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamService {
    public enum Team { SOLAR, LUNAR }

    private final NexusPlugin plugin;
    private final Map<UUID, Team> teams = new HashMap<>();
    private final File file;

    public TeamService(NexusPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "teams.yml");
        load();
    }

    public Team get(UUID uuid) { return teams.get(uuid); }

    public boolean hasTeam(UUID uuid) { return teams.containsKey(uuid); }

    public void set(UUID uuid, Team team) { teams.put(uuid, team); }

    public void saveNow() {
        try { save(); } catch (Exception ignored) {}
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            UUID u = UUID.fromString(key);
            Team t = Team.valueOf(cfg.getString(key));
            teams.put(u, t);
        }
    }

    private void save() throws IOException {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<UUID, Team> e : teams.entrySet()) cfg.set(e.getKey().toString(), e.getValue().name());
        cfg.save(file);
    }
}
