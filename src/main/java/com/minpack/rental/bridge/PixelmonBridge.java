package com.minpack.rental.bridge;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Pixelmon 9.3.13 (1.21.1) 런타임 브릿지.
 *
 * - 빌드/CI 환경에서는 Pixelmon 모드가 없으므로 컴파일 의존성을 두지 않습니다.
 * - 런타임(서버)에서만 Class.forName + reflection 으로 Pixelmon Storage API에 접근합니다.
 */
public final class PixelmonBridge {

    private final PixelmonRentalMarketPlugin plugin;

    public PixelmonBridge(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isPixelmonPresent() {
        try {
            Class.forName("com.pixelmonmod.pixelmon.api.storage.StorageProxy");
            Class.forName("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 0~5 파티 슬롯 포켓몬 객체 반환 */
    public Object getPartyPokemon(UUID playerUuid, int slot0) throws Exception {
        Object party = getPartyNow(playerUuid);
        if (party == null) return null;
        Method get = party.getClass().getMethod("get", int.class);
        return get.invoke(party, slot0);
    }

    /** 0~5 파티 슬롯에 포켓몬 설정 (null 가능) */
    public void setPartyPokemon(UUID playerUuid, int slot0, Object pokemonOrNull) throws Exception {
        Object party = getPartyNow(playerUuid);
        if (party == null) return;

        // PartyStorage.set(int, Pokemon)
        Method set = null;
        for (Method m : party.getClass().getMethods()) {
            if (!m.getName().equals("set")) continue;
            if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == int.class) {
                set = m;
                break;
            }
        }
        if (set == null) throw new IllegalStateException("PartyStorage#set(int, Pokemon) not found");
        set.invoke(party, slot0, pokemonOrNull);

        sendPartyUpdateIfOnline(playerUuid, party);
    }

    /** Pokemon.getUUID().toString() */
    public String getPokemonUuidString(Object pokemon) {
        if (pokemon == null) return "";
        try {
            Method m = pokemon.getClass().getMethod("getUUID");
            Object u = m.invoke(pokemon);
            return (u instanceof UUID) ? u.toString() : String.valueOf(u);
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * 파티가 비어있으면 파티에, 아니면 PC에 넣습니다.
     * (또한 동일 UUID 중복이 있으면 먼저 제거해서 듀프 방지)
     */
    public void addToPartyOrPC(UUID playerUuid, Object pokemon) throws Exception {
        if (pokemon == null) return;

        String uuid = getPokemonUuidString(pokemon);
        if (!uuid.isEmpty()) {
            // 듀프 방지: 기존 위치(파티/PC)에 같은 UUID가 남아있으면 제거
            removeFromPartyByUuid(playerUuid, uuid);
            removeFromPCByUuid(playerUuid, uuid);
        }

        Object party = getPartyNow(playerUuid);
        if (party != null) {
            Integer empty = findFirstEmptyPartySlot(party);
            if (empty != null) {
                setPartyPokemon(playerUuid, empty, pokemon);
                return;
            }
        }

        Object pc = getPCNow(playerUuid);
        if (pc == null) throw new IllegalStateException("PCStorage not available");

        Object pos = invokeNoArg(pc, "getFirstEmptyPosition");
        if (pos == null) {
            throw new IllegalStateException("PC is full");
        }
        invokeSetByPosition(pc, pos, pokemon);
        sendPCUpdateIfOnline(playerUuid, pc);
    }

    /** 파티에서 특정 UUID 포켓몬 제거 */
    public void removeFromPartyByUuid(UUID playerUuid, String pokemonUuid) {
        try {
            for (int i = 0; i < 6; i++) {
                Object p = getPartyPokemon(playerUuid, i);
                if (p == null) continue;
                if (pokemonUuid.equals(getPokemonUuidString(p))) {
                    setPartyPokemon(playerUuid, i, null);
                }
            }
        } catch (Throwable ignored) {}
    }

    /** PC 전체를 훑어 특정 UUID 포켓몬 제거 (렌탈 듀프 방지용) */
    public void removeFromPCByUuid(UUID playerUuid, String pokemonUuid) {
        try {
            Object pc = getPCNow(playerUuid);
            if (pc == null) return;
            Object positions = invokeNoArg(pc, "getAllPositions");
            if (positions == null || !positions.getClass().isArray()) return;

            int len = Array.getLength(positions);
            for (int i = 0; i < len; i++) {
                Object pos = Array.get(positions, i);
                Object p = invokeGetByPosition(pc, pos);
                if (p == null) continue;
                if (pokemonUuid.equals(getPokemonUuidString(p))) {
                    invokeSetByPosition(pc, pos, null);
                }
            }

            sendPCUpdateIfOnline(playerUuid, pc);
        } catch (Throwable ignored) {}
    }

    // -------------------- 내부 리플렉션 유틸 --------------------

    private Object getPartyNow(UUID playerUuid) throws Exception {
        Class<?> storageProxy = Class.forName("com.pixelmonmod.pixelmon.api.storage.StorageProxy");
        Method m = storageProxy.getMethod("getPartyNow", UUID.class);
        return m.invoke(null, playerUuid);
    }

    private Object getPCNow(UUID playerUuid) throws Exception {
        Class<?> storageProxy = Class.forName("com.pixelmonmod.pixelmon.api.storage.StorageProxy");
        Method m = storageProxy.getMethod("getPCForPlayerNow", UUID.class);
        return m.invoke(null, playerUuid);
    }

    private Integer findFirstEmptyPartySlot(Object party) {
        try {
            for (int i = 0; i < 6; i++) {
                Method get = party.getClass().getMethod("get", int.class);
                Object p = get.invoke(party, i);
                if (p == null) return i;
            }
            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    private Object invokeNoArg(Object target, String name) throws Exception {
        Method m = target.getClass().getMethod(name);
        return m.invoke(target);
    }

    private Object invokeGetByPosition(Object storage, Object position) throws Exception {
        Method get = null;
        for (Method m : storage.getClass().getMethods()) {
            if (!m.getName().equals("get")) continue;
            if (m.getParameterCount() == 1) {
                get = m;
                break;
            }
        }
        if (get == null) throw new IllegalStateException("Storage#get(StoragePosition) not found");
        return get.invoke(storage, position);
    }

    private void invokeSetByPosition(Object storage, Object position, Object pokemonOrNull) throws Exception {
        Method set = null;
        for (Method m : storage.getClass().getMethods()) {
            if (!m.getName().equals("set")) continue;
            if (m.getParameterCount() == 2) {
                set = m;
                break;
            }
        }
        if (set == null) throw new IllegalStateException("Storage#set(StoragePosition, Pokemon) not found");
        set.invoke(storage, position, pokemonOrNull);
    }

    /** 온라인이면 파티 갱신 패킷 전송 */
    private void sendPartyUpdateIfOnline(UUID playerUuid, Object party) {
        try {
            Player p = Bukkit.getPlayer(playerUuid);
            if (p == null) return;
            // PlayerPartyStorage#sendClientUpdatePacket()
            Method m = party.getClass().getMethod("sendClientUpdatePacket");
            m.invoke(party);
        } catch (Throwable ignored) {}
    }

    /** 온라인이면 PC 갱신 패킷 전송 */
    private void sendPCUpdateIfOnline(UUID playerUuid, Object pc) {
        try {
            Player p = Bukkit.getPlayer(playerUuid);
            if (p == null) return;
            // PCStorage#sendContents(ServerPlayer) 가 있지만 NMS 타입이라 여기서는 호출 생략 (감사 루프/반환은 서버 저장에 반영됨)
            // 필요 시 Arclight 환경에서만 추가 훅 가능.
        } catch (Throwable ignored) {}
    }
}
