package com.servermine;

import com.servermine.Objetivos.ObjetivosPlugin;
import com.servermine.loja.LojaPlugin;
import com.servermine.PainelHud.PainelHUDPlugin;
import com.servermine.RankNivel.RanksNiveisPlugin;
import com.servermine.TimeGuilda.TimesGuildasPlugin;
import com.servermine.VipRecompensa.VipRecompensaPlugin;
import com.servermine.escudobase.EscudoBasePlugin;
import org.bukkit.plugin.java.JavaPlugin;
import com.servermine.storage.DatabaseManager;
import com.servermine.util.Messages;

/**
 * MainPlugin — ponto de entrada do JAR. Registra e coordena os módulos do servidor.
 */
public class MainPlugin extends JavaPlugin {

    private ObjetivosPlugin objetivos;
    private Messages messages;
    private DatabaseManager database;
    private LojaPlugin loja;
    private PainelHUDPlugin painel;
    private RanksNiveisPlugin ranks;
    private TimesGuildasPlugin times;
    private VipRecompensaPlugin vip;
    private EscudoBasePlugin escudo;

    @Override
    public void onEnable() {
        getLogger().info("MainPlugin ativando — inicializando módulos...");

        // garante que config padrão esteja presente antes de inicializar DB
        saveDefaultConfig();

        // inicializa banco de dados SQLite
        database = new DatabaseManager(this);
        try {
            database.init();
            // migrar dados antigos do config para DB (se aplicável)
            database.migrateFromConfig(this);
        } catch (Exception e) {
            getLogger().severe("Falha ao iniciar banco de dados: " + e.getMessage());
            e.printStackTrace();
            // se o DB não inicializar, ainda podemos seguir, porém persistência ficará sem funcionar
        }

        // load messages resource and keep instance
        messages = new Messages(this);

        // Inicializa módulos passando a referência do plugin principal (respeita config.modules)
        if (getConfig().getBoolean("modules.objetivos", true)) objetivos = new ObjetivosPlugin(this, database, messages);
        if (getConfig().getBoolean("modules.loja", true)) loja = new LojaPlugin(this, database, messages);
        if (getConfig().getBoolean("modules.painelhud", true)) painel = new PainelHUDPlugin(this);
        if (getConfig().getBoolean("modules.ranks", true)) ranks = new RanksNiveisPlugin(this, database, messages);
        if (getConfig().getBoolean("modules.times", true)) times = new TimesGuildasPlugin(this, messages);
        if (getConfig().getBoolean("modules.vip", true)) vip = new VipRecompensaPlugin(this, messages);
        if (getConfig().getBoolean("modules.escudobase", true)) escudo = new EscudoBasePlugin(this, messages);

        // Chama os métodos de ativação de cada módulo (se instanciados)
        if (objetivos != null) objetivos.enable();
        if (loja != null) loja.enable();
        if (painel != null) painel.enable();
        if (ranks != null) ranks.enable();
        if (times != null) times.enable();
        if (vip != null) vip.enable();
        if (escudo != null) escudo.enable();

        getLogger().info("MainPlugin ativado — todos os módulos inicializados.");

        // Agendar autosave se habilitado e intervalo > 0
        if (getConfig().getBoolean("autosave.enabled", true)) {
            int interval = Math.max(0, getConfig().getInt("autosave.interval-seconds", 300));
            if (interval > 0) {
                long ticks = interval * 20L;
                org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                    getLogger().info(String.format(getConfig().getString("messages.autosave_started", "Salvamento periódico iniciado (cada %d segundos)."), interval));
                    java.util.List<java.util.concurrent.CompletableFuture<Void>> tasks = new java.util.ArrayList<>();
                    if (objetivos != null) tasks.add(objetivos.saveState());
                    if (loja != null) tasks.add(loja.saveState());
                    if (ranks != null) tasks.add(ranks.saveState());
                    // aguarda conclusão dos saves (assíncrono) e loga
                    java.util.concurrent.CompletableFuture.allOf(tasks.toArray(new java.util.concurrent.CompletableFuture[0]))
                            .whenComplete((r, e) -> {
                                if (e != null) getLogger().warning("Erro durante autosave: " + e.getMessage());
                                else getLogger().info(String.format(getConfig().getString("messages.autosave_completed", "Autosave completo (%d módulos salvos)."), tasks.size()));
                            });
                }, ticks, ticks);
            } else {
                getLogger().info(getConfig().getString("messages.autosave_disabled", "Autosave desativado."));
            }
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("MainPlugin desativando — encerrando módulos...");
        // Desliga módulos ordenadamente (despacha saves e aguarda)
        if (objetivos != null) objetivos.disable();
        if (loja != null) loja.disable();
        if (painel != null) painel.disable();
        if (ranks != null) ranks.disable();
        if (times != null) times.disable();
        if (vip != null) vip.disable();
        if (escudo != null) escudo.disable();

        // garante que todo módulo com saveState seja aguardado antes de fechar o DB
        java.util.List<java.util.concurrent.CompletableFuture<Void>> saves = new java.util.ArrayList<>();
        if (objetivos != null) saves.add(objetivos.saveState());
        if (loja != null) saves.add(loja.saveState());
        if (ranks != null) saves.add(ranks.saveState());

        try {
            if (!saves.isEmpty()) java.util.concurrent.CompletableFuture.allOf(saves.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .get(getConfig().getInt("performance.shutdown-wait-seconds", 8), java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ex) {
            getLogger().warning("Timeout ou erro aguardando saves em shutdown: " + ex.getMessage());
        }

        // fechar DB
        try { if (database != null) database.shutdownExecutor(); } catch (Exception ignore) {}
        try { if (database != null) database.close(); } catch (Exception ignore) {}
        getLogger().info("MainPlugin desativado.");
    }
}
