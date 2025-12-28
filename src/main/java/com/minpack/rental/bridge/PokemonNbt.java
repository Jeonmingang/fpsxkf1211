package com.minpack.rental.bridge;

import org.bukkit.Bukkit;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Base64;

public final class PokemonNbt {

    private PokemonNbt() {}

    public static String toBase64(Object pokemon) throws Exception {
        Object tag = newCompoundTag();
        Object provider = lookupProvider();

        Method write = pokemon.getClass().getMethod("writeToNBT", tag.getClass(), provider.getClass());
        write.invoke(pokemon, tag, provider);

        byte[] compressed = writeCompressed(tag);
        return Base64.getEncoder().encodeToString(compressed);
    }

    public static Object fromBase64(String b64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(b64);
        Object tag = readCompressed(bytes);
        Object provider = lookupProvider();

        Class<?> factory = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory");
        for (Method m : factory.getMethods()) {
            if (!m.getName().equals("create")) continue;
            if (m.getParameterCount() == 2 && m.getParameterTypes()[0].getName().endsWith("CompoundTag")) {
                return m.invoke(null, tag, provider);
            }
        }
        throw new IllegalStateException("PokemonFactory.create not found");
    }

    private static Object newCompoundTag() throws Exception {
        Class<?> c = Class.forName("net.minecraft.nbt.CompoundTag");
        return c.getConstructor().newInstance();
    }

    private static Object lookupProvider() throws Exception {
        Object mcServer = getMinecraftServer();
        if (mcServer == null) throw new IllegalStateException("NMS server not available");
        Method m = mcServer.getClass().getMethod("registryAccess");
        return m.invoke(mcServer);
    }

    private static Object getMinecraftServer() {
        try {
            Object craftServer = Bukkit.getServer();
            Method m = craftServer.getClass().getMethod("getServer");
            return m.invoke(craftServer);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] writeCompressed(Object tag) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Class<?> nbtIo = Class.forName("net.minecraft.nbt.NbtIo");
        Method write = null;
        for (Method m : nbtIo.getMethods()) {
            if (m.getName().equals("writeCompressed") && m.getParameterCount() == 2) { write = m; break; }
        }
        if (write == null) throw new IllegalStateException("NbtIo.writeCompressed not found");
        write.invoke(null, tag, baos);
        return baos.toByteArray();
    }

    private static Object readCompressed(byte[] bytes) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Class<?> nbtIo = Class.forName("net.minecraft.nbt.NbtIo");
        Method read = null;
        for (Method m : nbtIo.getMethods()) {
            if (m.getName().equals("readCompressed") && m.getParameterCount() == 1) { read = m; break; }
        }
        if (read == null) throw new IllegalStateException("NbtIo.readCompressed not found");
        return read.invoke(null, bais);
    }
}
