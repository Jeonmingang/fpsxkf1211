package com.minpack.rental.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Msg {
    private static JavaPlugin plugin;
    private static String prefix;

    private Msg() {}

    public static void init(JavaPlugin pl) { plugin = pl; reload(); }

    public static void reload() {
        FileConfiguration c = plugin.getConfig();
        prefix = color(c.getString("messages.prefix", "&7[&b렌탈&7] "));
    }

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static void send(CommandSender sender, String key) {
        String raw = plugin.getConfig().getString("messages." + key, "");
        if (raw == null || raw.isEmpty()) return;
        sender.sendMessage(prefix + color(raw));
    }

    public static String fmt(String key, String... kv) {
        String s = plugin.getConfig().getString("messages." + key, "");
        if (s == null) s = "";
        for (int i=0;i+1<kv.length;i+=2) s = s.replace("{"+kv[i]+"}", kv[i+1]);
        return prefix + color(s);
    }
}
