package com.minpack.rental.gui;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.minpack.rental.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class GuiListener implements Listener {
    private final PixelmonRentalMarketPlugin plugin;

    public GuiListener(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // [안전장치 1] 제목에 '렌탈'이 들어가면 무조건 클릭 취소부터 함 (세션 오류 방지)
        String title = e.getView().getTitle();
        if (title.contains("렌탈") || title.contains("목록")) {
            e.setCancelled(true);
        }

        GuiSession.Session s = GuiSession.get(p);
        if (s == null) return;

        // 내 인벤토리(아래쪽)를 클릭했을 때
        if (e.getClickedInventory() == e.getWhoClicked().getInventory()) {
            // 쉬프트 클릭으로 아이템을 집어넣으려는 행위 차단
            if (e.isShiftClick()) e.setCancelled(true);
            return;
        }

        ItemStack it = e.getCurrentItem();
        if (it == null || it.getType() == Material.AIR) return;

        try {
            // 1. 렌탈 거래소 (전체 목록)
            if (MarketGui.KEY.equals(s.key())) {
                int page = s.page();

                // [핵심 수정] 페이지 이동 시 '1틱 딜레이'를 줘서 인벤토리 닫힘 처리가 꼬이지 않게 함
                if (e.getRawSlot() == 45) { // 이전 페이지
                    Bukkit.getScheduler().runTask(plugin, () -> new MarketGui(plugin).open(p, Math.max(0, page - 1)));
                    return;
                }
                if (e.getRawSlot() == 49) { // 내 목록으로 가기
                    Bukkit.getScheduler().runTask(plugin, () -> new MyListingsGui(plugin).open(p));
                    return;
                }
                if (e.getRawSlot() == 53) { // 다음 페이지
                    Bukkit.getScheduler().runTask(plugin, () -> new MarketGui(plugin).open(p, page + 1));
                    return;
                }

                // 구매 로직
                long id = parseId(it);
                if (id > 0) {
                    boolean ok = plugin.getRentalService().buy(p, id);
                    if (ok) p.sendMessage(Msg.fmt("bought", "DUR", "0"));

                    // 구매 후 새로고침도 안전하게 딜레이 적용
                    Bukkit.getScheduler().runTask(plugin, () -> new MarketGui(plugin).open(p, page));
                }
                return;
            }

            // 2. 내 렌탈 목록
            if (MyListingsGui.KEY.equals(s.key())) {
                if (e.getRawSlot() == 49) {
                    p.closeInventory();
                    return;
                }

                // 등록 취소 로직
                long id = parseId(it);
                if (id > 0) {
                    boolean ok = plugin.getRentalService().cancelListing(p, id);
                    if (ok) p.sendMessage(Msg.fmt("cancelled", "ID", String.valueOf(id)));

                    // 취소 후 새로고침 딜레이 적용
                    Bukkit.getScheduler().runTask(plugin, () -> new MyListingsGui(plugin).open(p));
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("GUI error: " + ex.getMessage());
        }
    }

    // [추가] 드래그해서 아이템 가져가는 것 방지
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player) {
            String title = e.getView().getTitle();
            // 제목이 렌탈 관련이면 드래그 절대 금지
            if (title.contains("렌탈") || title.contains("목록")) {
                e.setCancelled(true);
            }
        }
    }

    // ID 파싱 (Lore에서 ID 숫자 추출)
    private long parseId(ItemStack it) {
        if (it.getItemMeta() == null || !it.getItemMeta().hasLore()) return -1;
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
        if (e.getPlayer() instanceof Player p) {
            // 인벤토리가 닫히면 세션 삭제
            GuiSession.clear(p);
        }
    }
}