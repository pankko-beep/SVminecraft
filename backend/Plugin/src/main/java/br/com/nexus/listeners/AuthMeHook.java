package br.com.nexus.listeners;

import br.com.nexus.NexusPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Method;
import java.util.Map;

public class AuthMeHook implements Listener {
    private final NexusPlugin plugin;

    public AuthMeHook(NexusPlugin plugin) {
        this.plugin = plugin;
        PluginManager pm = plugin.getServer().getPluginManager();
        // LoginEvent
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> loginEventClass = (Class<? extends Event>) Class.forName("fr.xephi.authme.events.LoginEvent");
            pm.registerEvent(loginEventClass, this, EventPriority.MONITOR, (listener, event) -> {
                try {
                    Method getPlayer = loginEventClass.getMethod("getPlayer");
                    Object playerObj = getPlayer.invoke(event);
                    if (playerObj instanceof Player p) {
                        long now = System.currentTimeMillis();
                        plugin.players().touch(p.getUniqueId(), p.getName(), now);
                        plugin.players().saveNow();
                        plugin.audit().log("authme.login", p.getUniqueId(), p.getName(), "player", p.getUniqueId().toString(), Map.of("name", p.getName()));
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Falha ao processar AuthMe LoginEvent: " + t.getMessage());
                }
            }, plugin);
            plugin.getLogger().info("Hook AuthMe Login registrado.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("AuthMe LoginEvent não encontrado; hook de login não registrado.");
        }

        // LogoutEvent
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> logoutEventClass = (Class<? extends Event>) Class.forName("fr.xephi.authme.events.LogoutEvent");
            pm.registerEvent(logoutEventClass, this, EventPriority.MONITOR, (listener, event) -> {
                try {
                    Method getPlayer = logoutEventClass.getMethod("getPlayer");
                    Object playerObj = getPlayer.invoke(event);
                    if (playerObj instanceof Player p) {
                        long now = System.currentTimeMillis();
                        plugin.players().touch(p.getUniqueId(), p.getName(), now);
                        plugin.players().saveNow();
                        plugin.audit().log("authme.logout", p.getUniqueId(), p.getName(), "player", p.getUniqueId().toString(), Map.of("name", p.getName()));
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("Falha ao processar AuthMe LogoutEvent: " + t.getMessage());
                }
            }, plugin);
            plugin.getLogger().info("Hook AuthMe Logout registrado.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("AuthMe LogoutEvent não encontrado; hook de logout não registrado.");
        }
    }
}
