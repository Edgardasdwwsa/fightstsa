package dev.fightstats;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FightListener implements Listener {

    private static final long ANCHOR_ATTRIBUTION_MS = 3000;
    private static final double ANCHOR_RADIUS_SQ = 12 * 12;

    private static class AnchorUse {
        final UUID owner;
        final World world;
        final Location location;
        final long time = System.currentTimeMillis();
        boolean counted;

        AnchorUse(UUID owner, Location location) {
            this.owner = owner;
            this.world = location.getWorld();
            this.location = location;
        }
    }

    private final FightManager manager;

    /** Which player last hit each end crystal (so crystal explosions can be credited). */
    private final Map<UUID, UUID> crystalOwner = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, UUID> eldest) {
            return size() > 512;
        }
    };

    private final List<AnchorUse> recentAnchors = new ArrayList<>();

    public FightListener(FightManager manager) {
        this.manager = manager;
    }

    // ---------------------------------------------------------------- damage

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            return p;
        }
        if (damager instanceof EnderCrystal crystal) {
            UUID owner = crystalOwner.get(crystal.getUniqueId());
            if (owner != null) {
                return Bukkit.getPlayer(owner);
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        // Player hits a crystal: remember who set it off.
        if (event.getEntity() instanceof EnderCrystal crystal) {
            crystalOwner.put(crystal.getUniqueId(), attacker.getUniqueId());
            Fight fight = manager.get(attacker.getUniqueId());
            if (fight != null) {
                countCrystal(fight.stats(attacker.getUniqueId()), crystal.getUniqueId());
                fight.touch();
            }
            return;
        }

        if (!(event.getEntity() instanceof Player victim) || victim.equals(attacker)) {
            return;
        }

        Fight fight = manager.getOrCreate(attacker.getUniqueId(), attacker.getName(),
                victim.getUniqueId(), victim.getName());
        PlayerStats stats = fight.stats(attacker.getUniqueId());
        stats.damageDealt += event.getFinalDamage();
        stats.hits++;
        if (event.getDamager() instanceof EnderCrystal crystal) {
            countCrystal(stats, crystal.getUniqueId());
        }
        fight.touch();
    }

    private void countCrystal(PlayerStats stats, UUID crystalId) {
        if (stats.countedCrystals.add(crystalId)) {
            stats.crystals++;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageByBlock(EntityDamageByBlockEvent event) {
        if (!(event.getEntity() instanceof Player victim)
                || event.getCause() != DamageCause.BLOCK_EXPLOSION) {
            return;
        }
        long now = System.currentTimeMillis();
        recentAnchors.removeIf(use -> now - use.time > ANCHOR_ATTRIBUTION_MS);

        AnchorUse match = null;
        for (AnchorUse use : recentAnchors) {
            if (use.owner.equals(victim.getUniqueId()) || !use.world.equals(victim.getWorld())) {
                continue;
            }
            if (use.location.distanceSquared(victim.getLocation()) <= ANCHOR_RADIUS_SQ) {
                match = use;
            }
        }
        if (match == null) {
            return;
        }
        Player attacker = Bukkit.getPlayer(match.owner);
        if (attacker == null) {
            return;
        }

        Fight fight = manager.getOrCreate(attacker.getUniqueId(), attacker.getName(),
                victim.getUniqueId(), victim.getName());
        PlayerStats stats = fight.stats(attacker.getUniqueId());
        stats.damageDealt += event.getFinalDamage();
        stats.hits++;
        if (!match.counted) {
            match.counted = true;
            stats.anchors++;
        }
        fight.touch();
    }

    // ---------------------------------------------------------------- item use

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.RESPAWN_ANCHOR
                || block.getWorld().getEnvironment() == World.Environment.NETHER) {
            return;
        }
        if (!(block.getBlockData() instanceof RespawnAnchor anchor) || anchor.getCharges() <= 0) {
            return;
        }
        // Right-clicking with glowstone on a non-full anchor charges it instead of exploding it.
        ItemStack hand = event.getItem();
        boolean charging = hand != null && hand.getType() == Material.GLOWSTONE
                && anchor.getCharges() < anchor.getMaximumCharges();
        if (charging) {
            return;
        }

        Player player = event.getPlayer();
        AnchorUse use = new AnchorUse(player.getUniqueId(), block.getLocation().add(0.5, 0.5, 0.5));
        recentAnchors.add(use);

        Fight fight = manager.get(player.getUniqueId());
        if (fight != null) {
            use.counted = true;
            fight.stats(player.getUniqueId()).anchors++;
            fight.touch();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof ThrownExpBottle bottle)
                || !(bottle.getShooter() instanceof Player player)) {
            return;
        }
        Fight fight = manager.get(player.getUniqueId());
        if (fight != null) {
            fight.stats(player.getUniqueId()).xpBottles++;
            fight.touch();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorDamage(PlayerItemDamageEvent event) {
        String type = event.getItem().getType().name();
        boolean armor = type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE")
                || type.endsWith("_LEGGINGS") || type.endsWith("_BOOTS");
        if (!armor) {
            return;
        }
        Fight fight = manager.get(event.getPlayer().getUniqueId());
        if (fight != null) {
            fight.stats(event.getPlayer().getUniqueId()).armorDamage += event.getDamage();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotem(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Fight fight = manager.get(player.getUniqueId());
        if (fight != null) {
            fight.stats(player.getUniqueId()).totems++;
            fight.touch();
        }
    }

    // ---------------------------------------------------------------- results

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Fight fight = manager.get(victim.getUniqueId());
        if (fight == null) {
            return;
        }
        UUID killerId = fight.opponent(victim.getUniqueId());
        manager.end(victim.getUniqueId(), killerId);

        Player killer = Bukkit.getPlayer(killerId);
        if (killer != null) {
            killer.sendMessage(FightMessages.summary(fight, killerId));
        }
        victim.sendMessage(FightMessages.summary(fight, victim.getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.forget(event.getPlayer().getUniqueId());
    }
}
