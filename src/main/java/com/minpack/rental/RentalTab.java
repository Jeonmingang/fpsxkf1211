package com.minpack.rental;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class RentalTab implements TabCompleter {
    private final PixelmonRentalMarketPlugin plugin;

    public RentalTab(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // 1. 첫 번째 명령어 (등록, 목록, 알림, 취소 등)
        if (args.length == 1) {
            List<String> sub = new ArrayList<>();
            sub.add("등록");
            sub.add("목록");
            sub.add("알림"); // [추가됨] 알림 명령어 자동완성
            sub.add("등록취소");

            // 관리자 권한이 있을 때만 뜨는 명령어
            if (sender.hasPermission("rental.admin")) {
                sub.add("등록시간");
                sub.add("강제회수");
                sub.add("리로드");
            }
            return StringUtil.copyPartialMatches(args[0], sub, completions);
        }

        // 2. 두 번째 이후 인자 처리
        if (args.length >= 2) {
            String subCmd = args[0].toLowerCase();

            // --- /렌탈 알림 <켜기|끄기> [초] ---
            if (subCmd.equals("알림")) {
                if (args.length == 2) {
                    return StringUtil.copyPartialMatches(args[1], List.of("켜기", "끄기"), completions);
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("켜기")) {
                    // 자주 쓰는 시간 추천 (1분, 5분, 10분)
                    return StringUtil.copyPartialMatches(args[2], List.of("60", "300", "600"), completions);
                }
            }

            // --- /렌탈 등록 <슬롯> <시간> <분> <초> <가격> ---
            if (subCmd.equals("등록")) {
                if (args.length == 2) {
                    // 파티 슬롯 1~6 제안
                    return StringUtil.copyPartialMatches(args[1], List.of("1", "2", "3", "4", "5", "6"), completions);
                }
                if (args.length == 3) return List.of("<시간>"); // 힌트
                if (args.length == 4) return List.of("<분>");   // 힌트
                if (args.length == 5) return List.of("<초>");   // 힌트
                if (args.length == 6) return List.of("<가격>"); // 힌트
            }

            // --- /렌탈 등록취소 <ID> ---
            if (subCmd.equals("등록취소") && sender instanceof Player p) {
                if (args.length == 2) {
                    // 내가 올린 매물 ID만 쏙 뽑아서 자동완성 목록에 띄워줌
                    List<String> myIds = plugin.getRentalService().getOwnerListings(p.getUniqueId())
                            .stream()
                            .map(l -> String.valueOf(l.id))
                            .collect(Collectors.toList());
                    return StringUtil.copyPartialMatches(args[1], myIds, completions);
                }
            }

            // --- /렌탈 강제회수 <ID> (관리자용) ---
            if (subCmd.equals("강제회수") && sender.hasPermission("rental.admin")) {
                if (args.length == 2) {
                    return List.of("<렌탈ID>"); // 힌트 제공
                }
            }

            // --- /렌탈 등록시간 <초> (관리자용) ---
            if (subCmd.equals("등록시간") && sender.hasPermission("rental.admin")) {
                if (args.length == 2) {
                    return List.of("<시간>", "3600", "86400"); // 힌트 제공
                }
            }
        }

        return Collections.emptyList();
    }
}