package dev.fightstats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Builds the chat output shown after a fight. Red and white theme. */
public final class FightMessages {

    /** Shown in the header. Set from config.yml ("title"). */
    public static String title = "LASTED SURVIVAL";

    private static final String DIVIDER = "━━━━━━━━━━━━━━━━━━━━━━";

    // Small icons that exist in Minecraft's built-in font (no resource pack needed).
    private static final String ICON_CRYSTAL = "✦"; // ✦
    private static final String ICON_ANCHOR = "⚓";  // ⚓
    private static final String ICON_XP = "⚗";      // ⚗
    private static final String ICON_ARMOR = "❖";   // ❖
    private static final String ICON_DAMAGE = "⚔";  // ⚔
    private static final String ICON_HITS = "➤";    // ➤
    private static final String ICON_TOTEM = "✪";   // ✪
    private static final String ICON_GAPPLE = "♦";  // ♦
    private static final String ICON_PEARL = "◎";   // ◎
    private static final String ICON_TIME = "⌚";    // ⌚
    private static final String ICON_HEALTH = "❤";  // ❤
    private static final String ICON_SKULL = "☠";   // ☠

    private FightMessages() {
    }

    private static Component divider() {
        return Component.text(DIVIDER, NamedTextColor.RED, TextDecoration.STRIKETHROUGH);
    }

    private static Component header(String subtitle) {
        List<Component> lines = new ArrayList<>();
        lines.add(divider());
        lines.add(Component.text()
                .append(Component.text(ICON_SKULL + " ", NamedTextColor.RED))
                .append(Component.text(title, NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.text(" " + ICON_SKULL, NamedTextColor.RED))
                .build());
        if (subtitle != null) {
            lines.add(Component.text(subtitle, NamedTextColor.GRAY));
        }
        lines.add(divider());
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    private static Component row(String icon, String label, String mine, String theirs) {
        return Component.text()
                .append(Component.text(icon + " ", NamedTextColor.RED))
                .append(Component.text(label + " ", NamedTextColor.WHITE))
                .append(Component.text(mine, NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("  THEM ", NamedTextColor.GRAY))
                .append(Component.text(theirs, NamedTextColor.WHITE))
                .build();
    }

    private static String n(int value) {
        return String.valueOf(value);
    }

    private static String d(double value) {
        return String.format("%.1f", value);
    }

    private static String seconds(Fight fight) {
        long end = fight.end != 0 ? fight.end : System.currentTimeMillis();
        return String.format("%.1fs", (end - fight.start) / 1000.0);
    }

    /** The summary sent when a fight ends in a kill. Written from {@code viewer}'s side. */
    public static Component summary(Fight fight, UUID viewer) {
        PlayerStats me = fight.stats(viewer);
        PlayerStats them = fight.stats(fight.opponent(viewer));

        List<Component> lines = new ArrayList<>();
        lines.add(header("vs " + fight.name(fight.opponent(viewer))));
        lines.add(row(ICON_CRYSTAL, "CRYSTALS USED", n(me.crystals), n(them.crystals)));
        lines.add(row(ICON_ANCHOR, "ANCHORS USED", n(me.anchors), n(them.anchors)));
        lines.add(row(ICON_XP, "XP BOTTLES USED", n(me.xpBottles), n(them.xpBottles)));
        lines.add(row(ICON_ARMOR, "ARMOR HEALTH TAKEN",
                n((int) Math.round(me.armorDamage)), n((int) Math.round(them.armorDamage))));
        lines.add(row(ICON_DAMAGE, "DAMAGE DEALT", d(me.damageDealt), d(them.damageDealt)));
        lines.add(row(ICON_TOTEM, "TOTEMS POPPED", n(me.totems), n(them.totems)));
        lines.add(Component.text()
                .append(Component.text("» ", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("View detailed fight breakdown", NamedTextColor.WHITE,
                        TextDecoration.UNDERLINED))
                .clickEvent(ClickEvent.runCommand("/fightbreakdown"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open the full breakdown",
                        NamedTextColor.RED)))
                .build());
        lines.add(divider());
        return Component.join(JoinConfiguration.newlines(), lines);
    }

    /** The long breakdown shown by /fightbreakdown. */
    public static Component breakdown(Fight fight, UUID viewer) {
        UUID otherId = fight.opponent(viewer);
        PlayerStats me = fight.stats(viewer);
        PlayerStats them = fight.stats(otherId);

        Component result;
        if (fight.winner == null) {
            result = Component.text("NO WINNER", NamedTextColor.GRAY, TextDecoration.BOLD);
        } else if (fight.winner.equals(viewer)) {
            result = Component.text("VICTORY", NamedTextColor.WHITE, TextDecoration.BOLD);
        } else {
            result = Component.text("DEFEAT", NamedTextColor.RED, TextDecoration.BOLD);
        }

        List<Component> lines = new ArrayList<>();
        lines.add(header("Fight breakdown vs " + fight.name(otherId)));
        lines.add(Component.text()
                .append(Component.text(ICON_TIME + " ", NamedTextColor.RED))
                .append(result)
                .append(Component.text("  lasted ", NamedTextColor.GRAY))
                .append(Component.text(seconds(fight), NamedTextColor.WHITE))
                .build());
        lines.add(row(ICON_DAMAGE, "DAMAGE DEALT", d(me.damageDealt), d(them.damageDealt)));
        lines.add(row(ICON_HITS, "HITS LANDED", n(me.hits), n(them.hits)));
        lines.add(row(ICON_CRYSTAL, "CRYSTALS USED", n(me.crystals), n(them.crystals)));
        lines.add(row(ICON_ANCHOR, "ANCHORS USED", n(me.anchors), n(them.anchors)));
        lines.add(row(ICON_XP, "XP BOTTLES USED", n(me.xpBottles), n(them.xpBottles)));
        lines.add(row(ICON_TOTEM, "TOTEMS POPPED", n(me.totems), n(them.totems)));
        lines.add(row(ICON_GAPPLE, "GAPPLES EATEN", n(me.gapples), n(them.gapples)));
        lines.add(row(ICON_PEARL, "PEARLS THROWN", n(me.pearls), n(them.pearls)));
        lines.add(row(ICON_ARMOR, "ARMOR HEALTH TAKEN",
                n((int) Math.round(me.armorDamage)), n((int) Math.round(them.armorDamage))));
        if (fight.winner != null && fight.winnerHealth >= 0) {
            String who = fight.winner.equals(viewer) ? "You" : fight.name(fight.winner);
            lines.add(Component.text()
                    .append(Component.text(ICON_HEALTH + " ", NamedTextColor.RED))
                    .append(Component.text("WINNER HEALTH LEFT ", NamedTextColor.WHITE))
                    .append(Component.text(d(fight.winnerHealth / 2.0) + " hearts", NamedTextColor.RED,
                            TextDecoration.BOLD))
                    .append(Component.text("  (" + who + ")", NamedTextColor.GRAY))
                    .build());
        }
        lines.add(divider());
        return Component.join(JoinConfiguration.newlines(), lines);
    }
}
