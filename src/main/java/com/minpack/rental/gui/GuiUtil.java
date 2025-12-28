package com.minpack.rental.gui;

import com.minpack.rental.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class GuiUtil {
    private GuiUtil() {}

    public static Inventory create(int size, String title) {
        return Bukkit.createInventory(null, size, Msg.color(title));
    }

    public static ItemStack nameLore(ItemStack it, String name, List<String> lore) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Msg.color(name));
            if (lore != null) meta.setLore(lore.stream().map(Msg::color).toList());
            it.setItemMeta(meta);
        }
        return it;
    }
}
