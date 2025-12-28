// ... (imports)
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon; // import 추가

public final class RentalService {
    // ... (기존 필드 유지)

    public Listing registerListing(Player owner, int slot0, long rentalDurationSeconds, double price) throws Exception {
        Object pokemonObj = plugin.getPixelmon().getPartyPokemon(owner.getUniqueId(), slot0);
        if (pokemonObj == null) return null;
        
        // [보안] Untradeable 플래그 추가
        if (pokemonObj instanceof Pokemon p) {
            p.addFlag("untradeable"); // 교환 불가
            p.addFlag("unbreedable"); // 교배 불가
        }

        // ... (이하 기존 registerListing 로직과 동일: NBT 저장, Map 등록 등) ...
        // 주의: NBT 저장(toBase64)은 위에서 Flag를 추가한 '후'에 해야, 복구될 때도 Flag가 유지됩니다.
    }

    public void endRental(long id) throws Exception {
        ActiveRental r = active.get(id);
        if (r == null || r.finished) return;

        // ... (파티에서 삭제 로직) ...

        // 복구 시 Flag 제거
        Object pokemonObj = PokemonNbt.fromBase64(r.pokemonNbtB64);
        if (pokemonObj instanceof Pokemon p) {
            p.removeFlag("untradeable"); //
            p.removeFlag("unbreedable"); //
        }
        
        plugin.getPixelmon().addToPartyOrPC(r.owner, pokemonObj);
        // ... (종료 처리) ...
    }
    
    // cancelListing 메서드에서도 동일하게 removeFlag를 해줘야 원주인이 다시 교환할 수 있습니다.
    public boolean cancelListing(Player owner, long id) throws Exception {
        // ... (매물 조회) ...
        
        Object pokemonObj = PokemonNbt.fromBase64(l.pokemonNbtB64);
        if (pokemonObj instanceof Pokemon p) {
             p.removeFlag("untradeable");
             p.removeFlag("unbreedable");
        }
        
        plugin.getPixelmon().addToPartyOrPC(l.owner, pokemonObj);
        // ... (삭제 처리) ...
    }
}
