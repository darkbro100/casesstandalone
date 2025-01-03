package me.paul.util;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.Pair;
import me.paul.CasesStandalone;
import me.paul.util.scheduler.Sync;
import me.paul.util.scheduler.TaskHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CaseRunnable implements Runnable {

  private static final Duration LIMIT = Duration.seconds(5.6);
  protected static final int MAX_ITEMS = 5;

  private final TaskHolder holder;
  private final Case caseInst;

  private int ticks = 0;
  private int endTicks = 0;
  private int endDelay = 0;

  protected final List<Item> itemCycle = Lists.newArrayListWithCapacity(MAX_ITEMS);

  private final Location maxLeft;
  private boolean shouldAdd = false;

  private Vector speed;

  protected CaseItem winningItem;
  protected final ItemStack winningItemStack;
  private static final int WINNING_TICKET = 106; // 102 = folia, 100 = paper?
  private Item winningItemInstance;
  protected CompletableFuture<Pair<CaseItem, ItemStack>> future;

  private Vector direction;

  public CaseRunnable(TaskHolder holder, Case caseInst, CompletableFuture<Pair<CaseItem, ItemStack>> future) {
    this.future = future;
    this.holder = holder;
    this.caseInst = caseInst;

    // generate what the player wins
    this.winningItem = caseInst.generateItem();
    this.winningItemStack = winningItem.generateItem().clone();

    Util.Direction dir = Util.Direction.get(caseInst.getDirection().getYaw() + 90);
    this.direction = dir.getVector().multiply(-1);
    this.speed = new Vector(direction.getX() * 0.5, direction.getY() * 0.5, direction.getZ() * 0.5);

    double maxOffset = ((double) MAX_ITEMS / 2);
    this.maxLeft = caseInst.location().clone().add(direction.clone().multiply(maxOffset)).subtract(direction.clone().multiply(0.25));

    for (int i = -(MAX_ITEMS / 2); i <= ((MAX_ITEMS)) / 2; i++)
      this.itemCycle.add(drop(caseInst.generateItem(true), i, false));
  }

  private static final double END_DEC = -0.01;

  @Override
  public void run() {
    //if time is up stop
    if (ticks >= LIMIT.ticks()) {

      // randomly make the ending wait 2-3 seconds
      if (endTicks == 0)
        endDelay = Util.random(40, 60);

      if (speed.length() > Math.abs(END_DEC) && endTicks != 0 && endTicks % 2 == 0) {
        speed.add(direction.clone().multiply(END_DEC));
      }

      // once the end state has ended
      if (endTicks >= endDelay) {
        holder.cancel();
        resetSpeed();

        // clear other items
        for (Item it : itemCycle) {
          if (it.equals(winningItemInstance))
            continue;

          it.remove();
        }
        itemCycle.clear();

        // exec cosmetic runnable
        CaseItem.CaseRarity rarity = winningItem.getRarity();
        if (rarity == CaseItem.CaseRarity.ANCIENT || rarity == CaseItem.CaseRarity.GOLD || rarity == CaseItem.CaseRarity.RED) {
          TaskHolder endHolder = new TaskHolder();
          Sync.get(winningItemInstance).interval(1).holder(endHolder).run(new CaseReceiveItemRunnable(caseInst, winningItemInstance, winningItem, winningItemStack, endHolder, future));
        } else {
          Vector offset = new Vector(Util.random(), Util.random(), Util.random());

          if (rarity == CaseItem.CaseRarity.PURPLE || rarity == CaseItem.CaseRarity.BLUE) {
            winningItemInstance.getWorld().playSound(winningItemInstance.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            new ParticleBuilder(Particle.ANGRY_VILLAGER).location(winningItemInstance.getLocation()).offset(offset.getX(), offset.getY(), offset.getZ()).count(25).spawn();
          } else {
            winningItemInstance.getWorld().playSound(winningItemInstance.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
            new ParticleBuilder(Particle.HAPPY_VILLAGER).location(winningItemInstance.getLocation()).offset(offset.getX(), offset.getY(), offset.getZ()).count(25).spawn();
          }

          winningItemInstance.remove();
          winningItemInstance = null;
          caseInst.spinner = null;

          caseInst.displayEntity().text(Component.text("Right Click to Spin!").color(TextColor.color(0x965613)));
          future.complete(Pair.of(winningItem, winningItemStack));

          // update floor
          caseInst.resetFloor(direction);
        }

        caseInst.running.set(false);
        return;
      }

      // keep shifting items over (slowly)
      for (int i = itemCycle.size() - 1; i >= 0; i--) {
        Item item = itemCycle.get(i);
        item.setVelocity(speed);
        if (!checkItem(item))
          updateBlock(item, true);
      }

      // set speed to 0 if the winning item is gonna leave
      if (checkIsWithinCenter(winningItemInstance)) {
        speed.multiply(0);
      }

      // add new item in case
      addNewItem();

      endTicks++;
      ticks++;
      return;
    }

    if (ticks != 0 && ticks % 40 == 0)
      speed.multiply(0.5);

    // Shift every item over to the left
    for (int i = itemCycle.size() - 1; i >= 0; i--) {
      Item item = itemCycle.get(i);
      item.setVelocity(speed);
      // mark for removal
      if (!checkItem(item))
        updateBlock(item, false);
    }

    // add new item to the end of the cycle
    addNewItem();

    ticks++;
  }

  private void resetSpeed() {
    this.speed = new Vector(direction.getX() * 0.5, direction.getY() * 0.5, direction.getZ() * 0.5);
  }

  private void updateBlock(Item item, boolean particle) {
    CaseItem.CaseRarity rarity = CaseItem.CaseRarity.of(item.getMetadata("color").get(0).asString());
    Location behindBlock = item.getLocation().clone().add(0, -1, 0).getBlock().getLocation();
    Location maxLeftBlock = maxLeft.getBlock().getLocation().clone().add(0, -1, 0).getBlock().getLocation();

    Location len = behindBlock.clone().subtract(maxLeftBlock.clone());
    len.setY(0);

    if (len.length() >= 0) {
      behindBlock.getBlock().setType(rarity.blockType);
    }

    if (particle && ticks % 2 == 0) {
      Location particleLoc = item.getLocation().clone().add(0, 0.25, 0.3);
      new ParticleBuilder(Particle.DUST).color(rarity.getColor().red(), rarity.getColor().green(), rarity.getColor().blue()).location(particleLoc).extra(0).count(1).spawn();
    }
  }

  private void addNewItem() {
    if (shouldAdd) {
      // need to arbitarily insert the pre-chosen item into the list
      // so it appears in the middle
      // FUCK YOU STOP SPAMMING MY CONSOLE
//      System.out.println("spawning @ " + ticks + " winner: " + WINNING_TICKET);

      boolean winner = ticks == WINNING_TICKET;
      Item it = drop(winner ? winningItem : caseInst.generateItem(true), -((MAX_ITEMS / 2)), winner);
      if (winner)
        winningItemInstance = it;

      itemCycle.add(it);

      caseInst.location().getWorld().playSound(caseInst.location(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);

      shouldAdd = false;
    }
  }

  private boolean checkIsWithinCenter(Item item) {
    if (item == null) {
      return false;
    }

    Location itemLoc = item.getLocation();
    Location center = caseInst.location();

    if (direction.getX() < 0 && itemLoc.getX() <= center.getX() - 0.3) {
      return true;
    } else if (direction.getX() > 0 && itemLoc.getX() >= center.getX() + 0.3) {
      return true;
    } else if (direction.getZ() < 0 && itemLoc.getZ() <= center.getZ() - 0.3) {
      return true;
    } else return direction.getZ() > 0 && itemLoc.getZ() >= center.getZ() + 0.3;
  }

  private boolean checkItem(Item item) {
    boolean shouldRemove = false;
    // mark for removal
    if (direction.getX() < 0 && item.getLocation().getX() <= maxLeft.getX()) {
      shouldRemove = true;
    } else if (direction.getX() > 0 && item.getLocation().getX() >= maxLeft.getX()) {
      shouldRemove = true;
    } else if (direction.getZ() < 0 && item.getLocation().getZ() <= maxLeft.getZ()) {
      shouldRemove = true;
    } else if (direction.getZ() > 0 && item.getLocation().getZ() >= maxLeft.getZ()) {
      shouldRemove = true;
    }

    if (shouldRemove) {
      item.remove();
      itemCycle.remove(item);
      shouldAdd = true;
    }

    return shouldRemove;
  }

  private Item drop(CaseItem item, double offset, boolean winner) {
    ItemStack it = winner ? winningItemStack.clone() : item.generateItem().clone();
    it.setAmount(1);

    ItemMeta meta = it.getItemMeta();
    meta.displayName(Component.text(UUID.randomUUID().toString()));
    it.setItemMeta(meta);

    Location spawnLoc = caseInst.location().add(direction.clone().multiply(offset == 2 ? 1.9 : offset)).subtract(direction.clone().multiply(0.4));

    return caseInst.location().getWorld().dropItem(spawnLoc, it, d -> {
      d.setVelocity(speed);
      d.setGravity(false);
      d.setCanPlayerPickup(false);
      d.setCanMobPickup(false);
      d.setMetadata("color", new FixedMetadataValue(CasesStandalone.getInstance(), item.getRarity().name()));
      d.setCustomNameVisible(false);
    });
  }
}
