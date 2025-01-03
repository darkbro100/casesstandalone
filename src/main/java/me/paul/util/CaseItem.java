package me.paul.util;

import com.google.common.collect.Lists;
import lombok.Getter;
import me.paul.CasesStandalone;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class CaseItem {

  @Getter
  private final CaseRarity rarity;
  private final List<ItemStack> drops = Lists.newArrayList();

  public CaseItem(CaseRarity rarity) {
    this.rarity = rarity;
  }

  public CaseItem add(ItemStack item) {
    drops.add(item);
    return this;
  }

  public CaseItem add(ItemStack... items) {
    Collections.addAll(drops, items);
    return this;
  }

  public CaseItem add(List<ItemStack> items) {
    drops.addAll(items);
    return this;
  }

  public ItemStack[] drops() {
    return drops.toArray(new ItemStack[0]);
  }

  public void removeDrop(ItemStack item) {
    CasesStandalone.getInstance().getLogger().info("Attempted to remove item from CaseItem pool: " + drops.remove(item));
  }

  public ItemStack generateItem() {
    List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
    Player player = Util.getRandomEntry(players);

    ItemStack it = Util.getRandomEntry(drops).clone();
    if (it.getType() == Material.ENCHANTED_BOOK) {
      ItemStack ret = new ItemStack(Material.ENCHANTED_BOOK);
      Random rnd = new Random();
      Iterator<Enchantment> itr = Registry.ENCHANTMENT.iterator();
      Enchantment res = null;
      float highest = 0;

      while(itr.hasNext()) {
        Enchantment ench = itr.next();
        if (rnd.nextFloat() > highest) {
          highest = rnd.nextFloat();
          res = ench;
        }
      }

      if(res != null) {
        ret.addUnsafeEnchantment(res, Util.random(1, res.getMaxLevel()));
        return ret;
      }

    } else if (it.getType() == Material.PLAYER_HEAD) {
      SkullMeta meta = (SkullMeta) it.getItemMeta();
      meta.setPlayerProfile(player.getPlayerProfile());
      it.setItemMeta(meta);
    }

    return it;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CaseItem caseItem = (CaseItem) o;
    return rarity == caseItem.rarity && Objects.equals(drops, caseItem.drops);
  }

  @Override
  public int hashCode() {
    return Objects.hash(rarity, drops);
  }

    @Getter
  public enum CaseRarity {
    BLUE(79_894, TextColor.color(0, 0, 125), Material.BLUE_WOOL),
    PURPLE(15_980, TextColor.color(125, 0, 125), Material.PURPLE_WOOL),
    PINK(3_200, TextColor.color(255, 85, 255), Material.PINK_WOOL),
    RED(640, TextColor.color(125, 0, 0), Material.RED_WOOL),
    GOLD(260, TextColor.color(255, 170, 0), Material.YELLOW_WOOL),
    ANCIENT(26, TextColor.color(255, 85, 0), Material.ORANGE_WOOL);

    private static final CaseRarity[] VALUES = values();

    final int weight;
    final TextColor color;
    final Material blockType;

    CaseRarity(int w, TextColor c, Material b) {
      weight = w;
      color = c;
      blockType = b;
    }

    public static CaseRarity of(String key) {
      for (CaseRarity rarity : VALUES) {
        if (rarity.name().equalsIgnoreCase(key)) {
          return rarity;
        }
      }

      return null;
    }

    public boolean isSuperRare() {
      return this == RED || this == GOLD || this == ANCIENT;
    }
  }

}
