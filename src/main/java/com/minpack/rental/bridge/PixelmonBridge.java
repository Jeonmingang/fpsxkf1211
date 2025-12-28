package com.minpack.rental.bridge;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

public final class PixelmonBridge {

    private final PixelmonRentalMarketPlugin plugin;

    public PixelmonBridge(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPixelmonPresent() {
        try {
            Class.forName("com.pixelmonmod.pixelmon.Pixelmon", false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public Object getPartyPokemon(UUID playerUuid, int slot) throws Exception {
        Object party = getPartyStorage(playerUuid);
        if (party == null) return null;

        Method m = findMethod(party.getClass(), "get", int.class);
        if (m != null) return m.invoke(party, slot);

        m = findMethod(party.getClass(), "getPokemon", int.class);
        if (m != null) return m.invoke(party, slot);

        return null;
    }

    public void setPartyPokemon(UUID playerUuid, int slot, Object pokemon) throws Exception {
        Object party = getPartyStorage(playerUuid);
        if (party == null) return;

        Method m = null;
        // try set(int, Pokemon)
        for (Method mm : party.getClass().getMethods()) {
            if (!mm.getName().equals("set") && !mm.getName().equals("setPokemon")) continue;
            if (mm.getParameterCount() == 2 && mm.getParameterTypes()[0] == int.class) {
                m = mm; break;
            }
        }
        if (m != null) {
            m.invoke(party, slot, pokemon);
            return;
        }
        throw new IllegalStateException("Party storage set method not found");
    }

    public boolean addToPartyOrPC(UUID playerUuid, Object pokemon) throws Exception {
        Object party = getPartyStorage(playerUuid);
        if (party == null) return false;

        Method add = null;
        for (Method mm : party.getClass().getMethods()) {
            if (mm.getName().equals("add") && mm.getParameterCount() == 1) { add = mm; break; }
        }
        if (add != null) {
            Object res = add.invoke(party, pokemon);
            if (res instanceof Boolean b && b) return true;
        }

        Object pc = getPcStorage(playerUuid);
        if (pc != null) {
            Method addPc = null;
            for (Method mm : pc.getClass().getMethods()) {
                if (mm.getName().equals("add") && mm.getParameterCount() == 1) { addPc = mm; break; }
            }
            if (addPc != null) {
                addPc.invoke(pc, pokemon);
                return true;
            }
        }
        return false;
    }

    public Object getPartyStorage(UUID playerUuid) throws Exception {
        Class<?> storageProxy = Class.forName("com.pixelmonmod.pixelmon.storage.StorageProxy");

        Method getParty = findMethod(storageProxy, "getParty", Player.class);
        if (getParty != null) {
            Player p = Bukkit.getPlayer(playerUuid);
            if (p == null) return null;
            return getParty.invoke(null, p);
        }

        getParty = findMethod(storageProxy, "getParty", UUID.class);
        if (getParty != null) return getParty.invoke(null, playerUuid);

        return null;
    }

    public Object getPcStorage(UUID playerUuid) throws Exception {
        Class<?> storageProxy = Class.forName("com.pixelmonmod.pixelmon.storage.StorageProxy");
        Method getPC = findMethod(storageProxy, "getPC", Player.class);
        if (getPC != null) {
            Player p = Bukkit.getPlayer(playerUuid);
            if (p == null) return null;
            return getPC.invoke(null, p);
        }
        getPC = findMethod(storageProxy, "getPC", UUID.class);
        if (getPC != null) return getPC.invoke(null, playerUuid);
        return null;
    }

    public String getPokemonUuidString(Object pokemon) {
        try {
            Method m = findMethod(pokemon.getClass(), "getUUID");
            if (m != null) return String.valueOf(m.invoke(pokemon));
        } catch (Exception ignored) {}
        return "";
    }

    public static Method findMethod(Class<?> c, String name, Class<?>... params) {
        try {
            return c.getMethod(name, params);
        } catch (Exception e) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
