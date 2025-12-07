package br.com.nexus.services;

import br.com.nexus.NexusPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class EconomyService {
    private final NexusPlugin plugin;
    private Economy economy;
    private final Set<UUID> frozen = new HashSet<>();

    public EconomyService(NexusPlugin plugin) {
        this.plugin = plugin;
        setupVault();
    }

    private void setupVault() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault não encontrado. Economia desativada.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Provedor de economia Vault não encontrado.");
            return;
        }
        this.economy = rsp.getProvider();
        plugin.getLogger().info("Economia via Vault conectada: " + economy.getName());
    }

    public boolean isReady() { return economy != null; }

    public double getBalance(OfflinePlayer player) {
        if (!isReady()) return 0.0;
        return economy.getBalance(player);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isReady()) return false;
        if (isFrozen(player.getUniqueId())) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isReady()) return false;
        if (isFrozen(player.getUniqueId())) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public void setFrozen(UUID uuid, boolean state) {
        if (state) frozen.add(uuid); else frozen.remove(uuid);
    }

    public boolean isFrozen(UUID uuid) { return frozen.contains(uuid); }
}
