package br.com.nexus.listeners;

import br.com.nexus.NexusPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerLifecycleListener implements Listener {
    private final NexusPlugin plugin;

    public PlayerLifecycleListener(NexusPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var p = e.getPlayer();
        long now = System.currentTimeMillis();
        plugin.players().touch(p.getUniqueId(), p.getName(), now);
        // Opcional: futuras integrações após autenticação (SimpleLogin), se necessário.
        plugin.players().saveNow();
        plugin.audit().log("player.join", p.getUniqueId(), p.getName(), "player", p.getUniqueId().toString(), java.util.Map.of("name", p.getName()));
        // Aplicar cor do nome conforme time, se já definido
        var t = plugin.teams().get(p.getUniqueId());
        if (t != null) {
            String color = t == br.com.nexus.services.TeamService.Team.SOLAR ? plugin.getConfig().getString("mensagens.time-cor-solar", "§e") : plugin.getConfig().getString("mensagens.time-cor-lunar", "§5");
            String colored = color + p.getName() + "§r";
            try { p.setDisplayName(colored); } catch (Throwable ignored) {}
            try { p.setPlayerListName(colored); } catch (Throwable ignored) {}
        } else {
            // Prompt para escolher time
            p.sendMessage(plugin.getConfig().getString("mensagens.prefixo", "") + plugin.getConfig().getString("mensagens.time-escolha-obrigatoria", "Escolha seu time: /time escolher <Solar|Lunar>"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        var p = e.getPlayer();
        long now = System.currentTimeMillis();
        plugin.players().touch(p.getUniqueId(), p.getName(), now);
        plugin.players().saveNow();
        // Persistir serviços centrais rapidamente no quit
        plugin.teams().saveNow();
        plugin.guilds().saveNow();
        plugin.transactions().saveAsyncNow();
        plugin.audit().log("player.quit", p.getUniqueId(), p.getName(), "player", p.getUniqueId().toString(), java.util.Map.of("name", p.getName()));
    }
}
