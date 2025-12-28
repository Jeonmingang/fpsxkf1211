package com.minpack.rental.gui;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiSession {
    private static final Map<UUID, Session> map = new ConcurrentHashMap<>();
    public record Session(String key, int page) {}

    public static void set(Player p, String key, int page) { map.put(p.getUniqueId(), new Session(key, page)); }
    public static Session get(Player p) { return map.get(p.getUniqueId()); }
    public static void clear(Player p) { map.remove(p.getUniqueId()); }
}
