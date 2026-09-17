package pluginsfix.grindmcancientrace.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pluginsfix.grindmcancientrace.config.PluginConfig;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.domain.TradeReward;
import pluginsfix.grindmcancientrace.domain.VillagerTrade;
import pluginsfix.grindmcancientrace.storage.TradeCooldownRepository;
import pluginsfix.grindmcancientrace.text.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

public final class AncientRaceGui {
    private final PluginConfig config;
    private final Messages messages;
    private final TradeCooldownRepository cooldownRepository;
    private final MiniMessage miniMessage;

    public AncientRaceGui(PluginConfig config, Messages messages, TradeCooldownRepository cooldownRepository) {
        this.config = config;
        this.messages = messages;
        this.cooldownRepository = cooldownRepository;
        this.miniMessage = messages.miniMessage();
    }

    public void open(Player player, UUID villagerUuid, Profession profession, List<VillagerTrade> trades) {
        String profDisplayName = messages.getRaw("professions." + profession.key());
        Component title = messages.parse(config.guiTitle(), Placeholder.parsed("profession", profDisplayName));

        AncientRaceGuiHolder holder = new AncientRaceGuiHolder(villagerUuid, profession, trades);
        Inventory inv = Bukkit.createInventory(holder, config.guiSize(), title);
        holder.setInventory(inv);

        ItemStack filler = createFillerItem();
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        long now = System.currentTimeMillis();
        List<Integer> slots = config.tradeSlots();
        for (int i = 0; i < trades.size() && i < slots.size(); i++) {
            VillagerTrade trade = trades.get(i);
            int slot = slots.get(i);

            OptionalLong cooldownOpt = cooldownRepository.getCooldownRemaining(player.getUniqueId(), villagerUuid, i, now);
            if (cooldownOpt.isPresent()) {
                inv.setItem(slot, buildCooldownItem(cooldownOpt.getAsLong()));
            } else {
                inv.setItem(slot, buildTradeDisplayItem(trade));
            }
        }

        player.openInventory(inv);
    }

    public ItemStack buildCooldownItem(long remainingMillis) {
        Material mat = Material.matchMaterial(config.cooldownMaterial());
        if (mat == null) mat = Material.BARRIER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String formattedTime = cooldownRepository.formatCooldown(remainingMillis);
        meta.displayName(messages.parse(config.cooldownName()).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : config.cooldownLore()) {
            lore.add(messages.parse(line, Placeholder.parsed("time", formattedTime)).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFillerItem() {
        Material mat = Material.matchMaterial(config.fillerMaterial());
        if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.parse(config.fillerName()).decoration(TextDecoration.ITALIC, false));
            meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack buildTradeDisplayItem(VillagerTrade trade) {
        TradeReward reward = trade.reward();
        Material mat = getMaterialForReward(reward);
        int amount = getAmountForReward(reward);

        ItemStack item = new ItemStack(mat, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String customName = getRewardCustomName(reward);
        if (customName != null && !customName.isEmpty()) {
            meta.displayName(messages.parse(customName).decoration(TextDecoration.ITALIC, false));
        }

        if (reward instanceof TradeReward.ItemReward itemReward) {
            if (itemReward.customModelData() != null) {
                meta.setCustomModelData(itemReward.customModelData());
            }
            if (itemReward.enchants() != null) {
                for (var entry : itemReward.enchants().entrySet()) {
                    Enchantment ench = resolveEnchantment(entry.getKey());
                    if (ench != null) {
                        meta.addEnchant(ench, entry.getValue(), true);
                    }
                }
            }
        }

        List<Component> finalLore = new ArrayList<>();

        for (String line : config.loreHeader()) {
            String processed = line.replace("<reward_type>", reward.type().name());
            finalLore.add(messages.parse(processed).decoration(TextDecoration.ITALIC, false));
        }

        List<String> baseLore = getRewardBaseLore(reward);
        for (String line : baseLore) {
            finalLore.add(messages.parse(line).decoration(TextDecoration.ITALIC, false));
        }

        if (!baseLore.isEmpty()) {
            finalLore.add(Component.empty());
        }

        for (String line : config.lorePriceSection()) {
            String processed = line.replace("<price_description>", trade.price().description());
            finalLore.add(messages.parse(processed).decoration(TextDecoration.ITALIC, false));
        }

        for (String line : config.loreStatusSection()) {
            finalLore.add(messages.parse(line).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(finalLore);
        meta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public static Enchantment resolveEnchantment(String name) {
        if (name == null || name.isEmpty()) return null;
        Enchantment ench = Enchantment.getByName(name.toUpperCase());
        if (ench != null) return ench;
        try {
            return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Material getMaterialForReward(TradeReward reward) {
        String matName = switch (reward) {
            case TradeReward.ItemReward item -> item.material();
            case TradeReward.EffectReward effect -> effect.iconMaterial();
            case TradeReward.MoneyReward money -> money.iconMaterial();
            case TradeReward.DonatePointsReward points -> points.iconMaterial();
            case TradeReward.CommandReward cmd -> cmd.iconMaterial();
        };

        Material mat = Material.matchMaterial(matName);
        return mat != null ? mat : Material.GOLD_INGOT;
    }

    private int getAmountForReward(TradeReward reward) {
        if (reward instanceof TradeReward.ItemReward item) {
            return item.amount();
        }
        return 1;
    }

    private String getRewardCustomName(TradeReward reward) {
        return switch (reward) {
            case TradeReward.ItemReward item -> item.customName();
            case TradeReward.EffectReward effect -> effect.customName();
            case TradeReward.MoneyReward money -> money.customName();
            case TradeReward.DonatePointsReward points -> points.customName();
            case TradeReward.CommandReward cmd -> cmd.customName();
        };
    }

    private List<String> getRewardBaseLore(TradeReward reward) {
        return switch (reward) {
            case TradeReward.ItemReward item -> item.lore();
            case TradeReward.EffectReward effect -> effect.lore();
            case TradeReward.MoneyReward money -> money.lore();
            case TradeReward.DonatePointsReward points -> points.lore();
            case TradeReward.CommandReward cmd -> cmd.lore();
        };
    }
}
