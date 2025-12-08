package br.com.nexus;

import br.com.nexus.commands.*;
import br.com.nexus.services.*;
import br.com.nexus.panels.PanelService;
import br.com.nexus.listeners.PlayerLifecycleListener;
import br.com.nexus.listeners.SimpleLoginHook;
import br.com.nexus.services.AuditService;
import br.com.nexus.services.DatabaseService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class NexusPlugin extends JavaPlugin {

    private EconomyService economyService;
    private TeamService teamService;
    private GuildService guildService;
    private PanelService panelService;
    private TransactionService transactionService;
    private PlayerDataService playerDataService;
    private DatabaseService databaseService;
    private AuditService auditService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.transactionService = new TransactionService(this);
        this.economyService = new EconomyService(this);
        this.teamService = new TeamService(this);
        this.guildService = new GuildService(this);
        this.panelService = new PanelService(this);
        this.playerDataService = new PlayerDataService(this);
        this.databaseService = new DatabaseService(this);
        this.auditService = new AuditService(this, this.databaseService);

        // Flags de módulos
        boolean mEconomia = getConfig().getBoolean("modulos.economia", true);
        boolean mTimes = getConfig().getBoolean("modulos.times", true);
        boolean mGuildas = getConfig().getBoolean("modulos.guildas", true);
        boolean mPaineis = getConfig().getBoolean("modulos.paineis", true);
        boolean mAuditoria = getConfig().getBoolean("modulos.auditoria", true);
        boolean mTransacoes = getConfig().getBoolean("modulos.transacoes", true);
        boolean mLogin = getConfig().getBoolean("modulos.login", true);

        // Registrar comandos por módulo
        if (mEconomia) {
            register("saldo", new SaldoCommand(this), new SaldoCommand.Tab(this));
            register("pagar", new PagarCommand(this), new PagarCommand.Tab(this));
            register("historico", new HistoricoCommand(this), new HistoricoCommand.Tab(this));
            register("econ", new EconCommand(this), new EconCommand.Tab(this));
        }
        if (mTimes) register("time", new TimeCommand(this), new TimeCommand.Tab(this));
        if (mGuildas) register("guild", new GuildCommand(this), new GuildCommand.Tab(this));
        register("fly", new FlyCommand(), new FlyCommand.Tab());
        if (mPaineis) register("painel", new PainelCommand(this), new PainelCommand.Tab(this));
        if (mAuditoria) register("auditoria", new AuditoriaCommand(this), new AuditoriaCommand.Tab(this));
        if (mTransacoes) register("_transacoes", new TransacoesCommand(this), new TransacoesCommand.Tab(this));

        // Registrar listeners
        if (mLogin) getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this), this);
        if (mTimes) getServer().getPluginManager().registerEvents(new br.com.nexus.listeners.NoTeamMovementListener(this), this);
        // Hook SimpleLogin (registra-se dinamicamente no construtor)
        if (mLogin && getServer().getPluginManager().isPluginEnabled("SimpleLogin")) {
            new SimpleLoginHook(this);
        }
        // Hook AuthMe (opcional, via reflexão)
        if (mLogin) {
            try {
                Class.forName("fr.xephi.authme.events.LoginEvent");
                new br.com.nexus.listeners.AuthMeHook(this);
            } catch (ClassNotFoundException ignored) {
                getLogger().info("AuthMe não encontrado; hook não ativado.");
            }
        }

        // Agendar refresh de painéis (se módulo ativo)
        if (mPaineis) {
            int refresh = getConfig().getInt("painel.refresh-segundos", 30);
            getServer().getScheduler().runTaskTimer(this, () -> {
                try { panelService.refreshAll(); } catch (Exception ignored) {}
            }, refresh * 20L, refresh * 20L);
        }

        getLogger().info("Nexus habilitado.");
    }

    private void register(String name, org.bukkit.command.CommandExecutor exec, org.bukkit.command.TabCompleter tab) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(exec);
            cmd.setTabCompleter(tab);
        } else {
            getLogger().warning("Comando não encontrado em plugin.yml: " + name);
        }
    }

    @Override
    public void onDisable() {
        try {
            this.transactionService.saveAsyncNow();
            this.teamService.saveNow();
            this.guildService.saveNow();
            this.panelService.saveNow();
            this.playerDataService.saveNow();
        } catch (Exception ignored) {}
        if (this.databaseService != null) this.databaseService.close();
        getLogger().info("Nexus desabilitado.");
    }

    public EconomyService economy() { return economyService; }
    public TeamService teams() { return teamService; }
    public GuildService guilds() { return guildService; }
    public PanelService panels() { return panelService; }
    public TransactionService transactions() { return transactionService; }
    public PlayerDataService players() { return playerDataService; }
    public DatabaseService database() { return databaseService; }
    public AuditService audit() { return auditService; }
}
