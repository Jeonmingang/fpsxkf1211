package com.minpack.rental;

import org.bukkit.command.*;

import java.util.ArrayList;
import java.util.List;

public final class RentalTab implements TabCompleter {
    private final PixelmonRentalMarketPlugin plugin;
    public RentalTab(PixelmonRentalMarketPlugin plugin) { this.plugin = plugin; }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("등록");
            out.add("목록");
            out.add("등록취소");
            if (sender.hasPermission("rental.admin")) { out.add("등록시간"); out.add("강제회수"); out.add("리로드"); }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("등록")) {
            return List.of("1","2","3","4","5","6");
        }
        return out;
    }
}
