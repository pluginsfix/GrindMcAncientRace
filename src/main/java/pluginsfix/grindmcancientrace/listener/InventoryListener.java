package pluginsfix.grindmcancientrace.listener;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pluginsfix.grindmcancientrace.config.PluginConfig;
import pluginsfix.grindmcancientrace.domain.ActiveEffect;
import pluginsfix.grindmcancientrace.domain.PriceRequirement;
import pluginsfix.grindmcancientrace.domain.TradeReward;
import pluginsfix.grindmcancientrace.domain.VillagerTrade;
import pluginsfix.grindmcancientrace.gui.AncientRaceGuiHolder;
import pluginsfix.grindmcancientrace.hook.PlayerPointsHook;
import pluginsfix.grindmcancientrace.hook.VaultEconomyHook;
import pluginsfix.grindmcancientrace.storage.EffectRepository;
import pluginsfix.grindmcancientrace.storage.TaskProgressRepository;
import pluginsfix.grindmcancientrace.text.Messages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryListener implements Listener {
    private final Plugin plugin;
    private final PluginConfig config;
    private final Messages messages;
    private final VaultEconomyHook vaultHook;
    private final PlayerPointsHook pointsHook;
    private final EffectRepository effectRepository;
    private final TaskProgressRepository taskRepository;
    private final Map<UUID, Long> clickDebounce;

    public InventoryListener(
            Plugin plugin,
            PluginConfig config,
            Messages messages,
            VaultEconomyHook vaultHook,
            PlayerPointsHook pointsHook,
            EffectRepository effectRepository,
            TaskProgressRepository taskRepository
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.vaultHook = vaultHook;
        this.pointsHook = pointsHook;
        this.effectRepository = effectRepository;
        this.taskRepository = taskRepository;
        this.clickDebounce = new ConcurrentHashMap<>();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AncientRaceGuiHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof AncientRaceGuiHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != holder) {
            return;
        }

        int slot = event.getSlot();
        List<Integer> tradeSlots = config.tradeSlots();
        int tradeIndex = tradeSlots.indexOf(slot);
        if (tradeIndex < 0 || tradeIndex >= holder.trades().size()) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastClick = clickDebounce.get(player.getUniqueId());
        if (lastClick != null && (now - lastClick) < 350L) {
            return;
        }
        clickDebounce.put(player.getUniqueId(), now);

        VillagerTrade trade = holder.trades().get(tradeIndex);
        processTrade(player, trade);
    }

    private void processTrade(Player player, VillagerTrade trade) {
        PriceRequirement price = trade.price();
        TradeReward reward = trade.reward();

        if (reward instanceof TradeReward.EffectReward effectReward) {
            Optional<ActiveEffect> existing = effectRepository.getEffect(player.getUniqueId(), effectReward.effectType());
            if (existing.isPresent() && !existing.get().isExpired(System.currentTimeMillis())) {
                String remaining = existing.get().formatRemainingTime(System.currentTimeMillis());
                messages.send(player, "trade.already-has-effect", Placeholder.parsed("time", remaining));
                return;
            }
        }

        if (!hasPrice(player, price)) {
            sendPriceFailMessage(player, price);
            return;
        }

        if (reward instanceof TradeReward.ItemReward && isInventoryFull(player)) {
            messages.send(player, "trade.inventory-full");
            return;
        }

        if (!deductPrice(player, price)) {
            sendPriceFailMessage(player, price);
            return;
        }

        grantReward(player, reward);
        messages.send(player, "trade.success");
    }

    private boolean hasPrice(Player player, PriceRequirement price) {
        return switch (price) {
            case PriceRequirement.ItemPrice itemPrice -> {
                Material mat = Material.matchMaterial(itemPrice.material());
                if (mat == null) yield false;
                yield countItems(player, mat, itemPrice.customModelData()) >= itemPrice.amount();
            }
            case PriceRequirement.MoneyPrice moneyPrice -> {
                Optional<Economy> ecoOpt = vaultHook.getEconomy();
                yield ecoOpt.map(economy -> economy.has(player, moneyPrice.amount())).orElse(false);
            }
            case PriceRequirement.DonatePointsPrice pointsPrice -> pointsHook.hasPoints(player.getUniqueId(), pointsPrice.amount());
            case PriceRequirement.TaskPrice taskPrice -> {
                String taskKey = taskPrice.taskType() + ":" + taskPrice.target();
                yield taskRepository.getProgress(player.getUniqueId(), taskKey) >= taskPrice.requiredCount();
            }
        };
    }

    private boolean deductPrice(Player player, PriceRequirement price) {
        return switch (price) {
            case PriceRequirement.ItemPrice itemPrice -> {
                Material mat = Material.matchMaterial(itemPrice.material());
                if (mat == null) yield false;
                yield removeItems(player, mat, itemPrice.amount(), itemPrice.customModelData());
            }
            case PriceRequirement.MoneyPrice moneyPrice -> {
                Optional<Economy> ecoOpt = vaultHook.getEconomy();
                if (ecoOpt.isEmpty()) yield false;
                yield ecoOpt.get().withdrawPlayer(player, moneyPrice.amount()).transactionSuccess();
            }
            case PriceRequirement.DonatePointsPrice pointsPrice -> pointsHook.takePoints(player.getUniqueId(), pointsPrice.amount());
            case PriceRequirement.TaskPrice taskPrice -> {
                String taskKey = taskPrice.taskType() + ":" + taskPrice.target();
                taskRepository.decrementProgress(player.getUniqueId(), taskKey, taskPrice.requiredCount());
                yield true;
            }
        };
    }

    private void grantReward(Player player, TradeReward reward) {
        switch (reward) {
            case TradeReward.ItemReward itemReward -> {
                ItemStack item = buildRewardItemStack(itemReward);
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    leftover.values().forEach(drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
                }
            }
            case TradeReward.EffectReward effectReward -> {
                long durationMillis = effectReward.durationHours() * 3600_000L;
                long expiresAt = System.currentTimeMillis() + durationMillis;
                ActiveEffect activeEffect = new ActiveEffect(
                        player.getUniqueId(),
                        effectReward.effectType().toUpperCase(),
                        effectReward.amplifier(),
                        expiresAt
                );
                effectRepository.saveEffect(activeEffect);
                applyPotionEffect(player, activeEffect);
                messages.send(player, "trade.effect-applied", Placeholder.parsed("effect", effectReward.customName() != null ? effectReward.customName() : effectReward.effectType()));
            }
            case TradeReward.MoneyReward moneyReward -> {
                vaultHook.getEconomy().ifPresent(eco -> eco.depositPlayer(player, moneyReward.amount()));
            }
            case TradeReward.DonatePointsReward pointsReward -> {
                pointsHook.givePoints(player.getUniqueId(), pointsReward.amount());
            }
            case TradeReward.CommandReward cmdReward -> {
                for (String cmd : cmdReward.commands()) {
                    String processed = cmd.replace("<player>", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
                }
            }
        }
    }

    private ItemStack buildRewardItemStack(TradeReward.ItemReward reward) {
        Material mat = Material.matchMaterial(reward.material());
        if (mat == null) mat = Material.STONE;
        ItemStack item = new ItemStack(mat, Math.max(1, reward.amount()));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (reward.customName() != null && !reward.customName().isEmpty()) {
            meta.displayName(messages.miniMessage().deserialize(reward.customName()));
        }
        if (reward.lore() != null && !reward.lore().isEmpty()) {
            meta.lore(reward.lore().stream().map(l -> messages.miniMessage().deserialize(l)).toList());
        }
        if (reward.customModelData() != null) {
            meta.setCustomModelData(reward.customModelData());
        }
        if (reward.enchants() != null) {
            for (var entry : reward.enchants().entrySet()) {
                Enchantment ench = Enchantment.getByName(entry.getKey().toUpperCase());
                if (ench != null) {
                    meta.addEnchant(ench, entry.getValue(), true);
                }
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private void applyPotionEffect(Player player, ActiveEffect effect) {
        PotionEffectType type = PotionEffectType.getByName(effect.effectType());
        if (type == null) return;
        int durationTicks = (int) Math.min(Integer.MAX_VALUE - 1, effect.remainingSeconds(System.currentTimeMillis()) * 20L);
        if (durationTicks > 0) {
            player.addPotionEffect(new PotionEffect(type, durationTicks, effect.amplifier(), false, false, true));
        }
    }

    private int countItems(Player player, Material material, Integer customModelData) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;
            if (customModelData != null) {
                if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) continue;
                if (item.getItemMeta().getCustomModelData() != customModelData) continue;
            }
            count += item.getAmount();
        }
        return count;
    }

    private boolean removeItems(Player player, Material material, int amount, Integer customModelData) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            if (customModelData != null) {
                if (!item.hasItemMeta() || !item.getItemMeta().hasCustomModelData()) continue;
                if (item.getItemMeta().getCustomModelData() != customModelData) continue;
            }

            int itemAmount = item.getAmount();
            if (itemAmount <= remaining) {
                remaining -= itemAmount;
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(itemAmount - remaining);
                remaining = 0;
                break;
            }

            if (remaining <= 0) break;
        }
        return remaining <= 0;
    }

    private boolean isInventoryFull(Player player) {
        return player.getInventory().firstEmpty() == -1;
    }

    private void sendPriceFailMessage(Player player, PriceRequirement price) {
        switch (price) {
            case PriceRequirement.ItemPrice itemPrice -> {
                messages.send(player, "trade.not-enough-items",
                        Placeholder.parsed("item", itemPrice.material()),
                        Placeholder.parsed("amount", String.valueOf(itemPrice.amount())));
            }
            case PriceRequirement.MoneyPrice moneyPrice -> {
                messages.send(player, "trade.not-enough-money",
                        Placeholder.parsed("amount", String.format("%.0f", moneyPrice.amount())));
            }
            case PriceRequirement.DonatePointsPrice pointsPrice -> {
                messages.send(player, "trade.not-enough-points",
                        Placeholder.parsed("amount", String.valueOf(pointsPrice.amount())));
            }
            case PriceRequirement.TaskPrice taskPrice -> {
                String taskKey = taskPrice.taskType() + ":" + taskPrice.target();
                int current = taskRepository.getProgress(player.getUniqueId(), taskKey);
                messages.send(player, "trade.task-not-completed",
                        Placeholder.parsed("task", taskPrice.description()),
                        Placeholder.parsed("current", String.valueOf(current)),
                        Placeholder.parsed("required", String.valueOf(taskPrice.requiredCount())));
            }
        }
    }
}
