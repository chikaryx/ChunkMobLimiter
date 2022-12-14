package net.claimworld.chunkmoblimiter;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static org.bukkit.Bukkit.*;

public final class ChunkMobLimiter extends JavaPlugin implements Listener, CommandExecutor {

    private int totalChunkMobLimit;
    private boolean logChanges;
    private final List<CreatureSpawnEvent.SpawnReason> limitedSpawnReasons = new ArrayList<>();

    private void logEvents(EntityType entityType, Location location) {
        if (!logChanges) return;

        World world = location.getWorld();
        assert world != null;

        getLogger().log(Level.INFO, "Cancelled spawn of " + entityType + " at " + Math.round(location.getX()) + "x, " + Math.round(location.getY()) + "y, " + Math.round(location.getZ()) + "z, " + world.getName());
    }

    private void updateData(CommandSender sender) {
        getScheduler().runTaskAsynchronously(this, () -> {
            String logChangesName = "log_changes";
            String globalMobLimitName = "global_mob_limit";
            String limitedSpawnReasonsName = "limited_spawn_reasons";

            reloadConfig();
            FileConfiguration config = getConfig();
            totalChunkMobLimit = config.getInt("settings." + globalMobLimitName);
            logChanges = config.getBoolean("settings." + logChangesName);

            limitedSpawnReasons.clear();

            for (String value : config.getStringList("settings." + limitedSpawnReasonsName)) {
                CreatureSpawnEvent.SpawnReason spawnReason = CreatureSpawnEvent.SpawnReason.valueOf(value);
                limitedSpawnReasons.add(spawnReason);
            }

            getLogger().log(Level.INFO, "Data has been updated to values: [" + globalMobLimitName + ": " + totalChunkMobLimit + "], [" + logChangesName + ": " + logChanges + "], [" + limitedSpawnReasonsName + ": " + limitedSpawnReasons + "].");
            if (sender instanceof Player) sender.sendMessage("Data bas been successfully reloaded. Check console for details.");
        });
    }

    @EventHandler
    private void spawnEvent(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (!limitedSpawnReasons.contains(reason)) return;

        Location location = event.getLocation();
        if (location.getChunk().getEntities().length <= totalChunkMobLimit) return;

        event.setCancelled(true);

        getScheduler().runTaskAsynchronously(this, () -> logEvents(event.getEntityType(), location));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        updateData(sender);
        return true;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("reload-chunkmoblimiter").setExecutor(this);
        getPluginManager().registerEvents(this, this);

        updateData(getConsoleSender());
    }
}
