package dev.fightstats;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Counters for one player inside one fight. */
public class PlayerStats {
    public int crystals;
    public int anchors;
    public int xpBottles;
    public int hits;
    public int totems;
    public int gapples;
    public int pearls;
    public double armorDamage;
    public double damageDealt;

    /** Crystal entity IDs already counted so one crystal is never counted twice. */
    public final Set<UUID> countedCrystals = new HashSet<>();
}
