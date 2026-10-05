package dev.fightstats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.UUID;

/** Builds the chat output shown after a fight. */
public final class FightMessages {

    private FightMessages() {
    }

    private static Component row(String label, NamedTextColor labelColor, String mine, String theirs) {
        return Component.text()
                .append(Component.text(label + " ", labelColor))
                .append(Component.text(mine, NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text("  THEM: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(theirs, NamedTextColor.GRAY))
                .build();
    }

    /** The short summary sent when a fight ends in a kill. Written from {@code viewer}'s side. */
    public static Component summary(Fight fight, UUID viewer) {
        PlayerStats me = fight.stats(viewer);
        PlayerStats them = fight.stats(fight.opponent(viewer));

        return Component.text()
                .append(Component.newline())
                .append(row("CRYSTALS USED", NamedTextColor.LIGHT_PURPLE,
                        String.valueOf(me.crystals), String.valueOf(them.crystals)))
                .append(Component.newline())
                .append(row("ANCHORS USED", NamedTextColor.DARK_PURPLE,
                        String.valueOf(me.anchors), String.valueOf(them.anchors)))
                .append(Component.newline())
                .append(row("XP BOTTLES USED", NamedTextColor.YELLOW,
                        String.valueOf(me.xpBottles), String.valueOf(them.xpBottles)))
                .append(Component.newline())
                .append(row("ARMOR HEALTH TAKEN", NamedTextColor.AQUA,
                        String.valueOf(Math.round(me.armorDamage)),
                        String.valueOf(Math.round(them.armorDamage))))
                .append(Component.newline())
                .append(Component.text("View detailed fight breakdown", NamedTextColor.AQUA,
                                TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.runCommand("/fightbreakdown"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open the full breakdown"))))
                .build();
    }

    /** The long breakdown shown by /fightbreakdown. */
    public static Component breakdown(Fight fight, UUID viewer) {
        UUID otherId = fight.opponent(viewer);
        PlayerStats me = fight.stats(viewer);
        PlayerStats them = fight.stats(otherId);

        long end = fight.end != 0 ? fight.end : System.currentTimeMillis();
        double seconds = (end - fight.start) / 1000.0;

        String result;
        if (fight.winner == null) {
            result = "No winner";
        } else if (fight.winner.equals(viewer)) {
            result = "You won";
        } else {
            result = "You lost";
        }

        return Component.text()
                .append(Component.text("Fight breakdown vs " + fight.name(otherId), NamedTextColor.GOLD,
                        TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(result + " - lasted " + String.format("%.1f", seconds) + "s",
                        NamedTextColor.GRAY))
                .append(Component.newline())
                .append(row("DAMAGE DEALT", NamedTextColor.RED,
                        String.format("%.1f", me.damageDealt), String.format("%.1f", them.damageDealt)))
                .append(Component.newline())
                .append(row("HITS LANDED", NamedTextColor.RED,
                        String.valueOf(me.hits), String.valueOf(them.hits)))
                .append(Component.newline())
                .append(row("CRYSTALS USED", NamedTextColor.LIGHT_PURPLE,
                        String.valueOf(me.crystals), String.valueOf(them.crystals)))
                .append(Component.newline())
                .append(row("ANCHORS USED", NamedTextColor.DARK_PURPLE,
                        String.valueOf(me.anchors), String.valueOf(them.anchors)))
                .append(Component.newline())
                .append(row("XP BOTTLES USED", NamedTextColor.YELLOW,
                        String.valueOf(me.xpBottles), String.valueOf(them.xpBottles)))
                .append(Component.newline())
                .append(row("TOTEMS POPPED", NamedTextColor.GOLD,
                        String.valueOf(me.totems), String.valueOf(them.totems)))
                .append(Component.newline())
                .append(row("ARMOR HEALTH TAKEN", NamedTextColor.AQUA,
                        String.valueOf(Math.round(me.armorDamage)),
                        String.valueOf(Math.round(them.armorDamage))))
                .build();
    }
}
