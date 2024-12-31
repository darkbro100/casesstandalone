package me.paul;

import io.github.miniplaceholders.api.Expansion;
import lombok.Getter;
import me.paul.cmd.*;
import me.paul.util.*;
import me.paul.util.gui.listener.GuiListener;
import me.paul.util.scheduler.Sync;
import me.paul.util.scheduler.TaskBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.util.Map;

public final class CasesStandalone extends JavaPlugin implements Listener {

    private final TextComponent itemText = Component.text().content("Gamba Token").color(TextColor.color(255, 100, 0)).build();
    private final ItemStack tokenItem = ItemBuilder.of(Material.SUNFLOWER).fakeEnchant().name(itemText).build();

    @Getter
    private long startTime;

    public static final NamespacedKey KEY = NamespacedKey.fromString("folia-stuff");

    @Getter
    private static CasesStandalone instance;

    public ItemStack getNewGambaToken() {
        return tokenItem.clone();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        startTime = System.currentTimeMillis() / 1000L;

//        getCommand("wheel").setExecutor(new WheelCommand());
//        getCommand("wheeleffect").setExecutor(new WheelEffectCommand());
        getCommand("displayname").setExecutor(new DisplayNameCommand());
//        getCommand("gif").setExecutor(new DisplayImageCommand());
//        getCommand("test").setExecutor(new TestCommand());

        CaseCommand cmd = new CaseCommand();
        getCommand("case").setExecutor(cmd);
        getCommand("case").setTabCompleter(cmd);

        registerListeners();

//        WheelEffectManager.getInstance();

        getCommand("home").setExecutor(new HomeCommand());
        getCommand("sethome").setExecutor(new SetHomeCommand());
        getCommand("spawn").setExecutor(new SpawnCommand());
        getCommand("gamba").setExecutor(new GambaCommand());

        registerExpansion();

        SettingsManager.getInstance().setup();

        if (TaskBuilder.isFoliaSupported()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(this, task -> {
                initStorageStuff();
            }, 40);
        } else {
            Bukkit.getScheduler().runTaskLater(this, this::initStorageStuff, 40);
        }

        Sync.get().interval(20).run(this::giveTimePlayed);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
//        SettingsManager.getInstance().saveWheels();
//        SettingsManager.getInstance().saveMapRenders();
        SettingsManager.getInstance().saveCases();
    }

    private void initStorageStuff() {
        SettingsManager.getInstance().loadCases();
//        SettingsManager.getInstance().loadWheels();
        SettingsManager.getInstance().loadAllCaseStats();
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(this, this);
//        pm.registerEvents(new WheelListener(), this);
        pm.registerEvents(new CaseListener(), this);
//        pm.registerEvents(new TreeFellerListener(), this);
        pm.registerEvents(new GuiListener(), this);
    }

    private void giveTimePlayed() {
        for(Player player : Bukkit.getOnlinePlayers()) {
            CaseStats stats = CaseStats.get(player.getUniqueId());
            stats.setTimePlayedSeconds(stats.getTimePlayedSeconds() + 1);

            if(stats.getTimePlayedSeconds() % 3600 == 0) {
                ItemStack award = CasesStandalone.getInstance().getNewGambaToken();
                var drops = player.getInventory().addItem(award);

                for(Map.Entry<Integer, ItemStack> drop : drops.entrySet()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop.getValue());
                }
            }
        }
    }

    public void registerExpansion() {
        getLogger().info("Registering expansion");

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
            getLogger().info("Registering case placeholders");
            new CasePlaceholders().register();
        }

        if (Bukkit.getPluginManager().isPluginEnabled("MiniPlaceholders")) {
            final Expansion.Builder expansionBuilder = Expansion.builder("case")
                    .filter(Player.class)
                    .audiencePlaceholder("name", (audience, ctx, queue) -> {
                        final Player player = (Player) audience;
                        return Tag.selfClosingInserting(player.name());
                    })
                    .audiencePlaceholder("time_played", (audience, ctx, queue) -> {
                        final Player player = (Player) audience;
                        int timePlayedSeconds = CaseStats.get(player.getUniqueId()).getTimePlayedSeconds();
                        String formattedTime = Duration.seconds(timePlayedSeconds).formatText();
                        return Tag.selfClosingInserting(Component.text(formattedTime));
                    })
                    .audiencePlaceholder("player_total_opens", (audience, ctx, queue) -> {
                        final Player player = (Player) audience;
                        CaseStats stats = CaseStats.get(player.getUniqueId());
                        return Tag.selfClosingInserting(Component.text(Util.format(stats.totalOpens())));
                    })
                    .audiencePlaceholder("total_emeralds_spent", (audience, ctx, queue) -> {
                        final Player player = (Player) audience;
                        CaseStats stats = CaseStats.get(player.getUniqueId());
                        return Tag.selfClosingInserting(Component.text(Util.format(stats.totalOpens() * 3)));
                    })
                    .globalPlaceholder("total_opens", (ctx, queue) -> {
                        int total = 0;
                        for (CaseStats stats : CaseStats.getAll())
                            total += stats.totalOpens();

                        return Tag.selfClosingInserting(Component.text(Util.format(total)));
                    })
                    .globalPlaceholder("server_total_emeralds_spent", (ctx, queue) -> {
                        int total = 0;
                        for (CaseStats stats : CaseStats.getAll())
                            total += (stats.totalOpens() * 3);

                        return Tag.selfClosingInserting(Component.text(Util.format(total)));
                    });

            for (CaseItem.CaseRarity rarity : CaseItem.CaseRarity.values()) {
                expansionBuilder.audiencePlaceholder(rarity.name().toLowerCase() + "_total_opens", (audience, ctx, queue) -> {
                    final Player player = (Player) audience;
                    CaseStats stats = CaseStats.get(player.getUniqueId());
                    return Tag.selfClosingInserting(Component.text(Util.format(stats.getCaseOpens(rarity))));
                });
                expansionBuilder.globalPlaceholder("total_" + rarity.name().toLowerCase() + "_opens", (ctx, queue) -> {
                    int total = 0;
                    for (CaseStats stats : CaseStats.getAll())
                        total += stats.getCaseOpens(rarity);

                    return Tag.selfClosingInserting(Component.text(Util.format(total)));
                });
                expansionBuilder.audiencePlaceholder(rarity.name().toLowerCase() + "_total_percentage", (audience, ctx, queue) -> {
                    final Player player = (Player) audience;
                    CaseStats stats = CaseStats.get(player.getUniqueId());

                    DecimalFormat df = new DecimalFormat("##.##");
                    return Tag.selfClosingInserting(Component.text(df.format(stats.getChance(rarity) * 100)));
                });
                expansionBuilder.globalPlaceholder("total_" + rarity.name().toLowerCase() + "_percentage", (ctx, queue) -> {
                    double totalRarityOpens = 0;
                    double total = 0;
                    for (CaseStats caseStats : CaseStats.getAll()) {
                        total += caseStats.totalOpens();
                        totalRarityOpens += caseStats.getCaseOpens(rarity);
                    }
                    double chance = totalRarityOpens / total;

                    DecimalFormat df = new DecimalFormat("##.##");
                    return Tag.selfClosingInserting(Component.text(df.format(chance * 100)));
                });
            }

            Expansion expansion = expansionBuilder.build();
            expansion.register();
        }
    }
}
