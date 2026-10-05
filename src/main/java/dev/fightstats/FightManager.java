package dev.fightstats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FightManager {
    private final Map<UUID, Fight> active = new HashMap<>();
    private final Map<UUID, Fight> last = new HashMap<>();

    public Fight get(UUID player) {
        return active.get(player);
    }

    public Fight getLast(UUID player) {
        return last.get(player);
    }

    /** Returns the fight between the two players, starting a new one if needed. */
    public Fight getOrCreate(UUID x, String nameX, UUID y, String nameY) {
        Fight existing = active.get(x);
        if (existing != null && existing.involves(y)) {
            return existing;
        }
        end(x, null);
        end(y, null);
        Fight fight = new Fight(x, nameX, y, nameY);
        active.put(x, fight);
        active.put(y, fight);
        return fight;
    }

    /** Ends the fight the player is in (if any) and remembers it for /fightbreakdown. */
    public Fight end(UUID player, UUID winner) {
        Fight fight = active.get(player);
        if (fight == null) {
            return null;
        }
        fight.end = System.currentTimeMillis();
        fight.winner = winner;
        active.remove(fight.a);
        active.remove(fight.b);
        last.put(fight.a, fight);
        last.put(fight.b, fight);
        return fight;
    }

    /** Ends fights that have had no activity for timeoutMs. */
    public void expire(long timeoutMs) {
        long now = System.currentTimeMillis();
        Set<Fight> distinct = new HashSet<>(active.values());
        List<Fight> expired = new ArrayList<>();
        for (Fight fight : distinct) {
            if (now - fight.lastActivity > timeoutMs) {
                expired.add(fight);
            }
        }
        for (Fight fight : expired) {
            end(fight.a, null);
        }
    }

    public void forget(UUID player) {
        end(player, null);
        last.remove(player);
    }
}
