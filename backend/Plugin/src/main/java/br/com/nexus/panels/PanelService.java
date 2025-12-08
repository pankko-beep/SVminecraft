package br.com.nexus.panels;

import br.com.nexus.NexusPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PanelService {
    public enum Type { GLOBAL, TIME, GUILDA }
    public static class Panel {
        public String id;
        public Type type;
        public Location loc;
        public String guildName; // quando type=GUILDA, opcional: guilda alvo
    }

    private final NexusPlugin plugin;
    private final Map<String, Panel> panels = new HashMap<>();
    private final File file;
    private final Map<String, UUID> displayIds = new HashMap<>();

    public PanelService(NexusPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), plugin.getConfig().getString("painel.arquivo-persistencia", "panels.yml"));
        load();
    }

    public Panel create(Type type, Location loc) {
        Panel p = new Panel();
        p.id = UUID.randomUUID().toString().substring(0, 8);
        p.type = type; p.loc = loc.clone();
        panels.put(p.id, p);
        spawn(p, linesFor(p));
        return p;
    }

    public Panel createGuildPanel(String guildName, Location loc) {
        Panel p = new Panel();
        p.id = UUID.randomUUID().toString().substring(0, 8);
        p.type = Type.GUILDA; p.loc = loc.clone(); p.guildName = guildName;
        panels.put(p.id, p);
        spawn(p, linesFor(p));
        return p;
    }

    public boolean delete(String id) {
        Panel p = panels.remove(id);
        if (p == null) return false;
        // no-op: text display will expire if not tracked; could add persistent tag cleanup
        return true;
    }

    public Map<String, Panel> all() { return panels; }

    private void spawn(Panel p, List<String> lines) {
        // Se DecentHolograms estiver presente e configurado, poderíamos delegar.
        boolean useDH = plugin.getConfig().getBoolean("painel.usar-decent-holograms", true)
                && plugin.getServer().getPluginManager().isPluginEnabled("DecentHolograms");
        if (useDH) {
            String name = "nexus_" + p.id;
            // Cria e posiciona explicitamente no mundo
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh create %s".formatted(name));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format(Locale.ROOT, "dh setlocation %s %s %f %f %f", name, p.loc.getWorld().getName(), p.loc.getX(), p.loc.getY(), p.loc.getZ()));
            for (String line : lines) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh addline %s %s".formatted(name, line));
            return;
        }
        // Fallback: TextDisplay estável, sem rotação inclinada
        TextDisplay td = p.loc.getWorld().spawn(p.loc, TextDisplay.class, e -> {
            e.setBillboard(Display.Billboard.CENTER);
            e.setSeeThrough(false);
            e.setShadowed(false);
            e.setDefaultBackground(false);
            e.setText(String.join("\n", lines));
        });
        displayIds.put(p.id, td.getUniqueId());
    }

    private List<String> linesFor(Panel p) {
        List<String> lines = new ArrayList<>();
        String title = switch (p.type) {
            case GLOBAL -> plugin.getConfig().getString("painel.texto.titulo-global", "Nexus — Global");
            case TIME -> plugin.getConfig().getString("painel.texto.titulo-time", "Nexus — Time");
            case GUILDA -> plugin.getConfig().getString("painel.texto.titulo-guilda", "Nexus — Guilda");
        };
        lines.add(title);
        if (p.type == Type.GLOBAL) {
            int mins = plugin.getConfig().getInt("painel.metricas.janela-minutos", 60);
            int top = plugin.getConfig().getInt("painel.metricas.top", 6);
            long after = System.currentTimeMillis() - mins*60_000L;
            Map<String,Integer> counts = plugin.audit().countByType(after, top);
            int total = 0; for (int v : counts.values()) total += v;
            lines.add("§7Últimos %d min — total §f%d".formatted(mins, total));
            for (Map.Entry<String,Integer> en : counts.entrySet()) lines.add("§b• §f"+en.getKey()+": §e"+en.getValue());
            if (counts.isEmpty()) lines.add("§7Sem eventos no período.");
        } else if (p.type == Type.TIME) {
            int mins = plugin.getConfig().getInt("painel.metricas.janela-minutos", 60);
            int top = plugin.getConfig().getInt("painel.metricas.top", 6);
            long after = System.currentTimeMillis() - mins*60_000L;
            // Para exemplo, mostramos contadores para ambos os times
            var solar = plugin.audit().countForTeam("SOLAR", after, top);
            var lunar = plugin.audit().countForTeam("LUNAR", after, top);
            lines.add("§7Últimos %d min".formatted(mins));
            lines.add("§eSolar:");
            if (solar.isEmpty()) lines.add("§7  — sem eventos"); else for (var en : solar.entrySet()) lines.add("§b  • §f"+en.getKey()+": §e"+en.getValue());
            lines.add("§9Lunar:");
            if (lunar.isEmpty()) lines.add("§7  — sem eventos"); else for (var en : lunar.entrySet()) lines.add("§b  • §f"+en.getKey()+": §e"+en.getValue());
        } else if (p.type == Type.GUILDA) {
            int mins = plugin.getConfig().getInt("painel.metricas.janela-minutos", 60);
            int top = plugin.getConfig().getInt("painel.metricas.top", 6);
            long after = System.currentTimeMillis() - mins*60_000L;
            if (p.guildName != null) {
                lines.add("§7Guilda §f%s §7— últimos %d min".formatted(p.guildName, mins));
                Map<String,Integer> counts = plugin.audit().countForGuild(p.guildName, after, top);
                for (Map.Entry<String,Integer> en : counts.entrySet()) lines.add("§b• §f"+en.getKey()+": §e"+en.getValue());
                if (counts.isEmpty()) lines.add("§7Sem eventos no período.");
                return lines;
            }
            // fallback: geral por tipo
            lines.add("§7Últimos %d min — eventos (top %d)".formatted(mins, top));
            Map<String,Integer> counts = plugin.audit().countByType(after, top);
            for (Map.Entry<String,Integer> en : counts.entrySet()) lines.add("§b• §f"+en.getKey()+": §e"+en.getValue());
            if (counts.isEmpty()) lines.add("§7Sem eventos no período.");
        }
        return lines;
    }

    public void refreshAll() {
        for (Panel p : panels.values()) refresh(p);
    }

    private void refresh(Panel p) {
        List<String> lines = linesFor(p);
        boolean useDH = plugin.getConfig().getBoolean("painel.usar-decent-holograms", true)
                && plugin.getServer().getPluginManager().isPluginEnabled("DecentHolograms");
        if (useDH) {
            String name = "nexus_" + p.id;
            // Recria para garantir atualização
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh delete %s".formatted(name));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh create %s".formatted(name));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format(Locale.ROOT, "dh setlocation %s %s %f %f %f", name, p.loc.getWorld().getName(), p.loc.getX(), p.loc.getY(), p.loc.getZ()));
            for (String line : lines) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dh addline %s %s".formatted(name, line));
            return;
        }
        UUID id = displayIds.get(p.id);
        TextDisplay td = null;
        if (id != null) {
            var ent = plugin.getServer().getEntity(id);
            if (ent instanceof TextDisplay display) td = display;
        }
        if (td == null) { spawn(p, lines); return; }
        td.setText(String.join("\n", lines));
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        for (String id : cfg.getKeys(false)) {
            String world = cfg.getString(id + ".world");
            double x = cfg.getDouble(id + ".x");
            double y = cfg.getDouble(id + ".y");
            double z = cfg.getDouble(id + ".z");
            float yaw = (float) cfg.getDouble(id + ".yaw");
            float pitch = (float) cfg.getDouble(id + ".pitch");
            String type = cfg.getString(id + ".type");
            Location loc = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);
            Panel p = new Panel();
            p.id = id;
            p.type = Type.valueOf(type);
            p.loc = loc;
            p.guildName = cfg.getString(id + ".guildName", null);
            panels.put(id, p);
        }
    }

    public void saveNow() { try { save(); } catch (Exception ignored) {} }

    private void save() throws IOException {
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Panel> e : panels.entrySet()) {
            Panel p = e.getValue();
            cfg.set(e.getKey() + ".world", p.loc.getWorld().getName());
            cfg.set(e.getKey() + ".x", p.loc.getX());
            cfg.set(e.getKey() + ".y", p.loc.getY());
            cfg.set(e.getKey() + ".z", p.loc.getZ());
            cfg.set(e.getKey() + ".yaw", p.loc.getYaw());
            cfg.set(e.getKey() + ".pitch", p.loc.getPitch());
            cfg.set(e.getKey() + ".type", p.type.name());
            if (p.guildName != null) cfg.set(e.getKey() + ".guildName", p.guildName);
        }
        cfg.save(file);
    }
}
