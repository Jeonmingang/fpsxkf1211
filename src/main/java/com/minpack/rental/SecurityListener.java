package com.minpack.rental.security;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.pixelmonmod.pixelmon.api.events.PixelmonReceivedItemEvent; // 아이템 사용 감지
import com.pixelmonmod.pixelmon.api.events.ExperienceGainEvent; // 경험치 획득 감지
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SecurityListener implements Listener {
    
    private final PixelmonRentalMarketPlugin plugin;

    public SecurityListener(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    // 1. 포켓몬에게 아이템(사탕, 포션, 노력치 열매 등) 주는 것 방지
    @EventHandler
    public void onItemGive(PixelmonReceivedItemEvent e) {
        Pokemon p = e.pokemon;
        // 렌탈 중인 포켓몬인지 확인 (UUID 체크)
        if (plugin.getRentalService().isPokemonListedOrActive(p.getUUID().toString())) {
            e.setCanceled(true); // 이벤트 취소
            if (e.player != null) {
                e.player.sendMessage("§c[렌탈] 이 포켓몬에게는 아이템을 사용할 수 없습니다.");
            }
        }
    }

    // 2. 경험치 획득 방지 (레벨업 원천 봉쇄)
    @EventHandler
    public void onExpGain(ExperienceGainEvent e) {
        Pokemon p = e.getPokemon();
        if (plugin.getRentalService().isPokemonListedOrActive(p.getUUID().toString())) {
            e.setCanceled(true); // 경험치 획득 자체를 취소
            e.setExperience(0);
        }
    }
}
