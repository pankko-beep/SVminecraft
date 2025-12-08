package br.com.nexus.commands;

import br.com.nexus.NexusPlugin;
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

public class TransacoesCommand implements CommandExecutor {
    private final NexusPlugin plugin;
    public TransacoesCommand(NexusPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("economia.admin")) { sender.sendMessage(msg("sem-permissao")); return true; }
        if (args.length == 0) { sender.sendMessage(prefix()+"Uso: /_transacoes listar [minutos] [limite] [jogador] [nota-contendo] | /_transacoes export <csv|json> [minutos] [limite] [nota-contendo]"); return true; }
        String sub = args[0].toLowerCase();
        if (sub.equals("listar")) {
            Integer minutos = args.length >= 2 ? parseInt(args[1]) : 60;
            Integer limite = args.length >= 3 ? parseInt(args[2]) : 50;
            UUID alvo = null; String noteLike = null;
            if (args.length >= 4) { OfflinePlayer op = Bukkit.getOfflinePlayer(args[3]); alvo = op.getUniqueId(); }
            if (args.length >= 5) { noteLike = args[4]; }
            listar(sender, minutos, limite, alvo, noteLike);
            return true;
        }
        if (sub.equals("export")) {
            if (args.length < 2) { sender.sendMessage(prefix()+"Uso: /_transacoes export <csv|json> [minutos] [limite]"); return true; }
            String formato = args[1].toLowerCase();
            Integer minutos = args.length >= 3 ? parseInt(args[2]) : 1440;
            Integer limite = args.length >= 4 ? parseInt(args[3]) : 1000;
            String noteLike = args.length >= 5 ? args[4] : null;
            export(sender, formato, minutos, limite, noteLike);
            return true;
        }
        sender.sendMessage(prefix()+"Subcomando inválido.");
        return true;
    }

    private void listar(CommandSender sender, Integer minutos, Integer limite, UUID alvo, String noteLike) {
        long after = System.currentTimeMillis() - (minutos==null?0:minutos*60_000L);
        String sql = "SELECT ts, id, from_uuid, to_uuid, amount, note FROM transactions WHERE ts>=?" + (alvo!=null?" AND (from_uuid=? OR to_uuid=?)":"") + (noteLike!=null?" AND note LIKE ?":"") + " ORDER BY ts DESC LIMIT ?";
        try (Connection c = plugin.database().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i=1; ps.setLong(i++, after);
            if (alvo != null) { ps.setString(i++, alvo.toString()); ps.setString(i++, alvo.toString()); }
            if (noteLike != null) { ps.setString(i++, "%"+noteLike+"%"); }
            ps.setInt(i, limite==null?50:limite);
            try (ResultSet rs = ps.executeQuery()) {
                sender.sendMessage(prefix()+"Transações:");
                while (rs.next()) {
                    long ts = rs.getLong(1); String id = rs.getString(2); String from = rs.getString(3); String to = rs.getString(4);
                    double amount = rs.getDouble(5); String note = rs.getString(6);
                    sender.sendMessage("§7[%tF %tT] §f%s §7%s->%s §e%.2f §7(%s)".formatted(ts, ts, id, shortUuid(from), shortUuid(to), amount, note));
                }
            }
        } catch (Exception e) { sender.sendMessage(prefix()+"§cFalha ao consultar: "+e.getMessage()); }
    }

    private void export(CommandSender sender, String formato, Integer minutos, Integer limite, String noteLike) {
        long after = System.currentTimeMillis() - (minutos==null?0:minutos*60_000L);
        String sql = "SELECT ts, id, from_uuid, to_uuid, amount, note FROM transactions WHERE ts>=?" + (noteLike!=null?" AND note LIKE ?":"") + " ORDER BY ts DESC LIMIT ?";
        File outDir = new File(plugin.getDataFolder(), "exports"); if (!outDir.exists()) outDir.mkdirs();
        File outFile = new File(outDir, "transactions_%d.%s".formatted(System.currentTimeMillis(), formato.equals("csv") ? "csv" : "json"));
        try (Connection c = plugin.database().getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i=1; ps.setLong(i++, after);
            if (noteLike != null) ps.setString(i++, "%"+noteLike+"%");
            ps.setInt(i, limite==null?1000:limite);
            try (ResultSet rs = ps.executeQuery()) {
                if ("csv".equalsIgnoreCase(formato)) {
                    try (FileWriter fw = new FileWriter(outFile, StandardCharsets.UTF_8)) {
                        fw.write("ts,id,from_uuid,to_uuid,amount,note\n");
                        while (rs.next()) {
                            fw.write(String.format(Locale.ROOT, "%d,%s,%s,%s,%.2f,%s\n",
                                    rs.getLong(1), safe(rs.getString(2)), safe(rs.getString(3)), safe(rs.getString(4)), rs.getDouble(5), quote(rs.getString(6))));
                        }
                    }
                } else {
                    java.util.List<java.util.Map<String,Object>> list = new java.util.ArrayList<>();
                    while (rs.next()) {
                        java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
                        m.put("ts", rs.getLong(1)); m.put("id", rs.getString(2)); m.put("from_uuid", rs.getString(3)); m.put("to_uuid", rs.getString(4));
                        m.put("amount", rs.getDouble(5)); m.put("note", rs.getString(6)); list.add(m);
                    }
                    String json = new com.google.gson.Gson().toJson(list);
                    try (FileWriter fw = new FileWriter(outFile, StandardCharsets.UTF_8)) { fw.write(json); }
                }
            }
        } catch (Exception e) { sender.sendMessage(prefix()+"§cFalha ao exportar: "+e.getMessage()); return; }
        sender.sendMessage(prefix()+"Exportado para: "+ outFile.getName());
    }

    private static String shortUuid(String s) { if (s==null) return "?"; return s.substring(0,8); }
    private static Integer parseInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return null; } }
    private static String safe(String s) { return s==null?"": s.replace(","," ").replace("\n"," ").replace("\r"," "); }
    private static String quote(String s) { if (s==null) return ""; String q=s.replace("\"","' "); return '"'+q+'"'; }
    private String prefix() { return plugin.getConfig().getString("mensagens.prefixo", ""); }
    private String msg(String k) { return plugin.getConfig().getString("mensagens."+k, k); }

    public static class Tab implements TabCompleter {
        private final NexusPlugin plugin;
        public Tab(NexusPlugin plugin) { this.plugin = plugin; }
        @Override
        public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) return java.util.Arrays.asList("listar","export");
            if (args.length == 2 && args[0].equalsIgnoreCase("listar")) return java.util.Arrays.asList("60","1440");
            if (args.length == 3 && args[0].equalsIgnoreCase("listar")) return java.util.Arrays.asList("50","200","1000");
            if (args.length == 4 && args[0].equalsIgnoreCase("listar")) return Bukkit.getOnlinePlayers().stream().map(p->p.getName()).sorted().collect(Collectors.toList());
            if (args.length == 2 && args[0].equalsIgnoreCase("export")) return java.util.Arrays.asList("csv","json");
            if (args.length == 3 && args[0].equalsIgnoreCase("export")) return java.util.Arrays.asList("60","1440");
            if (args.length == 4 && args[0].equalsIgnoreCase("export")) return java.util.Arrays.asList("200","1000");
            return java.util.Collections.emptyList();
        }
    }
}
