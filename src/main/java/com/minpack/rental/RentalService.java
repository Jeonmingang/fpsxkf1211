package com.minpack.rental.service;

import com.minpack.rental.PixelmonRentalMarketPlugin;
import com.minpack.rental.bridge.PokemonNbt;
import com.minpack.rental.data.ActiveRental;
import com.minpack.rental.data.Listing;
import com.minpack.rental.data.RentalRepository;
import com.minpack.rental.util.Msg;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public final class RentalService {

    private final PixelmonRentalMarketPlugin plugin;

    private long nextId = 1;
    private final Map<Long, Listing> listings = new LinkedHashMap<>();
    private final Map<Long, ActiveRental> active = new LinkedHashMap<>();
    private final Map<String, Long> idxListingPokemon = new HashMap<>();
    private final Map<String, Long> idxActivePokemon = new HashMap<>();

    // [추가] 유저별 알림 설정 (UUID -> 초 단위 간격, 0이면 끔)
    private final Map<UUID, Integer> notificationSettings = new HashMap<>();

    public RentalService(PixelmonRentalMarketPlugin plugin) {
        this.plugin = plugin;
    }

    private long now() { return System.currentTimeMillis() / 1000L; }

    public void loadFromRepository(RentalRepository repo) {
        listings.clear(); active.clear();
        idxListingPokemon.clear(); idxActivePokemon.clear();

        listings.putAll(repo.listings);
        active.putAll(repo.active);

        long max = 0;
        for (Long id : listings.keySet()) max = Math.max(max, id);
        for (Long id : active.keySet()) max = Math.max(max, id);
        nextId = Math.max(repo.peekNextId(), max + 1);

        for (Listing l : listings.values()) idxListingPokemon.put(l.pokemonUuid, l.id);
        for (ActiveRental r : active.values()) idxActivePokemon.put(r.pokemonUuid, r.id);
    }

    public void flushToRepository(RentalRepository repo) {
        repo.listings.clear();
        repo.active.clear();
        repo.listings.putAll(listings);
        repo.active.putAll(active);
        repo.setNextId(nextId);
    }

    public Collection<Listing> getListings() { return listings.values(); }

    public List<Listing> getListingsPage(int page, int pageSize) {
        List<Listing> all = new ArrayList<>(listings.values());
        int from = Math.max(0, page * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        if (from >= to) return Collections.emptyList();
        return all.subList(from, to);
    }

    public List<Listing> getOwnerListings(UUID owner) {
        List<Listing> out = new ArrayList<>();
        for (Listing l : listings.values()) if (l.owner.equals(owner)) out.add(l);
        return out;
    }

    // [추가] 특정 유저가 빌리고 있는 포켓몬 목록 조회 (GUI용)
    public List<ActiveRental> getRenterRentals(UUID renter) {
        List<ActiveRental> out = new ArrayList<>();
        for (ActiveRental r : active.values()) if (r.renter.equals(renter)) out.add(r);
        return out;
    }

    public boolean hasAnyActiveRental(UUID renter) {
        for (ActiveRental r : active.values()) if (r.renter.equals(renter)) return true;
        return false;
    }

    public boolean isPokemonListedOrActive(String pokemonUuid) {
        return idxListingPokemon.containsKey(pokemonUuid) || idxActivePokemon.containsKey(pokemonUuid);
    }

    // [추가] 알림 간격 설정 (0 = 끔)
    public void setNotificationInterval(Player p, int seconds) {
        if (seconds <= 0) notificationSettings.remove(p.getUniqueId());
        else notificationSettings.put(p.getUniqueId(), seconds);
    }

    public int getNotificationInterval(UUID uuid) {
        // 기본값 600초(10분), 설정 없으면 기본값 사용
        return notificationSettings.getOrDefault(uuid, 600);
    }

    public Listing registerListing(Player owner, int slot0, long rentalDurationSeconds, double price) throws Exception {
        Object pokemon = plugin.getPixelmon().getPartyPokemon(owner.getUniqueId(), slot0);
        if (pokemon == null) return null;

        String pokeUuid = plugin.getPixelmon().getPokemonUuidString(pokemon);
        if (pokeUuid.isEmpty()) throw new IllegalStateException("Pokemon UUID not found");

        if (isPokemonListedOrActive(pokeUuid)) throw new IllegalStateException("Already listed/active");

        String b64 = PokemonNbt.toBase64(pokemon);
        plugin.getPixelmon().setPartyPokemon(owner.getUniqueId(), slot0, null);

        Listing l = new Listing();
        l.id = nextId++;
        l.owner = owner.getUniqueId();
        l.ownerName = owner.getName();
        l.pokemonUuid = pokeUuid;
        l.pokemonNbtB64 = b64;
        l.price = price;
        l.rentalDurationSeconds = rentalDurationSeconds;
        l.createdEpochSec = now();

        long life = plugin.getConfig().getLong("market.listing_lifetime_seconds", 3600);
        l.expireEpochSec = l.createdEpochSec + Math.max(30, life);

        listings.put(l.id, l);
        idxListingPokemon.put(l.pokemonUuid, l.id);

        if (plugin.getConfig().getBoolean("discord.notify_on_register", true)) {
            plugin.getDiscord().sendAsync("🟦 렌탈 등록: ID " + l.id + " / " + owner.getName() + " / 가격 " + price + " / 대여 " + rentalDurationSeconds + "초");
        }

        return l;
    }

    public boolean cancelListing(Player owner, long id) throws Exception {
        Listing l = listings.get(id);
        if (l == null) return false;
        if (!l.owner.equals(owner.getUniqueId()) && !owner.hasPermission("rental.admin")) return false;

        listings.remove(id);
        idxListingPokemon.remove(l.pokemonUuid);

        Object pokemon = PokemonNbt.fromBase64(l.pokemonNbtB64);
        plugin.getPixelmon().addToPartyOrPC(l.owner, pokemon);
        return true;
    }

    public boolean buy(Player buyer, long id) throws Exception {
        Listing l = listings.get(id);
        if (l == null) return false;
        if (l.owner.equals(buyer.getUniqueId())) return false;

        Economy eco = plugin.getEconomy();
        if (plugin.getConfig().getBoolean("economy.require_vault", true)) {
            if (eco == null) throw new IllegalStateException("Vault economy not available");
            if (!eco.has(buyer, l.price)) return false;
        }

        if (eco != null) {
            eco.withdrawPlayer(buyer, l.price);
            OfflinePlayer owner = Bukkit.getOfflinePlayer(l.owner);
            eco.depositPlayer(owner, l.price);
        }

        Object pokemon = PokemonNbt.fromBase64(l.pokemonNbtB64);
        plugin.getPixelmon().addToPartyOrPC(buyer.getUniqueId(), pokemon);

        ActiveRental r = new ActiveRental();
        r.id = l.id;
        r.owner = l.owner;
        // ActiveRental에는 ownerName 필드가 없으므로 저장하지 않음 (필요 시 조회)
        r.renter = buyer.getUniqueId();
        r.renterName = buyer.getName();
        r.pokemonUuid = l.pokemonUuid;
        r.pokemonNbtB64 = l.pokemonNbtB64;
        r.price = l.price;
        r.startEpochSec = now();
        r.endEpochSec = r.startEpochSec + Math.max(30, l.rentalDurationSeconds);

        active.put(r.id, r);
        idxActivePokemon.put(r.pokemonUuid, r.id);

        listings.remove(id);
        idxListingPokemon.remove(l.pokemonUuid);

        if (plugin.getConfig().getBoolean("discord.notify_on_buy", true)) {
            plugin.getDiscord().sendAsync("🟩 렌탈 구매: ID " + id + " / 대여자 " + buyer.getName() + " / 가격 " + l.price);
        }

        return true;
    }

    public void tickExpiry() {
        long t = now();

        // 1. 만료된 매물 처리
        List<Long> expiredListings = new ArrayList<>();
        for (Listing l : listings.values()) if (l.isExpired(t)) expiredListings.add(l.id);
        for (Long id : expiredListings) {
            Listing l = listings.remove(id);
            if (l == null) continue;
            idxListingPokemon.remove(l.pokemonUuid);
            try {
                Object pokemon = PokemonNbt.fromBase64(l.pokemonNbtB64);
                plugin.getPixelmon().addToPartyOrPC(l.owner, pokemon);
                if (plugin.getConfig().getBoolean("discord.notify_on_return", true)) {
                    plugin.getDiscord().sendAsync("🟨 렌탈 만료 반환: ID " + id + " / 주인 " + l.ownerName);
                }
            } catch (Exception ignored) {}
        }

        // 2. 만료된 렌탈 종료
        List<Long> ended = new ArrayList<>();
        for (ActiveRental r : active.values()) if (!r.finished && r.isEnded(t)) ended.add(r.id);
        for (Long id : ended) {
            try { endRental(id); } catch (Exception ignored) {}
        }

        // [추가] 3. 알림 메시지 전송 체크 (매 초 실행됨)
        tickNotifications(t);
    }

    // [추가] 알림 로직
    private void tickNotifications(long now) {
        for (ActiveRental r : active.values()) {
            if (r.finished) continue;

            int interval = getNotificationInterval(r.renter);
            if (interval <= 0) continue; // 알림 끔

            long remaining = r.endEpochSec - now;
            if (remaining <= 0) continue;

            // 설정한 시간 간격마다 알림
            if (remaining % interval == 0) {
                Player p = Bukkit.getPlayer(r.renter);
                if (p != null && p.isOnline()) {
                    String ownerName = Bukkit.getOfflinePlayer(r.owner).getName();
                    if (ownerName == null) ownerName = "Unknown";

                    p.sendMessage("");
                    p.sendMessage(Msg.color("&b[렌탈 알림] &f포켓몬 대여 정보"));
                    p.sendMessage(Msg.color("&7 - 주인: &e" + ownerName));
                    p.sendMessage(Msg.color("&7 - 남은 시간: &c" + formatTime(remaining)));
                    p.sendMessage(Msg.color("&7 - 비용: &a" + r.price + "원"));
                    p.sendMessage("");
                }
            }
        }
    }

    private String formatTime(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (h > 0) return h + "시간 " + m + "분";
        return m + "분 " + s + "초";
    }

    public void endRental(long id) throws Exception {
        ActiveRental r = active.get(id);
        if (r == null || r.finished) return;

        // 대여자가 파티/PC 어디에 넣었든 전부 회수
        removePokemonByUuidFromParty(r.renter, r.pokemonUuid);
        plugin.getPixelmon().removeFromPCByUuid(r.renter, r.pokemonUuid);
        Object pokemon = PokemonNbt.fromBase64(r.pokemonNbtB64);
        plugin.getPixelmon().addToPartyOrPC(r.owner, pokemon);

        r.finished = true;
        active.remove(id);
        idxActivePokemon.remove(r.pokemonUuid);

        // 메시지 전송
        Player owner = Bukkit.getPlayer(r.owner);
        if (owner != null) owner.sendMessage(Msg.color("&a[렌탈] &f대여해준 포켓몬(ID:" + id + ")이 반환되었습니다."));

        Player renter = Bukkit.getPlayer(r.renter);
        if (renter != null) renter.sendMessage(Msg.color("&c[렌탈] &f대여 기간이 만료되어 포켓몬(ID:" + id + ")이 회수되었습니다."));

        if (plugin.getConfig().getBoolean("discord.notify_on_return", true)) {
            plugin.getDiscord().sendAsync("🟨 렌탈 종료 반환: ID " + id);
        }
    }

    private void removePokemonByUuidFromParty(UUID uuid, String pokeUuid) {
        try {
            for (int i = 0; i < 6; i++) {
                Object p = plugin.getPixelmon().getPartyPokemon(uuid, i);
                if (p == null) continue;
                if (pokeUuid.equals(plugin.getPixelmon().getPokemonUuidString(p))) {
                    plugin.getPixelmon().setPartyPokemon(uuid, i, null);
                }
            }
        } catch (Exception ignored) {}
    }

    public void auditAndEnforce() {
        // 강제 되돌림 (EV/IV/레벨/경험치/왕관/교배로 변형되더라도 원복)
        for (ActiveRental r : new ArrayList<>(active.values())) {
            try { enforceOne(r); } catch (Exception ignored) {}
        }
    }

    private void enforceOne(ActiveRental r) throws Exception {
        // 렌탈 포켓몬이 파티/PC에서 이동되거나 변형되어도 "항상" 스냅샷 상태로 유지
        int slotFound = -1;
        for (int i = 0; i < 6; i++) {
            Object p = plugin.getPixelmon().getPartyPokemon(r.renter, i);
            if (p == null) continue;
            if (!r.pokemonUuid.equals(plugin.getPixelmon().getPokemonUuidString(p))) continue;

            if (slotFound < 0) {
                slotFound = i;
            } else {
                // 듀프 방지: 동일 UUID가 여러 슬롯에 있으면 추가분 제거
                plugin.getPixelmon().setPartyPokemon(r.renter, i, null);
            }
        }

        Object snapshot = PokemonNbt.fromBase64(r.pokemonNbtB64);

        // [보안 강화] 온라인 유저 전체에서 동일 UUID 듀프를 제거
        // - 트레이드/드랍/기타 방법으로 다른 사람에게 넘어간 경우(온라인 한정) 즉시 회수
        // - 오프라인 유저의 PC까지 전수검사는 비용이 크므로 여기서는 온라인만 처리
        try {
            for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
                java.util.UUID ou = online.getUniqueId();
                if (ou.equals(r.renter)) continue;
                plugin.getPixelmon().removeFromPartyByUuid(ou, r.pokemonUuid);
                plugin.getPixelmon().removeFromPCByUuid(ou, r.pokemonUuid);
            }
        } catch (Throwable ignored) {}

        // 듀프/숨김 방지: PC에 같은 UUID가 남아있으면 제거
        plugin.getPixelmon().removeFromPCByUuid(r.renter, r.pokemonUuid);

        if (slotFound >= 0) {
            // 파티에 있으면 해당 슬롯을 스냅샷으로 덮어쓰기
            plugin.getPixelmon().setPartyPokemon(r.renter, slotFound, snapshot);
            return;
        }

        // 파티에서 사라졌으면(PC로 옮김/삭제 시도 등) 스냅샷을 다시 지급
        plugin.getPixelmon().addToPartyOrPC(r.renter, snapshot);
    }
}