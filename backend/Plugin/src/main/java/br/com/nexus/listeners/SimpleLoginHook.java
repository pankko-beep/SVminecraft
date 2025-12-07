package br.com.nexus.listeners;

import br.com.nexus.NexusPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Hook para SimpleLogin pós-autenticação. Usa reflexão para evitar dependência de compile.
 * Se o plugin SimpleLogin estiver presente e publicar um evento PlayerAuthenticateEvent,
 * chamamos o touch/save do PlayerDataService nesse momento.
 */
public class SimpleLoginHook implements Listener {
    private final NexusPlugin plugin;

    public SimpleLoginHook(NexusPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onAuth(org.bukkit.event.Event e) {
        try {
            // Detecta classe do evento por nome
            if (!e.getClass().getName().toLowerCase().contains("authenticate")) return;
            // Tenta acessar getPlayer() via reflexão
            var m = e.getClass().getMethod("getPlayer");
            Object playerObj = m.invoke(e);
            if (playerObj instanceof org.bukkit.entity.Player p) {
                long now = System.currentTimeMillis();
                plugin.players().touch(p.getUniqueId(), p.getName(), now);
                plugin.players().saveNow();
                plugin.getLogger().info("SimpleLogin auth registrado para "+p.getName());
            }
        } catch (Exception ignored) { }
    }
}
