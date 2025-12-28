package com.minpack.rental.security;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.daycare.event.DayCareEvent;
import com.pixelmonmod.pixelmon.api.events.*;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.events.battles.BattleUseItemEvent; // 배틀 아이템 사용
import com.pixelmonmod.pixelmon.api.events.pokemon.SetNicknameEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import org.bukkit.Bukkit;

import java.util.UUID;

public class SecurityListener {

    private final PixelmonRentalMarketPlugin plugin;

    public SecurityListener(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Pixelmon.EVENT_BUS.register(this);
    }

    public void unregister() {
        Pixelmon.EVENT_BUS.unregister(this);
    }

    // 헬퍼: 렌탈 중인 포켓몬인지 확인
    private boolean isRental(Pokemon p) {
        if (p == null) return false;
        // UUID가 렌탈 서비스에 등록되어 있는지 확인
        return plugin.getRentalService().isPokemonListedOrActive(p.getUUID().toString());
    }

    // ---------------- [보안 기능 구현] ----------------

    // 1. 포켓몬 방생(삭제) 차단 - 삭제 즉시 복구 전략
    @SubscribeEvent
    public void onDeleted(PixelmonDeletedEvent e) {
        if (isRental(e.getPokemon())) {
            // 경고 메시지
            org.bukkit.entity.Player p = Bukkit.getPlayer(e.getPlayer().getUUID());
            if (p != null) p.sendMessage("§c[렌탈] 대여 중인 포켓몬은 버릴 수 없습니다. (자동 복구됩니다)");

            // 삭제 이벤트는 이미 발생했으므로, 다음 틱에 강제로 다시 지급하여 복구
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getRentalService().auditAndEnforce();
            });
        }
    }

    // 2. 닉네임 변경 차단
    @SubscribeEvent
    public void onNickname(SetNicknameEvent e) {
        if (isRental(e.getPokemon())) {
            e.setCanceled(true);
            org.bukkit.entity.Player p = Bukkit.getPlayer(e.getPlayer().getUUID());
            if (p != null) p.sendMessage("§c[렌탈] 대여 중인 포켓몬의 이름은 변경할 수 없습니다.");
        }
    }

    // 3. 배틀 종료 시 상태 복구
    @SubscribeEvent
    public void onBattleEnd(BattleEndEvent e) {
        for (Player p : e.getPlayers()) {
            UUID uuid = p.getUUID();
            if (plugin.getRentalService().hasAnyActiveRental(uuid)) {
                // 배틀 종료 후 체력/상태이상/도구 소모 등을 원상복구
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getRentalService().auditAndEnforce();
                });
            }
        }
    }

    // 4. 교환 방지
    @SubscribeEvent
    public void onTrade(PixelmonTradeEvent.Pre e) {
        if (isRental(e.getPokemon1()) || isRental(e.getPokemon2())) {
            e.setCanceled(true);
            // 메시지 처리 (필요시)
        }
    }

    // 5. 레벨업 방지
    @SubscribeEvent
    public void onLevelUp(LevelUpEvent.Pre e) {
        if (isRental(e.getPokemon())) {
            e.setCanceled(true);
        }
    }

    // 6. 경험치 획득 방지
    @SubscribeEvent
    public void onExpGain(ExperienceGainEvent e) {
        if (isRental(e.getPokemon())) {
            e.setCanceled(true);
            e.setExperience(0);
        }
    }

    // 7. 진화 방지
    @SubscribeEvent
    public void onEvolve(EvolveEvent.Pre e) {
        if (isRental(e.getPokemon())) {
            e.setCanceled(true);
        }
    }

    // 8. 각종 사탕류 차단 (이상한 사탕, 다이맥스 사탕, 경험사탕 등 통합 방지 권장)
    @SubscribeEvent
    public void onRareCandy(RareCandyEvent e) {
        if (isRental(e.getPokemon())) e.setCanceled(true);
    }

    @SubscribeEvent
    public void onDynamaxCandy(DynamaxCandyEvent e) {
        if (isRental(e.getPokemon())) e.setCanceled(true);
    }

    // 9. 대단한 특훈(왕관) 차단
    @SubscribeEvent
    public void onHyperTrain(HyperTrainEvent e) {
        if (isRental(e.getPokemon())) e.setCanceled(true);
    }

    // 10. 지닌 물건 변경 차단 (매우 중요)
    @SubscribeEvent
    public void onItemChange(HeldItemChangedEvent.Pre e) {
        if (isRental(e.getPokemon())) {
            e.setCanceled(true);
            // 플레이어에게 알림
            if (e.getPlayer() != null) {
                org.bukkit.entity.Player p = Bukkit.getPlayer(e.getPlayer().getUUID());
                if (p != null) p.sendMessage("§c[렌탈] 대여 포켓몬의 도구는 변경할 수 없습니다.");
            }
        }
    }

    // 11. [추가] 기술 머신/기술 가르침 차단
    @SubscribeEvent
    public void onLearnMove(LearnMoveEvent e) {
        if (isRental(e.getPokemon())) {
            e.setCanceled(true); // TM, TR, 기술레코드 등 차단
        }
    }

    // 12. [추가] PC 박스로 이동 차단 (파티 이탈 방지)
    // 픽셀몬 버전에 따라 MoveToStorageEvent 혹은 TransferEvent 등을 사용
    @SubscribeEvent
    public void onMoveToStorage(MoveToStorageEvent.Pre e) {
        if (isRental(e.getPokemon())) {
            e.setCanceled(true);
            org.bukkit.entity.Player p = Bukkit.getPlayer(e.getPlayer().getUUID());
            if (p != null) p.sendMessage("§c[렌탈] 대여 포켓몬은 PC에 보관할 수 없습니다.");
        }
    }

    // 13. [수정] 교배(DayCare) 방지 구현
    @SubscribeEvent
    public void onDayCareAdd(DayCareEvent.PrePokemonAdd e) {
        // 교배소에 넣으려는 포켓몬이 렌탈인지 확인
        if (isRental(e.getPokemon())) {
            e.setCanceled(true);
            org.bukkit.entity.Player p = Bukkit.getPlayer(e.getPlayer().getUUID());
            if (p != null) p.sendMessage("§c[렌탈] 대여 포켓몬은 교배할 수 없습니다.");
        }
    }

    // 14. [추가] 배틀 중 아이템 사용 방지 (배틀 중 도구 소모 방지)
    @SubscribeEvent
    public void onBattleItemUse(BattleUseItemEvent e) {
        // 배틀에 참여 중인 내 포켓몬이 렌탈 포켓몬인 경우
        // (Pixelmon API 구조상 배틀 이벤트에서 대상 포켓몬 추출이 까다로울 수 있으므로,
        //  플레이어가 렌탈 중이라면 배틀 아이템 사용 자체를 막거나, auditAndEnforce에 의존)

        // 여기서는 안전하게 배틀 종료 시(onBattleEnd) 복구하는 로직(3번)이 있으므로
        // 굳이 막지 않아도 되지만, 완벽을 기한다면 추가합니다.
    }
}