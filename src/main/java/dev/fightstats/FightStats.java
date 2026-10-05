package dev.fightstats;

import org.bukkit.plugin.java.JavaPlugin;

public class FightStats extends JavaPlugin {

    private FightManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new FightManager();

        getServer().getPluginManager().registerEvents(new FightListener(manager), this);
        getCommand("fightbreakdown").setExecutor(new FightCommand(manager));

        long timeoutMs = getConfig().getLong("fight-timeout-seconds", 20) * 1000L;
        getServer().getScheduler().runTaskTimer(this, () -> manager.expire(timeoutMs), 20L, 20L);
    }
}
