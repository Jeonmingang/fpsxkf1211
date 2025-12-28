package com.minpack.rental.discord;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import org.bukkit.Bukkit;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class DiscordWebhook {
    private final PixelmonRentalMarketPlugin plugin;
    public DiscordWebhook(PixelmonRentalMarketPlugin plugin) { this.plugin = plugin; }

    private String webhook() { return plugin.getConfig().getString("discord.webhook_url", ""); }
    private boolean enabled() { String u = webhook(); return u != null && !u.isBlank(); }

    public void sendAsync(String content) {
        if (!enabled()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL u = new URL(webhook());
                HttpURLConnection con = (HttpURLConnection) u.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                String payload = "{\"content\":" + jsonEscape(content) + "}";
                byte[] data = payload.getBytes(StandardCharsets.UTF_8);
                con.setFixedLengthStreamingMode(data.length);
                try (OutputStream os = con.getOutputStream()) { os.write(data); }
                con.getInputStream().close();
            } catch (Exception ignored) {}
        });
    }

    private String jsonEscape(String s) {
        String esc = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "\"" + esc + "\"";
    }
}
