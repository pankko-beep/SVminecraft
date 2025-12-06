package com.servermine.util;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Messages {
    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        // ensure resource exists
        plugin.saveResource("messages.yml", false);
        File f = new File(plugin.getDataFolder(), "messages.yml");
        cfg = YamlConfiguration.loadConfiguration(f);
    }

    public String get(String key, Object... args) {
        String s = cfg.getString(key, key);
        String formatted = args.length > 0 ? String.format(s, args) : s;
        return ChatColor.translateAlternateColorCodes('&', formatted);
    }

    /**
     * Return a localized display name for a material constant (eg "DIAMOND" -> "Diamante").
     * Falls back to the raw material name if no translation is present.
     */
    public String getMaterialName(String material) {
        String key = "material." + material;
        if (cfg.contains(key)) return get(key);
        // fallback: convert enum-style NAME to Title Case
        String lower = material.toLowerCase().replace('_', ' ');
        String[] parts = lower.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }

    public FileConfiguration raw() { return cfg; }

    public void reload() throws IOException {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        if (!f.exists()) saveDefault();
        cfg = YamlConfiguration.loadConfiguration(f);
    }

    private void saveDefault() {
        plugin.saveResource("messages.yml", false);
    }
}
