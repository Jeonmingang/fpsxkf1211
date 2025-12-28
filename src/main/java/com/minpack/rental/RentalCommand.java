package com.minpack.rental;

import com.minpack.rental.gui.MarketGui;
import com.minpack.rental.gui.MyListingsGui;
import com.minpack.rental.util.Msg;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public final class RentalCommand implements CommandExecutor {
    private final PixelmonRentalMarketPlugin plugin;
    public RentalCommand(PixelmonRentalMarketPlugin plugin) { this.plugin = plugin; }

    private long parseTime(String h, String m, String s) {
        long hh = Long.parseLong(h);
        long mm = Long.parseLong(m);
        long ss = Long.parseLong(s);
        return hh*3600L + mm*60L + ss;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { Msg.send(sender, "only_player"); return true; }

        try {
            // 1. /렌탈 (거래소 오픈)
            if (args.length == 0) {
                new MarketGui(plugin).open(p, 0);
                Msg.send(p, "open_market");
                return true;
            }

            // 2. /렌탈 목록 (내 목록 오픈)
            if (args[0].equalsIgnoreCase("목록")) {
                new MyListingsGui(plugin).open(p);
                Msg.send(p, "open_my");
                return true;
            }

            // [추가] 3. /렌탈 알림 <켜기|끄기> [초]
            if (args[0].equalsIgnoreCase("알림")) {
                if (args.length < 2) {
                    p.sendMessage(Msg.color("&c사용법: /렌탈 알림 <켜기|끄기> [초]"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("끄기")) {
                    plugin.getRentalService().setNotificationInterval(p, 0);
                    p.sendMessage(Msg.color("&c렌탈 남은 시간 알림을 껐습니다."));
                    return true;
                }
                if (args[1].equalsIgnoreCase("켜기")) {
                    int sec = 600; // 기본값 10분(600초)
                    if (args.length >= 3) {
                        try {
                            sec = Integer.parseInt(args[2]);
                            if (sec < 10) sec = 10; // 최소 10초 제한 (도배 방지)
                        } catch(Exception ignored){
                            p.sendMessage(Msg.color("&c시간은 숫자로 입력해주세요."));
                            return true;
                        }
                    }
                    plugin.getRentalService().setNotificationInterval(p, sec);
                    p.sendMessage(Msg.color("&b렌탈 알림이 &e" + sec + "초&b마다 울리도록 설정되었습니다."));
                    return true;
                }
            }

            // 4. /렌탈 등록 취소 (구 버전 명령어 호환)
            if (args[0].equalsIgnoreCase("등록") && args.length >= 2 && args[1].equalsIgnoreCase("취소")) {
                if (args.length < 3) return false;
                long id = Long.parseLong(args[2]);
                boolean ok = plugin.getRentalService().cancelListing(p, id);
                if (ok) p.sendMessage(Msg.fmt("cancelled","ID",String.valueOf(id)));
                return true;
            }

            // 5. /렌탈 등록 <슬롯> <시간> <분> <초> <가격>
            if (args[0].equalsIgnoreCase("등록")) {
                if (args.length < 6) return false;
                int slot = Integer.parseInt(args[1]);
                if (slot < 1 || slot > 6) { Msg.send(p, "invalid_slot"); return true; }
                long dur = parseTime(args[2], args[3], args[4]);
                double price = Double.parseDouble(args[5]);
                var l = plugin.getRentalService().registerListing(p, slot-1, dur, price);
                if (l == null) { Msg.send(p,"slot_empty"); return true; }
                p.sendMessage(Msg.fmt("registered","ID",String.valueOf(l.id),"PRICE",String.valueOf(price)));
                return true;
            }

            // 6. /렌탈 등록취소 <ID>
            if (args[0].equalsIgnoreCase("등록취소")) {
                if (args.length < 2) return false;
                long id = Long.parseLong(args[1]);
                boolean ok = plugin.getRentalService().cancelListing(p, id);
                if (ok) p.sendMessage(Msg.fmt("cancelled","ID",String.valueOf(id)));
                return true;
            }

            // --- 관리자 명령어 ---

            if (args[0].equalsIgnoreCase("등록시간")) {
                if (!p.hasPermission("rental.admin")) { Msg.send(p,"no_permission"); return true; }
                if (args.length < 4) return false;
                long sec = parseTime(args[1], args[2], args[3]);
                plugin.getConfig().set("market.listing_lifetime_seconds", Math.max(30, sec));
                plugin.saveConfig();
                p.sendMessage(Msg.fmt("set_listing_time","SEC",String.valueOf(sec)));
                return true;
            }

            if (args[0].equalsIgnoreCase("리로드")) {
                if (!p.hasPermission("rental.admin")) { Msg.send(p,"no_permission"); return true; }
                plugin.reloadConfig();
                com.minpack.rental.util.Msg.init(plugin);
                p.sendMessage(com.minpack.rental.util.Msg.color("&aconfig.yml 리로드 완료"));
                return true;
            }

            if (args[0].equalsIgnoreCase("강제회수")) {
                if (!p.hasPermission("rental.admin")) { Msg.send(p,"no_permission"); return true; }
                if (args.length < 2) return false;
                long id = Long.parseLong(args[1]);
                plugin.getRentalService().endRental(id);
                p.sendMessage(com.minpack.rental.util.Msg.color("&a렌탈 ID " + id + " 강제 회수 완료"));
                return true;
            }

        } catch (Exception ex) {
            p.sendMessage(Msg.color("&c오류: " + ex.getMessage()));
            return true;
        }
        return false;
    }
}