package com.minpack.rental.data;

import java.util.UUID;

public final class ActiveRental {
    public long id;
    public UUID owner;
    public UUID renter;
    public String renterName;
    public String pokemonUuid;
    public String pokemonNbtB64;
    public double price;
    public long startEpochSec;
    public long endEpochSec;
    public boolean finished = false;

    public boolean isEnded(long now) { return now >= endEpochSec; }
}
