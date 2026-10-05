package dev.fightstats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FightCommand implements CommandExecutor {

    private final FightManager manager;

    public FightCommand(FightManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Fight fight = manager.getLast(player.getUniqueId());
        if (fight == null) {
            fight = manager.get(player.getUniqueId());
        }
        if (fight == null) {
            player.sendMessage(Component.text("You have no recent fight to show.", NamedTextColor.RED));
            return true;
        }
        player.sendMessage(FightMessages.breakdown(fight, player.getUniqueId()));
        return true;
    }
}
