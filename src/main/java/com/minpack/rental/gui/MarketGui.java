package com.minpack.rental.gui;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.minpack.rental.bridge.PokemonNbt;
import com.minpack.rental.data.Listing;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MarketGui {
    public static final String KEY = "rental_market";
    private final PixelmonRentalMarketPlugin plugin;

    public MarketGui(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p, int page) {
        Inventory inv = GuiUtil.create(54, plugin.getConfig().getString("market.title", "&b&l렌탈 거래소") + " &7#" + (page + 1));
        int pageSize = Math.min(45, plugin.getConfig().getInt("market.page_size", 45));
        List<Listing> list = plugin.getRentalService().getListingsPage(page, pageSize);

        int slot = 0;
        for (Listing l : list) {
            ItemStack it;
            List<String> spriteLore = new ArrayList<>();

            // 1. NBT 데이터로 포켓몬 복구 및 스프라이트 생성
            try {
                Object pObj = PokemonNbt.fromBase64(l.pokemonNbtB64);
                // 새로 만든 메서드 호출!
                it = plugin.getPixelmon().getPokemonSpriteWithLore(pObj);
                
                // 스프라이트에 붙어있는 기본 Lore(레벨, 성격 등)를 저장
                if (it.hasItemMeta() && it.getItemMeta().hasLore()) {
                    spriteLore.addAll(it.getItemMeta().getLore());
                }
            } catch (Exception e) {
                it = new ItemStack(Material.ENDER_PEARL);
                spriteLore.add("&c이미지 로드 실패");
            }

            // 2. 렌탈 정보 덧붙이기
            List<String> finalLore = new ArrayList<>();
            finalLore.add("&7ID: &e" + l.id);
            finalLore.add("&7주인: &f" + l.ownerName);
            finalLore.add("&7가격: &a" + l.price);
            finalLore.add("&7대여시간: &b" + l.rentalDurationSeconds + "초");
            finalLore.add("");
            
            // 포켓몬 스탯 Lore 추가
            finalLore.addAll(spriteLore);
            
            finalLore.add("");
            finalLore.add("&a클릭: 구매");

            // 3. 최종 적용
            GuiUtil.nameLore(it, "&b렌탈 포켓몬 &7(#" + l.id + ")", finalLore);
            inv.setItem(slot++, it);
        }

        inv.setItem(45, GuiUtil.nameLore(new ItemStack(Material.ARROW), "&e이전 페이지", List.of("&7클릭")));
        inv.setItem(49, GuiUtil.nameLore(new ItemStack(Material.BOOK), "&6내 렌탈 목록", List.of("&7클릭")));
        inv.setItem(53, GuiUtil.nameLore(new ItemStack(Material.ARROW), "&e다음 페이지", List.of("&7클릭")));

        GuiSession.set(p, KEY, page);
        p.openInventory(inv);
    }
}
