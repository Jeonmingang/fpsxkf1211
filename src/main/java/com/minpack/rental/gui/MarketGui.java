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
            List<String> statsLore = new ArrayList<>();

            // 1. 저장된 NBT 데이터로 포켓몬 객체를 복구하고 스프라이트(사진) 생성 시도
            try {
                Object pObj = PokemonNbt.fromBase64(l.pokemonNbtB64);
                // 앞서 만든 PixelmonBridge의 메서드를 호출하여 사진+스탯 정보를 가져옴
                it = plugin.getPixelmon().getPokemonSpriteWithLore(pObj);

                // 가져온 아이템에 기존 스탯 Lore가 있다면 저장해둠
                if (it != null && it.hasItemMeta() && it.getItemMeta().hasLore()) {
                    statsLore = it.getItemMeta().getLore();
                }
                
                if (it == null) {
                    it = new ItemStack(Material.ENDER_PEARL); // 실패 시 엔더진주로 대체
                }
            } catch (Exception e) {
                // 오류 발생 시 안전하게 엔더진주 표시
                it = new ItemStack(Material.ENDER_PEARL);
                statsLore.add("&c[이미지 로드 실패]");
            }

            // 2. 렌탈 거래 정보와 포켓몬 상세 스탯 합치기
            List<String> finalLore = new ArrayList<>();
            finalLore.add("&7ID: &e" + l.id);
            finalLore.add("&7주인: &f" + l.ownerName);
            finalLore.add("&7가격: &a" + l.price);
            finalLore.add("&7대여시간: &b" + l.rentalDurationSeconds + "초");
            finalLore.add(""); // 공백
            
            // 포켓몬 스탯 정보 추가 (레벨, IV, 기술 등)
            if (statsLore != null) {
                finalLore.addAll(statsLore);
            }
            
            finalLore.add("");
            finalLore.add("&a클릭: 구매");

            // 3. 최종 아이템 적용
            GuiUtil.nameLore(it, "&b렌탈 포켓몬 &7(#" + l.id + ")", finalLore);
            inv.setItem(slot++, it);
        }

        // 네비게이션 버튼 (이전/다음/내목록)
        inv.setItem(45, GuiUtil.nameLore(new ItemStack(Material.ARROW), "&e이전 페이지", List.of("&7클릭")));
        inv.setItem(49, GuiUtil.nameLore(new ItemStack(Material.BOOK), "&6내 렌탈 목록", List.of("&7클릭")));
        inv.setItem(53, GuiUtil.nameLore(new ItemStack(Material.ARROW), "&e다음 페이지", List.of("&7클릭")));

        GuiSession.set(p, KEY, page);
        p.openInventory(inv);
    }
}
