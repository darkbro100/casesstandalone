package me.paul;

import it.unimi.dsi.fastutil.Pair;
import me.paul.util.Case;
import me.paul.util.CaseItem;
import me.paul.util.CaseStats;
import me.paul.util.SettingsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CaseListener implements Listener {

  @EventHandler
  public void onGambaTokenBlockInteract(PlayerInteractEvent event) {
    if(event.getItem() == null) return;
    if(!event.getItem().isSimilar(CasesStandalone.getInstance().getNewGambaToken())) return;

    if(event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
//    if(event instanceof PlayerInteractAtEntityEvent) return;

    event.setCancelled(true);
  }

  @EventHandler
  public void onAdvancement(PlayerAdvancementDoneEvent event) {
    String key = event.getAdvancement().key().namespace();
    String name = event.getAdvancement().key().value();

    if(!key.equalsIgnoreCase("minecraft")) {
    //  System.out.println("ignoring advancement: " + key);
      return;
    }

    if(name.toLowerCase().startsWith("recipes/")) {
    //  System.out.println("ignoring advancement: " + name);
      return;
    }

    System.out.println("advancement : " + key + ":" + name);
//    System.out.println("requirements : " + event.getAdvancement().getRequirements().getRequirements().size());

    ItemStack award = CasesStandalone.getInstance().getNewGambaToken();
    var drops = event.getPlayer().getInventory().addItem(award);

    for(Map.Entry<Integer, ItemStack> drop : drops.entrySet()) {
      event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), drop.getValue());
    }
  }

  @EventHandler
  public void onDie(PlayerDeathEvent event) {
    Player player = event.getPlayer();
    if (player.getKiller() != null) {
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      meta.setPlayerProfile(player.getPlayerProfile());
      head.setItemMeta(meta);

      event.getDrops().add(head);
    }
  }

  @EventHandler
  public void onSpawnerPlace(BlockPlaceEvent event) {
    if (event.getBlockPlaced().getType() == Material.SPAWNER && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.SPAWNER) {
      BlockStateMeta bsm = (BlockStateMeta) event.getPlayer().getInventory().getItemInMainHand().getItemMeta();
      CreatureSpawner itemSpawner = (CreatureSpawner) bsm.getBlockState();

      CreatureSpawner blockSpawner = (CreatureSpawner) event.getBlockPlaced().getState();
      blockSpawner.setSpawnedType(itemSpawner.getSpawnedType());
      blockSpawner.update();
    }
  }


  //TODO: undo this if we ever move to some type of DB solution (prolly sqlite)
  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    CaseStats stats = CaseStats.get(event.getPlayer().getUniqueId());
    if (stats == null) {
      stats = SettingsManager.getInstance().load(event.getPlayer().getUniqueId());
      CaseStats.store(event.getPlayer().getUniqueId(), stats);
      CasesStandalone.getInstance().getLogger().info("Loading case stats for: " + event.getPlayer().getName());
    }
  }

//  @EventHandler
//  public void onQuit(PlayerQuitEvent event) {
//    FoliaStuff.getInstance().getLogger().info("Saving case stats for: " + event.getPlayer().getName());
//    CaseStats stats = CaseStats.get(event.getPlayer().getUniqueId());
//    SettingsManager.getInstance().save(stats);
////    CaseStats.clear(event.getPlayer().getUniqueId());
//  }

//  @EventHandler
//  public void onItemGiveJoin(PlayerJoinEvent event) {
//    Sync.get(event.getPlayer()).delay(20).run(() -> {
//      for (CaseItem caseItem : Case.getCases()[0].getItems()) {
//        if (caseItem.getRarity() == CaseItem.CaseRarity.BLUE)
//          continue;
//
//        if (caseItem.getRarity() == CaseItem.CaseRarity.PURPLE)
//          continue;
//
//        if (caseItem.getRarity() == CaseItem.CaseRarity.ANCIENT)
//          continue;
//
//        if (caseItem.getRarity() == CaseItem.CaseRarity.RED)
//          continue;
//
//        if (caseItem.getRarity() == CaseItem.CaseRarity.PINK)
//          continue;
//
//        for (ItemStack drop : caseItem.drops())
//          event.getPlayer().getInventory().addItem(drop);
//      }
//    });
//  }

//  @EventHandler
//  public void onInteract(PlayerInteractEvent event) {
//    if (event.getClickedBlock() == null)
//      return;
//    if (event.getClickedBlock().isEmpty())
//      return;
//    if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
//      return;
//    if (event.getItem() == null)
//      return;
//    if (event.getItem().getType() != Material.PANDA_SPAWN_EGG)
//      return;
//    if (!ItemUtil.getName(event.getItem()).contains("THE GOAT"))
//      return;
//
//    event.setCancelled(true);
//    event.getItem().subtract();
//
//    Player player = event.getPlayer();
//
//    try {
//      CustomPanda cp = (CustomPanda) NMS.createEntity(CustomPanda.class, player.getLocation());
//      cp.setOwner(player.getUniqueId());
//
//      // wanna make the text gold/grayish color
//      Component customName = Component.text("THE GOAT PANDA").color(TextColor.color(0xFFD700));
//      cp.setCustomName(io.papermc.paper.adventure.PaperAdventure.asVanilla(customName));
//    } catch (NoSuchMethodException e) {
//      throw new RuntimeException(e);
//    } catch (InvocationTargetException e) {
//      throw new RuntimeException(e);
//    } catch (InstantiationException e) {
//      throw new RuntimeException(e);
//    } catch (IllegalAccessException e) {
//      throw new RuntimeException(e);
//    }
//  }

  @EventHandler
  public void onInteract(PlayerInteractAtEntityEvent event) {
    if (event.getRightClicked() instanceof ArmorStand stand && event.getRightClicked().hasMetadata("caseId")) {
      event.setCancelled(true);

      Player player = event.getPlayer();
      int id = stand.getMetadata("caseId").get(0).asInt();
      Case caseIns = Case.get(id);

      // can the player afford a spin?
      ItemStack hand = player.getInventory().getItemInMainHand();
      if (!hand.isSimilar(CasesStandalone.getInstance().getNewGambaToken())) {
        player.sendMessage(Component.text("You need a gamba token to spin a case!", NamedTextColor.RED));
        return;
      }

      // don't let the case double spin
      if (caseIns.isRunning()) {
        if (caseIns.spinner().equals(player.getUniqueId())) {
          caseIns.quickOpen();
        } else {
          player.sendMessage(Component.text("Case is already spinning!", NamedTextColor.RED));
        }
        return;
      }

      // remove 1 from their hand
      hand.subtract(1);

      CompletableFuture<Pair<CaseItem, ItemStack>> future = new CompletableFuture<>();
      caseIns.spin(player, future);
      future.whenComplete((pair, ex) -> {
        if (ex != null) {
          ex.printStackTrace();
          return;
        }

        ItemStack it = pair.right();
        CaseItem caseItem = pair.left();

        @NotNull HashMap<Integer, ItemStack> map = player.getInventory().addItem(it);
        if (!map.isEmpty())
          map.values().forEach(it2 -> player.getWorld().dropItemNaturally(player.getLocation(), it2));


        Component msg = player.displayName()
          .append(Component.text(" got a ")
            .color(TextColor.color(255, 255, 255)))
          .append(Component.text(caseItem.getRarity().name())
            .color(caseItem.getRarity().getColor()))
          .append(Component.text(" item!")
            .color(TextColor.color(255, 255, 255)));

        if (caseItem.getRarity().isSuperRare()) {
          Bukkit.broadcast(msg);
        } else {
          player.getWorld().getNearbyPlayers(player.getLocation(), RADIUS).forEach(p -> p.sendMessage(msg));
        }

        CaseStats stats = CaseStats.get(player.getUniqueId());
        stats.addCaseOpen(caseItem.getRarity());
        SettingsManager.getInstance().save(stats);
      });

      // send msg nearby
      player.getWorld().getNearbyPlayers(player.getLocation(), RADIUS).forEach(p -> p.sendMessage(player.displayName()
        .append(Component.text(" is going for the gold!"))
        .append(Component.text(" GOLD GOLD GOLD!", NamedTextColor.GOLD))));
    }
  }

  private static final double RADIUS = 50.0D;
}
