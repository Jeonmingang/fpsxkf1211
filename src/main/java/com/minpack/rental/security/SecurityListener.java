package com.minpack.rental.security;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Pixelmon(모드) 이벤트 버스는 빌드 환경/CI에서 의존성이 없을 수 있으므로,
 * 이 클래스는 Bukkit 이벤트만 사용해 "감시/복구(auditAndEnforce)"를 자주 트리거합니다.
 *
 * - 핵심 보안은 RentalService#auditAndEnforce()가 스냅샷으로 덮어쓰는 방식
 * - 추가로 온라인 유저 전체에서 동일 UUID 듀프를 제거하도록 RentalService에서 강화됨
 */
public final class SecurityListener implements Listener {

    private final PixelmonRentalMarketPlugin plugin;
    private boolean registered = false;

    public SecurityListener(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 플러그인 활성화 시 호출. Bukkit 리스너 등록 + 즉시 1회 감사(audit).
     */
    public void register() {
        if (registered) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        registered = true;

        // 최초 1회 강제 정합성 체크
        Bukkit.getScheduler().runTask(plugin, () -> {
            try { plugin.getRentalService().auditAndEnforce(); } catch (Throwable ignored) {}
        });

        // Pixelmon 모드가 없거나 접근 실패 시에도 플러그인은 정상 동작
        if (!plugin.getPixelmon().isPixelmonPresent()) {
            plugin.getLogger().warning("Pixelmon API not detected. Security will rely on periodic audit only.");
        }
    }

    /**
     * 플러그인 비활성화 시 호출. Bukkit 리스너는 자동 해제되지만, 플러그인 재로딩 대비 플래그만 정리.
     */
    public void unregister() {
        registered = false;
    }

    // ---- Bukkit 트리거: 렌탈 관련 조작이 자주 일어나는 타이밍에 audit를 앞당김 ----

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        triggerAuditSoon();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // 나갈 때도 한번 정리 (듀프/PC이동 등)
        triggerAuditSoon();
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        triggerAuditSoon();
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent e) {
        triggerAuditSoon();
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        // 거래/보관류 GUI를 닫는 순간 변경이 반영될 수 있어 audit
        triggerAuditSoon();
    }

    private void triggerAuditSoon() {
        // 이미 스케줄러가 돌아가지만, "즉시" 복구 체감 개선용
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try { plugin.getRentalService().auditAndEnforce(); } catch (Throwable ignored) {}
        }, 1L);
    }
}
