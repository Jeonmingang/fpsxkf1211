package com.minpack.rental.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class RentalRepository {
    private final JavaPlugin plugin;
    private final File file;
    private long nextId = 1;

    public final Map<Long, Listing> listings = new LinkedHashMap<>();
    public final Map<Long, ActiveRental> active = new LinkedHashMap<>();

    public RentalRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "rentals.yml");
    }

    public long peekNextId() { return nextId; }
    public void setNextId(long id) { nextId = id; }

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        nextId = y.getLong("meta.nextId", 1);
        listings.clear(); active.clear();

        if (y.isConfigurationSection("listings")) {
            for (String k : Objects.requireNonNull(y.getConfigurationSection("listings")).getKeys(false)) {
                long id = Long.parseLong(k);
                Listing l = new Listing();
                l.id = id;
                l.owner = UUID.fromString(y.getString("listings."+k+".owner"));
                l.ownerName = y.getString("listings."+k+".ownerName", "unknown");
                l.pokemonUuid = y.getString("listings."+k+".pokemonUuid", "");
                l.pokemonNbtB64 = y.getString("listings."+k+".pokemonNbtB64", "");
                l.price = y.getDouble("listings."+k+".price", 0.0);
                l.rentalDurationSeconds = y.getLong("listings."+k+".rentalDurationSeconds", 0);
                l.createdEpochSec = y.getLong("listings."+k+".createdEpochSec", 0);
                l.expireEpochSec = y.getLong("listings."+k+".expireEpochSec", 0);
                listings.put(id, l);
            }
        }

        if (y.isConfigurationSection("active")) {
            for (String k : Objects.requireNonNull(y.getConfigurationSection("active")).getKeys(false)) {
                long id = Long.parseLong(k);
                ActiveRental r = new ActiveRental();
                r.id = id;
                r.owner = UUID.fromString(y.getString("active."+k+".owner"));
                r.renter = UUID.fromString(y.getString("active."+k+".renter"));
                r.renterName = y.getString("active."+k+".renterName", "unknown");
                r.pokemonUuid = y.getString("active."+k+".pokemonUuid", "");
                r.pokemonNbtB64 = y.getString("active."+k+".pokemonNbtB64", "");
                r.price = y.getDouble("active."+k+".price", 0.0);
                r.startEpochSec = y.getLong("active."+k+".startEpochSec", 0);
                r.endEpochSec = y.getLong("active."+k+".endEpochSec", 0);
                r.finished = y.getBoolean("active."+k+".finished", false);
                active.put(id, r);
            }
        }
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            YamlConfiguration y = new YamlConfiguration();
            y.set("meta.nextId", nextId);

            for (Listing l : listings.values()) {
                String p = "listings."+l.id+".";
                y.set(p+"owner", l.owner.toString());
                y.set(p+"ownerName", l.ownerName);
                y.set(p+"pokemonUuid", l.pokemonUuid);
                y.set(p+"pokemonNbtB64", l.pokemonNbtB64);
                y.set(p+"price", l.price);
                y.set(p+"rentalDurationSeconds", l.rentalDurationSeconds);
                y.set(p+"createdEpochSec", l.createdEpochSec);
                y.set(p+"expireEpochSec", l.expireEpochSec);
            }
            for (ActiveRental r : active.values()) {
                String p = "active."+r.id+".";
                y.set(p+"owner", r.owner.toString());
                y.set(p+"renter", r.renter.toString());
                y.set(p+"renterName", r.renterName);
                y.set(p+"pokemonUuid", r.pokemonUuid);
                y.set(p+"pokemonNbtB64", r.pokemonNbtB64);
                y.set(p+"price", r.price);
                y.set(p+"startEpochSec", r.startEpochSec);
                y.set(p+"endEpochSec", r.endEpochSec);
                y.set(p+"finished", r.finished);
            }
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save rentals.yml: " + e.getMessage());
        }
    }
}
