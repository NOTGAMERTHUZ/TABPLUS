package dev.tabplus;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TabPlus extends JavaPlugin implements Listener {

    private record Group(String id, String permission, int priority, String prefix, String nameColor, String display) {}

    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final String TEAM_PREFIX = "tp";

    private final Set<UUID> hidden = new HashSet<>();
    private final List<Group> groups = new ArrayList<>();
    private Group defaultGroup;
    private List<String> titles, header, footer;
    private String suffix, serverName, timeFormat;
    private BukkitTask task;
    private int frame = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        load();
        getLogger().info("TabPlus enabled.");
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
        for (Player p : Bukkit.getOnlinePlayers()) {
            reset(p);
            removeTeam(p);
        }
    }

    // ───────────── Setup ─────────────
    private void load() {
        reloadConfig();
        if (task != null) task.cancel();

        titles = getConfig().getStringList("animated-title");
        if (titles.isEmpty()) titles = List.of("");
        header = getConfig().getStringList("header");
        footer = getConfig().getStringList("footer");
        suffix = getConfig().getString("player-format.suffix", "");
        serverName = getConfig().getString("server-name", "");
        timeFormat = getConfig().getString("time-format", "hh:mm a");

        groups.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("groups");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection g = sec.getConfigurationSection(id);
                if (g == null) continue;
                groups.add(new Group(id, g.getString("permission", "tabplus.group." + id),
                        g.getInt("priority", 50), g.getString("prefix", ""),
                        g.getString("name-color", "&f"), id));
            }
        }
        groups.sort(Comparator.comparingInt(Group::priority));
        ConfigurationSection d = getConfig().getConfigurationSection("default-group");
        defaultGroup = new Group("default", "", d == null ? 99 : d.getInt("priority", 99),
                d == null ? "&7" : d.getString("prefix", "&7"),
                d == null ? "&7" : d.getString("name-color", "&7"),
                d == null ? "Member" : d.getString("display", "Member"));

        long interval = Math.max(1, getConfig().getLong("update-interval-ticks", 10));
        task = Bukkit.getScheduler().runTaskTimer(this, this::tick, 20L, interval);
    }

    // ───────────── Main loop ─────────────
    private void tick() {
        frame++;
        String title = titles.get(frame % titles.size());
        String tps = tps();
        String time = new SimpleDateFormat(timeFormat).format(new Date());

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (hidden.contains(p.getUniqueId())) continue;
            Group g = groupOf(p);

            p.setPlayerListHeaderFooter(
                    render(header, p, g, title, tps, time),
                    render(footer, p, g, title, tps, time));

            String name = color(g.prefix() + g.nameColor()) + p.getName()
                    + color(replace(suffix, p, g, title, tps, time));
            p.setPlayerListName(name);
            applyTeam(p, g);
        }
    }

    private String render(List<String> lines, Player p, Group g, String title, String tps, String time) {
        StringJoiner j = new StringJoiner("\n");
        for (String l : lines) j.add(color(replace(l, p, g, title, tps, time)));
        return j.toString();
    }

    private String replace(String s, Player p, Group g, String title, String tps, String time) {
        int ping = ping(p);
        return s.replace("%title%", title)
                .replace("%player%", p.getName())
                .replace("%displayname%", p.getDisplayName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%ping%", String.valueOf(ping))
                .replace("%ping_color%", pingColor(ping))
                .replace("%tps%", tps)
                .replace("%world%", p.getWorld().getName())
                .replace("%group%", g.display())
                .replace("%time%", time)
                .replace("%server%", serverName);
    }

    // ───────────── Sorting (via scoreboard teams) ─────────────
    private String teamName(Player p, Group g) {
        return String.format("%s%02d%s", TEAM_PREFIX, Math.min(99, g.priority()),
                p.getUniqueId().toString().substring(0, 8));
    }

    private void applyTeam(Player p, Group g) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        String wanted = teamName(p, g);
        Team current = board.getEntryTeam(p.getName());
        if (current != null && current.getName().equals(wanted)) return;
        removeTeam(p);
        Team t = board.getTeam(wanted);
        if (t == null) t = board.registerNewTeam(wanted);
        t.addEntry(p.getName());
    }

    private void removeTeam(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = board.getEntryTeam(p.getName());
        if (t != null && t.getName().startsWith(TEAM_PREFIX)) {
            t.removeEntry(p.getName());
            if (t.getEntries().isEmpty()) t.unregister();
        }
    }

    private Group groupOf(Player p) {
        for (Group g : groups) if (p.hasPermission(g.permission())) return g;
        return defaultGroup;
    }

    private void reset(Player p) {
        p.setPlayerListHeaderFooter("", "");
        p.setPlayerListName(null);
    }

    // ───────────── Helpers ─────────────
    private static int ping(Player p) {
        try { return p.getPing(); } catch (Throwable t) { return 0; }
    }

    private static String pingColor(int ping) {
        if (ping < 50) return "&a";
        if (ping < 100) return "&e";
        if (ping < 200) return "&6";
        return "&c";
    }

    private static String tps() {
        try {
            double v = Math.min(20.0, Bukkit.getTPS()[0]);
            return String.format("%.1f", v);
        } catch (Throwable t) {
            return "20.0";
        }
    }

    static String color(String s) {
        Matcher m = HEX.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(ChatColor.of("#" + m.group(1)).toString()));
        }
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    private void msg(CommandSender s, String key) {
        s.sendMessage(color(getConfig().getString("messages." + key, key)));
    }

    // ───────────── Events ─────────────
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(this, this::tick, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        hidden.remove(e.getPlayer().getUniqueId());
        removeTeam(e.getPlayer());
    }

    // ───────────── Commands ─────────────
    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) { s.sendMessage(color("&7/tabplus &f<reload|toggle>")); return true; }
        switch (a[0].toLowerCase()) {
            case "reload" -> {
                if (!s.hasPermission("tabplus.admin")) { msg(s, "no-permission"); return true; }
                load();
                msg(s, "reloaded");
            }
            case "toggle" -> {
                if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
                if (!p.hasPermission("tabplus.toggle")) { msg(s, "no-permission"); return true; }
                if (hidden.remove(p.getUniqueId())) {
                    msg(p, "toggled-on");
                } else {
                    hidden.add(p.getUniqueId());
                    reset(p);
                    msg(p, "toggled-off");
                }
            }
            default -> s.sendMessage(color("&7/tabplus &f<reload|toggle>"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        return a.length == 1 ? List.of("reload", "toggle") : List.of();
    }
}
