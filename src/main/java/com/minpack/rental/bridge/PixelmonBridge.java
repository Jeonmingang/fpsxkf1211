package com.minpack.rental.bridge;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.entities.pixelmon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.items.ItemPixelmonSprite; // EV훈련권에 있던 그 클래스
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PixelmonBridge {

    private final PixelmonRentalMarketPlugin plugin;

    public PixelmonBridge(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    // ... (기존 메서드들 생략: isPixelmonPresent, getPartyPokemon 등은 유지) ...

    /**
     * 포켓몬 객체를 받아 GUI용 아이템(사진 + 상세 Lore)으로 변환합니다.
     */
    public ItemStack getPokemonSpriteWithLore(Object pokemonObj) {
        if (!(pokemonObj instanceof Pokemon)) return null;
        Pokemon p = (Pokemon) pokemonObj;

        // 1. 스프라이트(사진) 가져오기 (EV훈련권 로직 참고)
        ItemStack item = ItemPixelmonSprite.getPhoto(p); 

        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        // 2. 상세 정보 Lore 구성 (이미지 참고)
        lore.add("§7--------------------");
        lore.add("§e레벨: §f" + p.getLevel()); //
        lore.add("§e성별: §f" + p.getGender().toString()); //
        lore.add("§e성격: §f" + (p.getMintNature() != null ? p.getMintNature().getLocalizedName() : p.getNature().getLocalizedName())); //
        lore.add("§e특성: §f" + p.getAbility().getLocalizedName()); //
        lore.add("§e볼: §f" + p.getBall().getName()); //
        lore.add("§e닉네임: §f" + (p.getNickname() != null ? p.getNickname().getString() : "-")); //
        lore.add("§e원트레이너: §f" + (p.getOriginalTrainer() != null ? p.getOriginalTrainer() : "Unknown")); //
        lore.add("§e이로치: §f" + (p.isShiny() ? "예" : "아니오")); //
        lore.add("§e크기: §f" + p.getGrowth().toString()); //
        lore.add("§e친밀도: §f" + p.getFriendship()); //
        lore.add("");

        // 스탯 (IVs)
        lore.add("§e[스탯 정보]");
        // 문서에 따르면 getIVs()는 IVStore를 반환하며, getStat(BattleStatsType)을 씁니다.
        String ivs = String.format("§7체:%d 공:%d 방:%d 특공:%d 특방:%d 스핏:%d",
                p.getIVs().getStat(BattleStatsType.HP),
                p.getIVs().getStat(BattleStatsType.ATTACK),
                p.getIVs().getStat(BattleStatsType.DEFENSE),
                p.getIVs().getStat(BattleStatsType.SPECIAL_ATTACK),
                p.getIVs().getStat(BattleStatsType.SPECIAL_DEFENSE),
                p.getIVs().getStat(BattleStatsType.SPEED));
        lore.add(ivs);
        lore.add("§7(개체값은 모두 고정됨)"); 

        // 기술 목록
        lore.add("");
        lore.add("§b[기술 목록]");
        for (Attack attack : p.getMoveset()) { //
            if (attack != null) {
                lore.add("§7- " + attack.getActualMove().getLocalizedName());
            }
        }
        lore.add("§7--------------------");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    // ... (기존 유틸 메서드 유지) ...
}
