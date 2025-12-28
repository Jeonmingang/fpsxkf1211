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
            if (args.length == 0) {
                new MarketGui(plugin).open(p, 0);
                Msg.send(p, "open_market");
                return true;
            }

            if (args[0].equalsIgnoreCase("목록")) {
                new MyListingsGui(plugin).open(p);
                Msg.send(p, "open_my");
                return true;
            }

            if (args[0].equalsIgnoreCase("등록") && args.length >= 2 && args[1].equalsIgnoreCase("취소")) {
                if (args.length < 3) return false;
                long id = Long.parseLong(args[2]);
                boolean ok = plugin.getRentalService().cancelListing(p, id);
                if (ok) p.sendMessage(Msg.fmt("cancelled","ID",String.valueOf(id)));
                return true;
            }

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

            if (args[0].equalsIgnoreCase("등록취소")) {
                if (args.length < 2) return false;
                long id = Long.parseLong(args[1]);
                boolean ok = plugin.getRentalService().cancelListing(p, id);
                if (ok) p.sendMessage(Msg.fmt("cancelled","ID",String.valueOf(id)));
                return true;
            }

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
