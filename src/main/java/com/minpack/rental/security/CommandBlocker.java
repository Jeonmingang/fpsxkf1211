package com.minpack.rental.security;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.minpack.rental.util.Msg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Locale;

public final class CommandBlocker implements Listener {

    private final PixelmonRentalMarketPlugin plugin;

    public CommandBlocker(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        if (msg == null || msg.length() < 2) return;
        String cmd = msg.substring(1).trim();
        if (cmd.isEmpty()) return;

        String root = cmd.split("\\s+")[0].toLowerCase(Locale.ROOT);
        List<String> blocked = plugin.getConfig().getStringList("security.blocked_commands");
        if (blocked == null || blocked.isEmpty()) return;

        if (!plugin.getRentalService().hasAnyActiveRental(e.getPlayer().getUniqueId())) return;

        for (String b : blocked) {
            if (b != null && root.equalsIgnoreCase(b.trim())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(Msg.color(plugin.getConfig().getString("messages.prefix","")) + Msg.color(plugin.getConfig().getString("messages.blocked_rental_command","&c렌탈 포켓몬 상태에서는 이 명령어를 사용할 수 없습니다.")));
                return;
            }
        }
    }
}
