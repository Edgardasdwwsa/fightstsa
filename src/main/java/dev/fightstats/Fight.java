package dev.fightstats;

import java.util.UUID;

/** A fight between exactly two players. */
public class Fight {
    public final UUID a;
    public final UUID b;
    public final String nameA;
    public final String nameB;
    public final PlayerStats statsA = new PlayerStats();
    public final PlayerStats statsB = new PlayerStats();
    public final long start = System.currentTimeMillis();
    public long lastActivity = start;
    public long end;
    public UUID winner;

    public Fight(UUID a, String nameA, UUID b, String nameB) {
        this.a = a;
        this.nameA = nameA;
        this.b = b;
        this.nameB = nameB;
    }

    public boolean involves(UUID id) {
        return a.equals(id) || b.equals(id);
    }

    public UUID opponent(UUID id) {
        return a.equals(id) ? b : a;
    }

    public PlayerStats stats(UUID id) {
        return a.equals(id) ? statsA : statsB;
    }

    public String name(UUID id) {
        return a.equals(id) ? nameA : nameB;
    }

    public void touch() {
        lastActivity = System.currentTimeMillis();
    }
}
