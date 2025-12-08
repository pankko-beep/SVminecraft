package br.com.nexus.listeners;

import br.com.nexus.NexusPlugin;
import org.bukkit.event.Listener;

/**
 * Hook para SimpleLogin pós-autenticação. Usa reflexão para evitar dependência de compile.
 * Se o plugin SimpleLogin estiver presente e publicar um evento PlayerAuthenticateEvent,
 * chamamos o touch/save do PlayerDataService nesse momento.
 */
public class SimpleLoginHook implements Listener {
    private final NexusPlugin plugin;

    public SimpleLoginHook(NexusPlugin plugin) {
        this.plugin = plugin;
        // Registrar evento dinamicamente via reflexão
        try {
            @SuppressWarnings("unchecked")
            Class<? extends org.bukkit.event.Event> eventClass = 
                (Class<? extends org.bukkit.event.Event>) Class.forName("de.xxschrandxx.api.events.PlayerAuthenticateEvent");
            
            plugin.getServer().getPluginManager().registerEvent(
                eventClass, 
                this, 
                org.bukkit.event.EventPriority.MONITOR,
                (listener, event) -> {
                    try {
                        var m = event.getClass().getMethod("getPlayer");
                        Object playerObj = m.invoke(event);
                        if (playerObj instanceof org.bukkit.entity.Player p) {
                            long now = System.currentTimeMillis();
                            plugin.players().touch(p.getUniqueId(), p.getName(), now);
                            plugin.players().saveNow();
                        }
                    } catch (Exception ignored) { }
                },
                plugin
            );
            plugin.getLogger().info("Hook SimpleLogin registrado.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("Evento SimpleLogin não encontrado; hook não registrado.");
        }
    }
}
