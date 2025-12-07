package br.com.nexus.listeners;

import br.com.nexus.NexusPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NoTeamMovementListener implements Listener {
    private final NexusPlugin plugin;
    private final Set<UUID> notified = new HashSet<>();

    public NoTeamMovementListener(NexusPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        var p = e.getPlayer();
        if (!plugin.teams().hasTeam(p.getUniqueId())) {
            // Permitir apenas rotação; bloquear mudança de posição
            if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getY() != e.getTo().getY() || e.getFrom().getZ() != e.getTo().getZ()) {
                e.setCancelled(true);
                if (notified.add(p.getUniqueId())) {
                    String msg = plugin.getConfig().getString("mensagens.time-escolha-obrigatoria", "Escolha seu time: /time escolher <Solar|Lunar>");
                    p.sendMessage(plugin.getConfig().getString("mensagens.prefixo", "") + msg);
                }
            }
        } else {
            notified.remove(p.getUniqueId());
        }
    }
}
