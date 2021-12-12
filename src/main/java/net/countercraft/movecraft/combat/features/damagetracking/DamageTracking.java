package net.countercraft.movecraft.combat.features.damagetracking;

import net.countercraft.movecraft.combat.event.CollisionDamagePlayerCraftEvent;
import net.countercraft.movecraft.combat.features.damagetracking.events.CraftDamagedByEvent;
import net.countercraft.movecraft.combat.features.damagetracking.events.CraftReleasedByEvent;
import net.countercraft.movecraft.combat.features.damagetracking.events.CraftSunkByEvent;
import net.countercraft.movecraft.combat.features.damagetracking.types.DamageType;
import net.countercraft.movecraft.combat.features.damagetracking.types.TorpedoDamage;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftSinkEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageTracking implements Listener {
    public static boolean EnableFireballTracking = false;
    public static boolean EnableTNTTracking = true;
    public static boolean EnableTorpedoTracking = false;
    public static int DamageTimeout = 300;

    public static void load(@NotNull FileConfiguration config) {
        EnableFireballTracking = config.getBoolean("EnableFireballTracking", false);
        EnableTNTTracking = config.getBoolean("EnableTNTTracking", true);
        EnableTorpedoTracking = config.getBoolean("EnableTorpedoTracking", false);
        DamageTimeout = config.getInt("DamageTimeout", 300);
    }

    @Deprecated(forRemoval = true)
    private static DamageTracking instance;

    @Nullable @Deprecated(forRemoval = true)
    public static DamageTracking getInstance() {
        return instance;
    }


    private final Map<PlayerCraft, List<DamageRecord>> damageRecords = new HashMap<>();

    public DamageTracking() {
        instance = this;
    }

    public void addDamageRecord(@NotNull PlayerCraft craft, @NotNull Player cause, @NotNull DamageType type) {
        DamageRecord damageRecord = new DamageRecord(craft.getPilot(), cause, type);
        Bukkit.getServer().getPluginManager().callEvent(new CraftDamagedByEvent(craft, damageRecord));

        if(damageRecords.containsKey(craft)) {
            List<DamageRecord> records = damageRecords.get(craft);
            records.add(damageRecord);
        }
        else {
            List<DamageRecord> records = new ArrayList<>();
            records.add(damageRecord);
            damageRecords.put(craft, records);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftRelease(@NotNull CraftReleaseEvent e) {
        if(!(e.getCraft() instanceof PlayerCraft))
            return;

        PlayerCraft craft = (PlayerCraft) e.getCraft();
        if(!damageRecords.containsKey(craft))
            return;

        List<DamageRecord> records = damageRecords.get(craft);
        if(records.isEmpty()) {
            damageRecords.remove(craft);
            return;
        }

        // Call event
        CraftReleasedByEvent event = new CraftReleasedByEvent(craft, records);
        Bukkit.getServer().getPluginManager().callEvent(event);

        damageRecords.remove(craft);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftSink(@NotNull CraftSinkEvent e) {
        if(!(e.getCraft() instanceof PlayerCraft))
            return;

        PlayerCraft craft = (PlayerCraft) e.getCraft();
        if(!damageRecords.containsKey(craft))
            return;

        List<DamageRecord> records = damageRecords.get(craft);
        if(records.isEmpty()) {
            damageRecords.remove(craft);
            return;
        }

        // Set last damage record as kill shot
        records.get(records.size() - 1).setKillShot(true);

        // Call event
        CraftSunkByEvent event = new CraftSunkByEvent(craft, records);
        Bukkit.getServer().getPluginManager().callEvent(event);

        Bukkit.broadcastMessage(event.causesToString());
        damageRecords.remove(craft);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCollisionDamagePlayerCraft(@NotNull CollisionDamagePlayerCraftEvent e) {
        addDamageRecord(e.getDamaged(), e.getDamaging().getPilot(), new TorpedoDamage());
    }
}
