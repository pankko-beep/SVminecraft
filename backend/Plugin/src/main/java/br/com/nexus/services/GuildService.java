package br.com.nexus.services;

import br.com.nexus.NexusPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GuildService {
    public static class Guild {
        public String name;
        public UUID leader;
        public final Set<UUID> members = new HashSet<>();
    }

    private final NexusPlugin plugin;
    private final Map<String, Guild> byName = new HashMap<>();
    private final Map<UUID, String> byPlayer = new HashMap<>();
    private final Map<UUID, String> invites = new HashMap<>();
    private final File file;

    public GuildService(NexusPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "guilds.yml");
        load();
    }

    public boolean exists(String name) { return byName.containsKey(name.toLowerCase()); }

    public boolean hasGuild(UUID player) { return byPlayer.containsKey(player); }

    public Guild getByPlayer(UUID player) { String n = byPlayer.get(player); return n==null?null:byName.get(n.toLowerCase()); }

    public Guild get(String name) { return byName.get(name.toLowerCase()); }

    public Guild create(String name, UUID leader) {
        String key = name.toLowerCase();
        if (byName.containsKey(key)) return null;
        Guild g = new Guild();
        g.name = name; g.leader = leader; g.members.add(leader);
        byName.put(key, g); byPlayer.put(leader, key);
        return g;
    }

    public void invite(UUID target, String guild) { invites.put(target, guild.toLowerCase()); }

    public String pendingInvite(UUID target) { return invites.get(target); }

    public boolean accept(UUID target) {
        String gname = invites.remove(target);
        if (gname == null) return false;
        Guild g = byName.get(gname);
        if (g == null) return false;
        g.members.add(target);
        byPlayer.put(target, gname);
        return true;
    }

    public boolean leave(UUID player) {
        String gname = byPlayer.remove(player);
        if (gname == null) return false;
        Guild g = byName.get(gname);
        if (g == null) return false;
        g.members.remove(player);
        if (player.equals(g.leader)) {
            if (g.members.isEmpty()) { byName.remove(gname); }
            else g.leader = g.members.iterator().next();
        }
        return true;
    }

    public void saveNow() { try { save(); } catch (Exception ignored) {} }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String key : cfg.getKeys(false)) {
            String name = key;
            Guild g = new Guild();
            g.name = name;
            g.leader = UUID.fromString(Objects.requireNonNull(cfg.getString(key + ".leader")));
            for (String s : cfg.getStringList(key + ".members")) g.members.add(UUID.fromString(s));
            byName.put(name.toLowerCase(), g);
            for (UUID m : g.members) byPlayer.put(m, name.toLowerCase());
        }
    }

    private void save() throws IOException {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Guild> e : byName.entrySet()) {
            Guild g = e.getValue();
            cfg.set(e.getKey() + ".leader", g.leader.toString());
            List<String> m = new ArrayList<>();
            for (UUID u : g.members) m.add(u.toString());
            cfg.set(e.getKey() + ".members", m);
        }
        cfg.save(file);
    }
}
