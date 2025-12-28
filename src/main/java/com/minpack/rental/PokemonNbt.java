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

        // [수정됨] provider.getClass() 대신 정확한 인터페이스 클래스를 로드합니다.
        Class<?> registryAccessClass = Class.forName("net.minecraft.core.RegistryAccess");

        // [수정됨] writeToNBT(CompoundTag, RegistryAccess) 메서드를 찾습니다.
        Method write = pokemon.getClass().getMethod("writeToNBT", tag.getClass(), registryAccessClass);
        write.invoke(pokemon, tag, provider);

        byte[] compressed = writeCompressed(tag);
        return Base64.getEncoder().encodeToString(compressed);
    }

    public static Object fromBase64(String b64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(b64);
        Object tag = readCompressed(bytes);
        Object provider = lookupProvider();

        Class<?> factory = Class.forName("com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory");
        // [개선됨] create 메서드를 찾을 때 더 정확하게 파라미터 타입을 체크합니다.
        for (Method m : factory.getMethods()) {
            if (!m.getName().equals("create")) continue;
            Class<?>[] params = m.getParameterTypes();
            // 파라미터가 2개이고 (CompoundTag, RegistryAccess) 형태인 메서드를 찾음
            if (params.length == 2
                    && params[0].getName().endsWith("CompoundTag")
                    && params[1].getName().endsWith("RegistryAccess")) {
                return m.invoke(null, tag, provider);
            }
        }
        throw new IllegalStateException("PokemonFactory.create method not found");
    }

    private static Object newCompoundTag() throws Exception {
        Class<?> c = Class.forName("net.minecraft.nbt.CompoundTag");
        return c.getConstructor().newInstance();
    }

    private static Object lookupProvider() throws Exception {
        Object mcServer = getMinecraftServer();
        if (mcServer == null) throw new IllegalStateException("NMS server not available");
        // registryAccess() 메서드 호출
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
        // writeCompressed(CompoundTag, OutputStream) 찾기
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
        // readCompressed(InputStream) 찾기
        for (Method m : nbtIo.getMethods()) {
            if (m.getName().equals("readCompressed") && m.getParameterCount() == 1) { read = m; break; }
        }
        if (read == null) throw new IllegalStateException("NbtIo.readCompressed not found");
        return read.invoke(null, bais);
    }
}
