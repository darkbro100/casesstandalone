package me.paul.util;

import lombok.Getter;
import me.paul.CasesStandalone;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class SettingsManager {

  @Getter
  private static final SettingsManager instance = new SettingsManager();

  private SettingsManager() {
  }

  private File wheelFile, dataFile;
  private YamlConfiguration wheelConfig, dataConfig;

  public void setup() {
    if (!CasesStandalone.getInstance().getDataFolder().exists())
      CasesStandalone.getInstance().getDataFolder().mkdir();

    wheelFile = new File(CasesStandalone.getInstance().getDataFolder(), "wheel.yml");
    if (!wheelFile.exists()) {
      try {
        wheelFile.createNewFile();
      } catch (IOException e) {
        System.err.println("Could not create wheel.yml!");
        e.printStackTrace();
      }
    }

    wheelConfig = YamlConfiguration.loadConfiguration(wheelFile);

    dataFile = new File(CasesStandalone.getInstance().getDataFolder(), "data.yml");
    if (!dataFile.exists()) {
      try {
        dataFile.createNewFile();
      } catch (IOException e) {
        System.err.println("Could not create data.yml!");
        e.printStackTrace();
      }
    }

    dataConfig = YamlConfiguration.loadConfiguration(dataFile);
  }

  public void loadAllCaseStats() {
    for (String key : dataConfig.getKeys(false)) {
      UUID uuid = UUID.fromString(key);
      CaseStats stats = load(uuid);
      CaseStats.store(uuid, stats);
    }
  }

  public CaseStats load(UUID player) {
    if (!dataConfig.contains(player.toString()))
      return new CaseStats(player);

    CaseStats stats = new CaseStats(player);

    String val = dataConfig.getString(player + ".lastWheelSpin");
    int timeVal = dataConfig.getInt(player + ".timePlayedSeconds");

    if(val != null) {
      Instant instant = Instant.parse(val);
      Timestamp parsed = Timestamp.from(instant);
      stats.setLastWheelSpin(parsed);
    }

    stats.setTimePlayedSeconds(timeVal);

    for(CaseItem.CaseRarity rarity : CaseItem.CaseRarity.values())
      stats.setCaseOpens(rarity, dataConfig.getInt(player + "." + rarity.name().toLowerCase()));

    return stats;
  }

  public void save(CaseStats stats) {
    dataConfig.set(stats.getUuid() + ".lastWheelSpin", stats.getLastWheelSpin().toInstant().toString());
    dataConfig.set(stats.getUuid() + ".timePlayedSeconds", stats.getTimePlayedSeconds());

    for(CaseItem.CaseRarity rarity : CaseItem.CaseRarity.values())
      dataConfig.set(stats.getUuid() + "." + rarity.name().toLowerCase(), stats.getCaseOpens(rarity));

    try {
      dataConfig.save(dataFile);
    } catch (IOException e) {
      System.err.println("Could not save data.yml!");
      e.printStackTrace();
    }
  }

  public YamlConfiguration getWheelConfig() {
    return wheelConfig;
  }

  /**
   * private Location center; private Map<Vector, Material> cachedBlocks; private
   * int radius; private int pieces; private Material[] parts; private int
   * offsetInc; private int updateFrequency; private int lastAngle = 0;
   */
  public void loadCases() {
    File dir = new File(CasesStandalone.getInstance().getDataFolder(), "cases");
    if (!dir.exists()) {
      dir.mkdir();
      return;
    }

    for (File f : dir.listFiles()) {
      if (!f.getName().endsWith(".yml"))
        continue;

      YamlConfiguration config = YamlConfiguration.loadConfiguration(f);

      Location center = LocUtil.locFromString(config.getString("location"));
      Util.Direction direction = Util.Direction.valueOf(config.getString("direction"));
      Case caseInstance = new Case();

      for (String key : config.getConfigurationSection("items").getKeys(false)) {
        String path = "items." + key;
        CaseItem.CaseRarity rarity = CaseItem.CaseRarity.of(key);
        CaseItem caseItem = new CaseItem(rarity);

        for (String itemKey : config.getConfigurationSection(path + ".drops").getKeys(false)) {
          ItemStack stack = config.getItemStack(path + ".drops." + itemKey);
//          CasesStandalone.getInstance().getLogger().info("Loaded " + stack + " for rarity " + rarity.name());
          caseItem.add(stack);
        }

        caseInstance.add(caseItem);
//        CasesStandalone.getInstance().getLogger().info("Loaded caseitem " + rarity.name());
      }

      caseInstance.position(center, direction);
      CasesStandalone.getInstance().getLogger().info("Loaded case " + caseInstance.getId() + " @ " + center + " with " + caseInstance.getItems().length + " items");
    }
  }

  public void saveCases() {
    File dir = new File(CasesStandalone.getInstance().getDataFolder(), "cases");
    if (!dir.exists()) {
      dir.mkdir();
      return;
    }

    for(File f : dir.listFiles())
      f.delete();

    for (Case c : Case.getCases()) {
      File f = new File(dir, c.getId() + ".yml");
      YamlConfiguration config = YamlConfiguration.loadConfiguration(f);

      config.set("location", LocUtil.locToString(c.location()));
      config.set("direction", c.getDirection().name());

      for (CaseItem item : c.getItems()) {
        String path = "items." + item.getRarity().name();
        ItemStack[] items = item.drops();

        for (int i = 0; i < items.length; i++) {
          config.set(path + ".drops." + i, items[i]);
        }
      }

      try {
        config.save(f);
      } catch (IOException e) {
        e.printStackTrace();
      }

//      // attempt to delete previous ents
//      if(c.interactEntity() != null)
//        c.interactEntity().remove();
//      if(c.displayEntity() != null)
//        c.displayEntity().remove();
    }
  }

}
