package com.minpack.rental.data;

import java.util.UUID;

public final class Listing {
    public long id;
    public UUID owner;
    public String ownerName;
    public String pokemonUuid;
    public String pokemonNbtB64;
    public double price;
    public long rentalDurationSeconds;
    public long createdEpochSec;
    public long expireEpochSec;

    public boolean isExpired(long now) { return now >= expireEpochSec; }
}
