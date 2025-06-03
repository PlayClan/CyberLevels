package net.zerotoil.dev.cyberlevels.objects.exp;

import net.zerotoil.dev.cyberlevels.CyberLevels;
import net.zerotoil.dev.cyberlevels.objects.antiabuse.AntiAbuse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class EXPCache {

    private final CyberLevels main;
    private final Map<String, EXPEarnEvent> expEarnEvents = new HashMap<>();
    private final Map<String, AntiAbuse> antiAbuse = new HashMap<>();
    private final boolean useDouble, roundExp;

    private boolean preventSilkTouchAbuse;
    private boolean onlyNaturalBlocks;
    private boolean includeNaturalCrops;

    private BukkitTask timedEXP;

    public EXPCache(CyberLevels main) {
        cancelTimedEXP();
        cancelAntiAbuseTimers();
        this.main = main;
        useDouble = main.files().getConfig("config").getBoolean("config.earn-exp.integer-only");
        roundExp = main.files().getConfig("config").getBoolean("config.round-evaluation.round-earn-exp");
        loadExpEvents();
        loadAntiAbuse();
        startTimedEXP();
    }

    public void loadExpEvents() {

        main.logger("&dLoading exp earning events...");
        if (!expEarnEvents.isEmpty()) expEarnEvents.clear();
        long startTime = System.currentTimeMillis();
        addEvent("damaging-players", "players");
        addEvent("damaging-animals", "animals");
        addEvent("damaging-monsters", "monsters");

        addEvent("killing-players", "players");
        addEvent("killing-animals", "animals");
        addEvent("killing-monsters", "monsters");

        addEvent("placing", "blocks");
        addEvent("breaking", "blocks");
        addEvent("consuming", "items");
        addEvent("moving", "permissions");
        addEvent("crafting", "items");
        addEvent("brewing", "potions");
        addEvent("enchanting", "enchantments");
        addEvent("fishing", "fish");
        addEvent("breeding", "animals");

        addEvent("dying", "permissions");
        addEvent("timed-giving", "permissions");
        addEvent("chatting", "words");
        addEvent("vanilla-exp-gain", "amounts");
        addEvent("rivalhh-breaking", "blocks");
        addEvent("rivalp-breaking", "blocks");

        long counter = 0;
        for (EXPEarnEvent event : expEarnEvents.values()) if (event.isEnabled() || event.isSpecificEnabled()) counter++;

        main.logger("&7Loaded &e" + counter + " &7exp earn events in &a" + (System.currentTimeMillis() - startTime) + "ms&7.", "");

    }

    public void loadAntiAbuse() {
        main.logger("&dLoading anti-abuse...");
        preventSilkTouchAbuse = !main.files().getConfig("anti-abuse").getBoolean("anti-abuse.general.silk-touch-reward", true);
        onlyNaturalBlocks = main.files().getConfig("anti-abuse").getBoolean("anti-abuse.general.only-natural-blocks", false);
        includeNaturalCrops = main.files().getConfig("anti-abuse").getBoolean("anti-abuse.general.include-natural-crops", false);
        if (!antiAbuse.isEmpty()) antiAbuse.clear();
        long startTime = System.currentTimeMillis();
        long counter = 0;
        if (main.files().getConfig("anti-abuse").isConfigurationSection("anti-abuse")) {
            for (String s : main.files().getConfig("anti-abuse").getConfigurationSection("anti-abuse").getKeys(false)) {
                if (s.equalsIgnoreCase("general")) continue;
                antiAbuse.put(s, new AntiAbuse(main, s));
                counter++;
            }
        }

        main.logger("&7Loaded &e" + counter + " &7anti-abuse settings in &a" + (System.currentTimeMillis() - startTime) + "ms&7.", "");
    }

    public boolean isAntiAbuse(Player player, String event) {
        for (AntiAbuse a : antiAbuse.values()) {
            if (a.isWorldLimited(player, event)) return true;
            else if (a.isCoolingDown(player, event)) return true;
            else if (a.isLimited(player, event)) return true;
        }
        return false;
    }

    public void cancelAntiAbuseTimers() {
        for (AntiAbuse a : antiAbuse.values()) a.cancelTimer();
    }

    public void cancelTimedEXP() {
        if (timedEXP == null) return;
        timedEXP.cancel();
        timedEXP = null;
    }

    public void startTimedEXP() {
        if (!main.files().getConfig("earn-exp").getBoolean("earn-exp.timed-giving.general.enabled", false) &&
                !main.files().getConfig("earn-exp").getBoolean("earn-exp.timed-giving.specific-permissions.enabled", false)) return;
        //if (timedEXP != null && !timedEXP.isCancelled()) return;
        long interval = Math.max(20L * Math.max(1, main.files().getConfig("earn-exp").getLong("earn-exp.timed-giving.general.interval")), 1);
        timedEXP = (new BukkitRunnable() {
            @Override
            public void run() {

                for (Player p : Bukkit.getOnlinePlayers()) main.expListeners().sendPermissionExp(p, expEarnEvents.get("timed-giving"));

            }
        }).runTaskTimer(main, interval, interval);
    }

    private void addEvent(String category, String name) {
        expEarnEvents.put(category, new EXPEarnEvent(main, category, name));
    }

    public Map<String, EXPEarnEvent> expEarnEvents() {
        return expEarnEvents;
    }

    public boolean useDouble() {
        return useDouble;
    }
    public boolean roundExp() {
        return roundExp;
    }

    public boolean isPreventSilkTouchAbuse() {
        return preventSilkTouchAbuse;
    }
    public boolean isOnlyNaturalBlocks() {
        return onlyNaturalBlocks;
    }
    public boolean isIncludeNaturalCrops() {
        return includeNaturalCrops;
    }

}
