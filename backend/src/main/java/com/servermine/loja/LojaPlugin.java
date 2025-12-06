package com.servermine.loja;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import com.servermine.storage.DatabaseManager;
import com.servermine.util.Messages;

public class LojaPlugin implements Listener, TabExecutor {

    private final JavaPlugin plugin;
    private final DatabaseManager database;
    private final Messages messages;
    private Map<UUID, Loja> lojas = new HashMap<>();
    private Map<UUID, Long> ultimaRecompensa = new HashMap<>();

    private final Map<Material, Integer> precoTabelado = Map.of(
        Material.SUGAR_CANE, 1,
        Material.CACTUS, 2,
        Material.IRON_INGOT, 4,
        Material.GOLD_INGOT, 6,
        Material.DIAMOND, 10,
        Material.LAPIS_LAZULI, 1,
        Material.REDSTONE, 1,
        Material.OBSIDIAN, 8,
        Material.NETHER_STAR, 500
    );

    public LojaPlugin(JavaPlugin plugin, DatabaseManager database, Messages messages) {
        this.plugin = plugin;
        this.database = database;
        this.messages = messages;
    }

    public void enable() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        if (plugin.getCommand("loja") != null) plugin.getCommand("loja").setExecutor(this);
        if (plugin.getCommand("recompensa") != null) plugin.getCommand("recompensa").setExecutor(this);
        plugin.saveDefaultConfig();
        plugin.getLogger().info("LojaPlugin ativado.");
        // carregar lojas do DB a partir das tabelas normalizadas (meta + itens)
        try {
            Map<UUID, java.util.List<DatabaseManager.ItemRecord>> all = database.loadAllLojasItems();
            Map<UUID, String> meta = database.loadAllLojasMeta();
            for (Map.Entry<UUID, java.util.List<DatabaseManager.ItemRecord>> e : all.entrySet()) {
                String ownerName = meta.getOrDefault(e.getKey(), "<unknown>");
                Loja l = new Loja(ownerName);
                for (DatabaseManager.ItemRecord it : e.getValue()) {
                    try {
                        org.bukkit.Material mat = org.bukkit.Material.valueOf(it.material);
                        int amt = it.amount;
                        org.bukkit.inventory.ItemStack stack = new org.bukkit.inventory.ItemStack(mat, amt);
                        if (it.slot >= 0 && it.slot < 9) {
                            while (l.getItens().size() <= it.slot) l.getItens().add(new org.bukkit.inventory.ItemStack(org.bukkit.Material.AIR));
                            l.getItens().set(it.slot, stack);
                        } else {
                            l.getItens().add(stack);
                        }
                    } catch (Exception ignored) {}
                }
                lojas.put(e.getKey(), l);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Erro carregando lojas do DB: " + ex.getMessage());
        }
    }

    public void disable() {
        // persistir lojas em background (não bloquear aqui)
        saveState().exceptionally(ex -> { plugin.getLogger().warning("Erro salvando lojas no shutdown: " + ex.getMessage()); return null; });
        plugin.getLogger().info("LojaPlugin desativado.");
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        // Sugestões de tab-complete podem ser implementadas aqui
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.get("loja.only_players"));
            return true;
        }
            Player p = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("loja")) {
            if (args.length == 0) {
                p.sendMessage(messages.get("loja.usage"));
                return true;
            }
            if (args[0].equalsIgnoreCase("criar")) {
                if (lojas.containsKey(p.getUniqueId())) {
                    p.sendMessage(messages.get("loja.already_exists"));
                    return true;
                }
                Loja nova = new Loja(p.getName());
                lojas.put(p.getUniqueId(), nova);
                // persist loja vazia
                try { database.upsertLojaAsync(p.getUniqueId(), p.getName()); } catch (Exception ignored) {}
                try { database.saveLojaItemsAsync(p.getUniqueId(), java.util.Collections.emptyList()); } catch (Exception ignored) {}
                p.sendMessage(messages.get("loja.created"));
                return true;
            }
            // Abrir loja de outro jogador
            String dono = args[0];
            UUID donoUUID = null;
            for (UUID u : lojas.keySet()) {
                if (lojas.get(u).getDono().equalsIgnoreCase(dono)) {
                    donoUUID = u;
                    break;
                }
            }
            if (donoUUID == null) {
                p.sendMessage(messages.get("loja.not_found"));
                return true;
            }
            abrirLoja(p, lojas.get(donoUUID));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("recompensa")) {
            long agora = System.currentTimeMillis();
            long ultimo = ultimaRecompensa.getOrDefault(p.getUniqueId(), 0L);
            if (agora - ultimo < 24*60*60*1000) {
                p.sendMessage(messages.get("loja.daily_reward.claimed"));
                return true;
            }
            // Aqui pode-se colocar a lógica de recompensa
            // Para exemplo:
            p.sendMessage(messages.get("loja.daily_reward.success"));
            ultimaRecompensa.put(p.getUniqueId(), agora);
            return true;
        }

        return false;
    }

    private void abrirLoja(Player p, Loja loja) {
        Inventory inv = Bukkit.createInventory(null, 9, messages.get("loja.window_title", loja.getDono()));
        for (int i = 0; i < loja.getItens().size() && i < 9; i++) {
            inv.setItem(i, loja.getItens().get(i));
        }
        p.openInventory(inv);
    }

    /**
     * Persiste todas as lojas para o banco de dados de forma assíncrona.
     */
    public java.util.concurrent.CompletableFuture<Void> saveState() {
        List<java.util.concurrent.CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<UUID, Loja> e : lojas.entrySet()) {
            try {
                java.util.List<DatabaseManager.ItemRecord> list = new java.util.ArrayList<>();
                for (int i = 0; i < e.getValue().getItens().size(); i++) {
                    org.bukkit.inventory.ItemStack it = e.getValue().getItens().get(i);
                    if (it == null || it.getType() == org.bukkit.Material.AIR) continue;
                    list.add(new DatabaseManager.ItemRecord(i, it.getType().name(), it.getAmount()));
                }
                futures.add(database.saveLojaItemsAsync(e.getKey(), list)
                        .exceptionally(ex -> { plugin.getLogger().warning("Erro salvando loja em DB: " + ex.getMessage()); return null; }));
            } catch (Exception ex) {
                plugin.getLogger().warning("Erro serializando loja para salvar: " + ex.getMessage());
            }
        }
        return java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]));
    }

    @EventHandler
    public void onClickInventario(InventoryClickEvent e) {
        if (e.getView().getTitle().startsWith(messages.get("loja.window_title_prefix"))) {
            e.setCancelled(true);
            // Aqui colocar lógica para comprar do servidor ou do jogador
            // Simplificado, exemplo para comprar do servidor com preço tabelado
            ItemStack item = e.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;
            Player p = (Player) e.getWhoClicked();
            Material mat = item.getType();
            if (!precoTabelado.containsKey(mat)) {
                p.sendMessage(messages.get("loja.item_not_for_sale"));
                return;
            }
            int preco = precoTabelado.get(mat);
            // Aqui você implementaria sua economia para retirar moedas e dar o item
            String display = messages.getMaterialName(mat.name());
            p.sendMessage(messages.get("loja.purchased", display, preco));
        }
    }

    // Classe interna para representar a loja
    private static class Loja {
        private final String dono;
        private final List<ItemStack> itens = new ArrayList<>();

        public Loja(String dono) {
            this.dono = dono;
        }

        public String getDono() {
            return dono;
        }

        public List<ItemStack> getItens() {
            return itens;
        }
    }
}