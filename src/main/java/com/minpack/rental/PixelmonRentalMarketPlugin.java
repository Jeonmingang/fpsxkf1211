package com.minpack.rental;

import com.minpack.rental.bridge.PixelmonBridge;
import com.minpack.rental.data.RentalRepository;
import com.minpack.rental.discord.DiscordWebhook;
import com.minpack.rental.gui.GuiListener;
import com.minpack.rental.security.CommandBlocker;
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

        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CommandBlocker(this), this);

        getCommand("렌탈").setExecutor(new RentalCommand(this));
        getCommand("렌탈").setTabCompleter(new RentalTab(this));

        int audit = Math.max(5, getConfig().getInt("security.audit_tick_seconds", 15));
        Bukkit.getScheduler().runTaskTimer(this, () -> rentalService.auditAndEnforce(), audit * 20L, audit * 20L);
        Bukkit.getScheduler().runTaskTimer(this, () -> rentalService.tickExpiry(), 20L, 20L);

        getLogger().info("PixelmonRentalMarket enabled.");
    }

    @Override
    public void onDisable() {
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
