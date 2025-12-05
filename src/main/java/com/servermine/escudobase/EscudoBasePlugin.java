package com.servermine.escudobase;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class EscudoBasePlugin implements Listener {

    private final JavaPlugin plugin;
    private final com.servermine.util.Messages messages;
    private Map<String, Escudo> escudosAtivos = new HashMap<>();

    public EscudoBasePlugin(JavaPlugin plugin, com.servermine.util.Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("EscudoBasePlugin ativado.");
    }

    public void disable() {
        plugin.getLogger().info("EscudoBasePlugin desativado.");
    }

    @EventHandler
    public void onUsarEscudo(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItem();
        if (item == null) return;
        if (item.getType() != Material.DIAMOND) return; // Item usado como "escudo"
        if (!p.isSneaking()) return; // Exemplo: só ativa se estiver agachado

        String guilda = getGuildaJogador(p);
        if (guilda == null) {
            p.sendMessage(messages.get("escudo.no_guild"));
            return;
        }

        if (escudosAtivos.containsKey(guilda)) {
            p.sendMessage(messages.get("escudo.already_active"));
            return;
        }

        // Verificar se todos jogadores da guilda saíram da base (simplificado)
        if (!todosJogadoresForaBase(guilda)) {
            p.sendMessage(messages.get("escudo.members_inside_base"));
            return;
        }

        Escudo escudo = new Escudo(guilda, p.getLocation());
        escudosAtivos.put(guilda, escudo);

        p.sendMessage(messages.get("escudo.activated"));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            escudo.ativarProtecao();
            broadcastGuildaAtacada(guilda);
        }, 20L * 60 * 5);
    }

    private String getGuildaJogador(Player p) {
        // Aqui deve ser sua lógica de guilda
        // Exemplo fixo
        return "GuildaExemplo";
    }

    private boolean todosJogadoresForaBase(String guilda) {
        // Lógica para verificar se todos saíram da base
        // Deve integrar seu sistema de localização
        return true;
    }

    private void broadcastGuildaAtacada(String guilda) {
        String msg = messages.get("escudo.guild_attacked", guilda);
        // avoid deprecated broadcast call
        for (Player p : plugin.getServer().getOnlinePlayers()) p.sendMessage(msg);
    }

    private class Escudo {
        private final String guilda;
        private final Location centro;
        private boolean ativo = false;
        private int nivel = 1; // Pode setar de acordo com item usado

        public Escudo(String guilda, Location centro) {
            this.guilda = guilda;
            this.centro = centro;
        }

        public void ativarProtecao() {
            ativo = true;
            // Aqui implementa o aumento da vida do Nexus (multiplicador 1.5x)
            // E proteção contra destruição de blocos na área do escudo
            // Se nivel 2 ou 3 aplica efeitos de poison/lentidão nos inimigos na área

            // Exemplo para dar efeitos em inimigos:
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isInArea(p.getLocation())) {
                    if (!guilda.equals(getGuildaJogador(p))) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20*20, nivel-1));
                        if (nivel >= 3) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20*20, 0));
                        }
                    }
                }
            }

            // Agendar fim do escudo conforme nível (exemplo 24h)
            long duracaoTicks = 20L * 60 * 60 * 24; // 24 horas
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ativo = false;
                Bukkit.broadcastMessage(messages.get("escudo.protection_expired", guilda));
                escudosAtivos.remove(guilda);
            }, duracaoTicks);
        }

        private boolean isInArea(Location loc) {
            int raio = switch (nivel) {
                case 1 -> 12;
                case 2 -> 25;
                case 3 -> 60;
                default -> 12;
            };
            return centro.getWorld().equals(loc.getWorld())
                    && centro.distance(loc) <= raio;
        }
    }
}
