package com.minpack.rental.gui;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.minpack.rental.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public final class GuiListener implements Listener {
    private final PixelmonRentalMarketPlugin plugin;
    public GuiListener(PixelmonRentalMarketPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        GuiSession.Session s = GuiSession.get(p);
        if (s == null) return;

        e.setCancelled(true);

        ItemStack it = e.getCurrentItem();
        if (it == null || it.getType() == Material.AIR) return;

        try {
            if (MarketGui.KEY.equals(s.key())) {
                int page = s.page();
                if (e.getRawSlot() == 45) { new MarketGui(plugin).open(p, Math.max(0, page-1)); return; }
                if (e.getRawSlot() == 49) { new MyListingsGui(plugin).open(p); return; }
                if (e.getRawSlot() == 53) { new MarketGui(plugin).open(p, page+1); return; }

                long id = parseId(it);
                if (id > 0) {
                    boolean ok = plugin.getRentalService().buy(p, id);
                    if (ok) p.sendMessage(Msg.fmt("bought", "DUR", "0"));
                    new MarketGui(plugin).open(p, page);
                }
                return;
            }

            if (MyListingsGui.KEY.equals(s.key())) {
                if (e.getRawSlot() == 49) { p.closeInventory(); return; }
                long id = parseId(it);
                if (id > 0) {
                    boolean ok = plugin.getRentalService().cancelListing(p, id);
                    if (ok) p.sendMessage(Msg.fmt("cancelled","ID",String.valueOf(id)));
                    new MyListingsGui(plugin).open(p);
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("GUI error: " + ex.getMessage());
        }
    }

    private long parseId(ItemStack it) {
        if (it.getItemMeta() == null || it.getItemMeta().getLore() == null) return -1;
        for (String line : it.getItemMeta().getLore()) {
            String c = org.bukkit.ChatColor.stripColor(line);
            if (c == null) continue;
            c = c.trim();
            if (c.startsWith("ID:")) {
                String num = c.substring(3).trim();
                try { return Long.parseLong(num); } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) GuiSession.clear(p);
    }
}
