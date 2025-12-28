package com.minpack.rental.gui;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.minpack.rental.data.Listing;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MyListingsGui {
    public static final String KEY = "rental_my";
    private final PixelmonRentalMarketPlugin plugin;
    public MyListingsGui(PixelmonRentalMarketPlugin plugin) { this.plugin = plugin; }

    public void open(Player p) {
        Inventory inv = GuiUtil.create(54, plugin.getConfig().getString("market.my_title","&e&l내 렌탈 목록"));
        int slot = 0;
        for (Listing l : plugin.getRentalService().getOwnerListings(p.getUniqueId())) {
            ItemStack it = new ItemStack(Material.NAME_TAG);
            List<String> lore = new ArrayList<>();
            lore.add("&7ID: &e" + l.id);
            lore.add("&7가격: &a" + l.price);
            lore.add("&7대여시간: &b" + l.rentalDurationSeconds + "초");
            lore.add("");
            lore.add("&c클릭: 등록 취소");
            GuiUtil.nameLore(it, "&e내 등록 &7(#"+l.id+")", lore);
            inv.setItem(slot++, it);
            if (slot >= 45) break;
        }
        inv.setItem(49, GuiUtil.nameLore(new ItemStack(Material.BARRIER), "&c닫기", List.of("&7클릭")));
        GuiSession.set(p, KEY, 0);
        p.openInventory(inv);
    }
}
