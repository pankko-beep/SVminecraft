package br.com.nexus.commands;

import br.com.nexus.NexusPlugin;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class AuditoriaCommand implements CommandExecutor {
    private final NexusPlugin plugin;
    private static final List<String> KNOWN_TYPES = Arrays.asList(
            "player.join","player.quit",
            "economia.transfer","economia.freeze",
            "time.choose","time.swap",
            "guild.create","guild.invite","guild.accept","guild.leave",
            "panel.create","panel.delete"
    );

    public AuditoriaCommand(NexusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("auditoria.admin")) { sender.sendMessage(msg("sem-permissao")); return true; }
        if (args.length == 0) {
            sender.sendMessage(prefix()+"Uso: /auditoria listar [tipo] [minutos] [limite] [jogador] | /auditoria export <csv|json> [tipo] [minutos] [limite]");
            return true;
        }
        String sub = args[0].toLowerCase();
        if (sub.equals("listar")) {
            String tipo = args.length >= 2 ? nullIfAny(args[1]) : null;
            Integer minutos = args.length >= 3 ? parseInt(args[2]) : 60; // último 1h
            Integer limite = args.length >= 4 ? parseInt(args[3]) : 50;
            UUID alvo = null;
            if (args.length >= 5) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[4]);
                alvo = op.getUniqueId();
            }
            listar(sender, tipo, minutos, limite, alvo);
            return true;
        }
        if (sub.equals("export")) {
            if (args.length < 2) { sender.sendMessage(prefix()+"Uso: /auditoria export <csv|json> [tipo] [minutos] [limite]"); return true; }
            String formato = args[1].toLowerCase();
            String tipo = args.length >= 3 ? nullIfAny(args[2]) : null;
            Integer minutos = args.length >= 4 ? parseInt(args[3]) : 60;
            Integer limite = args.length >= 5 ? parseInt(args[4]) : 500;
            export(sender, formato, tipo, minutos, limite);
            return true;
        }
        sender.sendMessage(prefix()+"Subcomando inválido.");
        return true;
    }

    private void listar(CommandSender sender, String tipo, Integer minutos, Integer limite, UUID actor) {
        long after = System.currentTimeMillis() - (minutos == null ? 0 : minutos * 60_000L);
        String sql = "SELECT ts,type,actor_uuid,actor_name,target_type,target_id,data_json FROM audit_events WHERE ts>=?" + (tipo!=null?" AND type=?":"") + (actor!=null?" AND actor_uuid=?":"") + " ORDER BY ts DESC LIMIT ?";
        try (Connection c = plugin.database().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i=1; ps.setLong(i++, after);
            if (tipo != null) ps.setString(i++, tipo);
            if (actor != null) ps.setString(i++, actor.toString());
            ps.setInt(i, limite==null?50:limite);
            try (ResultSet rs = ps.executeQuery()) {
                sender.sendMessage(prefix()+"Eventos (mais recentes):");
                while (rs.next()) {
                    long ts = rs.getLong(1);
                    String t = rs.getString(2);
                    String au = rs.getString(3);
                    String an = rs.getString(4);
                    String tt = rs.getString(5);
                    String tid = rs.getString(6);
                    String dj = rs.getString(7);
                    sender.sendMessage(String.format("§7[%tF %tT] §b%s§7 actor=%s(%s) target=%s:%s data=%s", ts, ts, t, an, au, tt, tid, summarize(dj)));
                }
            }
        } catch (Exception e) {
            sender.sendMessage(prefix()+"§cFalha ao consultar: "+e.getMessage());
        }
    }

    private void export(CommandSender sender, String formato, String tipo, Integer minutos, Integer limite) {
        long after = System.currentTimeMillis() - (minutos == null ? 0 : minutos * 60_000L);
        String sql = "SELECT ts,type,actor_uuid,actor_name,target_type,target_id,data_json FROM audit_events WHERE ts>=?" + (tipo!=null?" AND type=?":"") + " ORDER BY ts DESC LIMIT ?";
        File outDir = new File(plugin.getDataFolder(), "exports"); if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, String.format("audit_%s_%d.%s", tipo==null?"all":tipo.replace(':','_'), System.currentTimeMillis(), formato.equals("csv")?"csv":"json"));
        try (Connection c = plugin.database().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i=1; ps.setLong(i++, after);
            if (tipo != null) ps.setString(i++, tipo);
            ps.setInt(i, limite==null?500:limite);
            try (ResultSet rs = ps.executeQuery()) {
                if ("csv".equalsIgnoreCase(formato)) {
                    try (FileWriter fw = new FileWriter(outFile, StandardCharsets.UTF_8)) {
                        fw.write("ts,type,actor_uuid,actor_name,target_type,target_id,data_json\n");
                        while (rs.next()) {
                            fw.write(String.format(Locale.ROOT, "%d,%s,%s,%s,%s,%s,%s\n",
                                    rs.getLong(1), safe(rs.getString(2)), safe(rs.getString(3)), safe(rs.getString(4)), safe(rs.getString(5)), safe(rs.getString(6)), quote(rs.getString(7))));
                        }
                    }
                } else {
                    List<Map<String,Object>> list = new ArrayList<>();
                    while (rs.next()) {
                        Map<String,Object> m = new LinkedHashMap<>();
                        m.put("ts", rs.getLong(1));
                        m.put("type", rs.getString(2));
                        m.put("actor_uuid", rs.getString(3));
                        m.put("actor_name", rs.getString(4));
                        m.put("target_type", rs.getString(5));
                        m.put("target_id", rs.getString(6));
                        m.put("data_json", rs.getString(7));
                        list.add(m);
                    }
                    String json = new Gson().toJson(list);
                    try (FileWriter fw = new FileWriter(outFile, StandardCharsets.UTF_8)) { fw.write(json); }
                }
            }
        } catch (Exception e) {
            sender.sendMessage(prefix()+"§cFalha ao exportar: "+e.getMessage()); return;
        }
        sender.sendMessage(prefix()+"Exportado para: "+ outFile.getName());
    }

    private static String summarize(String json) { if (json == null) return "{}"; String s = json; if (s.length()>60) s = s.substring(0,60)+"..."; return s; }
    private static String nullIfAny(String v) { if (v==null) return null; if (v.equalsIgnoreCase("any")||v.equals("*")) return null; return v; }
    private static Integer parseInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return null; } }
    private static String safe(String s) { return s==null?"": s.replace(","," ").replace("\n"," ").replace("\r"," "); }
    private static String quote(String s) { if (s==null) return ""; String q=s.replace("\"","' "); return '"'+q+'"'; }

    private String prefix() { return plugin.getConfig().getString("mensagens.prefixo", ""); }
    private String msg(String k) { return plugin.getConfig().getString("mensagens."+k, k); }

    public static class Tab implements TabCompleter {
        private final NexusPlugin plugin;
        public Tab(NexusPlugin plugin) { this.plugin = plugin; }
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) return Arrays.asList("listar","export");
            if (args.length == 2 && args[0].equalsIgnoreCase("listar")) return withAny(KNOWN_TYPES, args[1]);
            if (args.length == 3 && args[0].equalsIgnoreCase("listar")) return Arrays.asList("15","60","1440");
            if (args.length == 4 && args[0].equalsIgnoreCase("listar")) return Arrays.asList("20","50","200");
            if (args.length == 5 && args[0].equalsIgnoreCase("listar")) return Bukkit.getOnlinePlayers().stream().map(p->p.getName()).sorted().collect(Collectors.toList());
            if (args.length == 2 && args[0].equalsIgnoreCase("export")) return Arrays.asList("csv","json");
            if (args.length == 3 && args[0].equalsIgnoreCase("export")) return withAny(KNOWN_TYPES, args[2]);
            if (args.length == 4 && args[0].equalsIgnoreCase("export")) return Arrays.asList("60","1440");
            if (args.length == 5 && args[0].equalsIgnoreCase("export")) return Arrays.asList("200","1000");
            return Collections.emptyList();
        }
        private static List<String> withAny(List<String> src, String prefix) {
            List<String> all = new ArrayList<>(); all.add("any"); all.addAll(src);
            return all.stream().filter(s->s.toLowerCase().startsWith(prefix==null?"":prefix.toLowerCase())).collect(Collectors.toList());
        }
    }
}
