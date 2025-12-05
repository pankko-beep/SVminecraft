package com.servermine.VipRecompensa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VipRecompensaPlugin implements TabExecutor {

    private final JavaPlugin plugin;
    private final com.servermine.util.Messages messages;
    private Map<UUID, VipType> jogadoresVip = new HashMap<>();
    private Map<UUID, Long> ultimaRecompensa = new HashMap<>();

    public enum VipType {
        GUERREIRO,
        LORDE,
        MAGO,
        NORMAL
    }

    public VipRecompensaPlugin(JavaPlugin plugin, com.servermine.util.Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void enable() {
        if (plugin.getCommand("vipset") != null) plugin.getCommand("vipset").setExecutor(this);
        if (plugin.getCommand("resgatavip") != null) plugin.getCommand("resgatavip").setExecutor(this);
        plugin.saveDefaultConfig();
        plugin.getLogger().info("VipRecompensaPlugin ativado.");
    }

    public void disable() {
        plugin.getLogger().info("VipRecompensaPlugin desativado.");
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return java.util.Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("vip.only_players"));
            return true;
        }

            Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("vipset")) {
                if (!sender.hasPermission("vip.admin")) {
                sender.sendMessage(messages.get("vip.no_permission"));
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage(messages.get("vip.usage_set"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(messages.get("vip.player_not_found"));
                return true;
            }
            try {
                VipType tipo = VipType.valueOf(args[1].toUpperCase());
                jogadoresVip.put(target.getUniqueId(), tipo);
                sender.sendMessage(messages.get("vip.set_success"));
                target.sendMessage(messages.get("vip.updated_for_player", tipo.name()));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(messages.get("vip.invalid_type"));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("resgatavip")) {
            long agora = System.currentTimeMillis();
            long ultimo = ultimaRecompensa.getOrDefault(p.getUniqueId(), 0L);
            if (agora - ultimo < 7*24*60*60*1000) { // 7 dias
                p.sendMessage(messages.get("vip.already_weekly"));
                return true;
            }
            VipType tipo = jogadoresVip.getOrDefault(p.getUniqueId(), VipType.NORMAL);
            // Dar recompensas conforme VIP
            p.sendMessage(messages.get("vip.claimed_weekly", tipo.name()));
            // Aqui deve vir lógica para dar itens, dinheiro etc
            ultimaRecompensa.put(p.getUniqueId(), agora);
            return true;
        }

        return false;
    }
}
