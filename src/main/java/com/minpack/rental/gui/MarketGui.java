package com.minpack.rental.gui;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.minpack.rental.data.Listing;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MarketGui {
    public static final String KEY = "rental_market";
    private final PixelmonRentalMarketPlugin plugin;
    public MarketGui(PixelmonRentalMarketPlugin plugin) { this.plugin = plugin; }

    public void open(Player p, int page) {
        Inventory inv = GuiUtil.create(54, plugin.getConfig().getString("market.title","&b&l렌탈 거래소") + " &7#"+(page+1));
        int pageSize = Math.min(45, plugin.getConfig().getInt("market.page_size", 45));
        List<Listing> list = plugin.getRentalService().getListingsPage(page, pageSize);

        int slot = 0;
        for (Listing l : list) {
            ItemStack it = new ItemStack(Material.ENDER_PEARL);
            List<String> lore = new ArrayList<>();
            lore.add("&7ID: &e" + l.id);
            lore.add("&7주인: &f" + l.ownerName);
            lore.add("&7가격: &a" + l.price);
            lore.add("&7대여시간: &b" + l.rentalDurationSeconds + "초");
            lore.add("");
            lore.add("&a클릭: 구매");
            GuiUtil.nameLore(it, "&b렌탈 포켓몬 &7(#"+l.id+")", lore);
            inv.setItem(slot++, it);
        }

        inv.setItem(45, GuiUtil.nameLore(new ItemStack(Material.ARROW), "&e이전 페이지", List.of("&7클릭")));
        inv.setItem(49, GuiUtil.nameLore(new ItemStack(Material.BOOK), "&6내 렌탈 목록", List.of("&7클릭")));
        inv.setItem(53, GuiUtil.nameLore(new ItemStack(Material.ARROW), "&e다음 페이지", List.of("&7클릭")));

        GuiSession.set(p, KEY, page);
        p.openInventory(inv);
    }
}
