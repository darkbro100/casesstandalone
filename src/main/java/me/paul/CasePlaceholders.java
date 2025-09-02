package me.paul;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.paul.util.CaseItem;
import me.paul.util.CaseStats;
import me.paul.util.Duration;
import me.paul.util.Util;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;

public class CasePlaceholders extends PlaceholderExpansion {

  @Override
  public boolean canRegister() {
    return true;
  }

  @Override
  public @NotNull String getIdentifier() {
    // use "case" so placeholders are %case_*%
    return "case";
  }

  @Override
  public @NotNull String getAuthor() {
    return "paulyg";
  }

  @Override
  public @NotNull String getVersion() {
    return "1.0";
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public @Nullable String onRequest(final OfflinePlayer player, @NotNull String params) {
    if (player == null) return "";

    // Player-specific placeholders
    if (params.equalsIgnoreCase("name")) {
      return player.getName();
    } else if (params.equalsIgnoreCase("time_played")) {
      int timePlayedSeconds = CaseStats.get(player.getUniqueId()).getTimePlayedSeconds();
      return Duration.seconds(timePlayedSeconds).formatText();
    } else if (params.equalsIgnoreCase("player_total_opens")) {
      CaseStats stats = CaseStats.get(player.getUniqueId());
      return Util.format(stats.totalOpens());
    } else if (params.equalsIgnoreCase("total_emeralds_spent")) {
      CaseStats stats = CaseStats.get(player.getUniqueId());
      return Util.format(stats.totalOpens() * 3); // 3 emeralds per open
    }

    // Global placeholders
    if (params.equalsIgnoreCase("total_opens")) {
      int total = 0;
      for (CaseStats stats : CaseStats.getAll()) total += stats.totalOpens();
      return Util.format(total);
    } else if (params.equalsIgnoreCase("server_total_emeralds_spent")) {
      int total = 0;
      for (CaseStats stats : CaseStats.getAll()) total += stats.totalOpens() * 3;
      return Util.format(total);
    }

    // Rarity placeholders
    for (CaseItem.CaseRarity rarity : CaseItem.CaseRarity.values()) {
      String key = rarity.name().toLowerCase();

      String totalOpens = "total_" + key + "_opens";
      String playerOpens = key + "_total_opens";
      String totalPercentage = "total_" + key + "_percentage";
      String playerPercentage = key + "_total_percentage";

      if (params.equalsIgnoreCase(totalOpens)) {
        int total = 0;
        for (CaseStats stats : CaseStats.getAll()) total += stats.getCaseOpens(rarity);
        return Util.format(total);
      } else if (params.equalsIgnoreCase(playerOpens)) {
        CaseStats stats = CaseStats.get(player.getUniqueId());
        return Util.format(stats.getCaseOpens(rarity));
      } else if (params.equalsIgnoreCase(totalPercentage)) {
        double totalRarityOpens = 0;
        double total = 0;
        for (CaseStats s : CaseStats.getAll()) {
          total += s.totalOpens();
          totalRarityOpens += s.getCaseOpens(rarity);
        }
        double chance = (total == 0 ? 0 : (totalRarityOpens / total));
        DecimalFormat df = new DecimalFormat("##.##");
        return df.format(chance * 100);
      } else if (params.equalsIgnoreCase(playerPercentage)) {
        CaseStats stats = CaseStats.get(player.getUniqueId());
        DecimalFormat df = new DecimalFormat("##.##");
        return df.format(stats.getChance(rarity) * 100);
      }
    }

    return null;
  }
}
