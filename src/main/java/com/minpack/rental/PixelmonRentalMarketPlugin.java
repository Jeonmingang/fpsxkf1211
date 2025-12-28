package com.minpack.rental;

import com.minpack.rental.bridge.PixelmonBridge;
import com.minpack.rental.data.RentalRepository;
import com.minpack.rental.discord.DiscordWebhook;
import com.minpack.rental.gui.GuiListener;
import com.minpack.rental.security.CommandBlocker;
import com.minpack.rental.security.SecurityListener; // [중요] 1. 임포트 추가
import com.minpack.rental.service.RentalService;
import com.minpack.rental.util.Msg;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class PixelmonRentalMarketPlugin extends JavaPlugin {

    private static PixelmonRentalMarketPlugin instance;

    private Economy economy;
    private RentalRepository repository;
    private DiscordWebhook discord;
    private PixelmonBridge pixelmon;
    private RentalService rentalService;

    // [중요] 2. 리스너 변수 선언
    private SecurityListener securityListener;

    public static PixelmonRentalMarketPlugin getInstance() { return instance; }

    public Economy getEconomy() { return economy; }
    public RentalRepository getRepository() { return repository; }
    public DiscordWebhook getDiscord() { return discord; }
    public PixelmonBridge getPixelmon() { return pixelmon; }
    public RentalService getRentalService() { return rentalService; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Msg.init(this);

        repository = new RentalRepository(this);
        repository.load();

        discord = new DiscordWebhook(this);
        pixelmon = new PixelmonBridge(this);

        if (getConfig().getBoolean("economy.require_vault", true)) setupEconomy();

        rentalService = new RentalService(this);
        rentalService.loadFromRepository(repository);

        // 버킷(일반) 리스너 등록
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlocker(this), this);

        // [중요] 3. 픽셀몬 보안 리스너 등록
        // 이 코드가 있어야 배틀 후 복구, 삭제 방지 기능이 켜집니다.
        securityListener = new SecurityListener(this);
        securityListener.register();

        getCommand("렌탈").setExecutor(new RentalCommand(this));
        getCommand("렌탈").setTabCompleter(new RentalTab(this));

        int audit = Math.max(5, getConfig().getInt("security.audit_tick_seconds", 15));
        Bukkit.getScheduler().runTaskTimer(this, () -> rentalService.auditAndEnforce(), audit * 20L, audit * 20L);
        Bukkit.getScheduler().runTaskTimer(this, () -> rentalService.tickExpiry(), 20L, 20L);

        getLogger().info("PixelmonRentalMarket enabled.");
    }

    @Override
    public void onDisable() {
        // [중요] 4. 리스너 해제 (플러그인 꺼질 때 깔끔하게 정리)
        if (securityListener != null) {
            securityListener.unregister();
        }

        if (rentalService != null) rentalService.flushToRepository(repository);
        if (repository != null) repository.save();
        getLogger().info("PixelmonRentalMarket disabled.");
    }

    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found - economy disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().warning("No economy provider found - economy disabled.");
            return;
        }
        economy = rsp.getProvider();
        getLogger().info("Economy provider: " + economy.getName());
    }
}