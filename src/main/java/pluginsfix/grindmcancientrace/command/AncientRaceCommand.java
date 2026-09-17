package pluginsfix.grindmcancientrace.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import pluginsfix.grindmcancientrace.domain.EggManager;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.hook.FancyHologramsHook;
import pluginsfix.grindmcancientrace.listener.VillagerSpawnListener;
import pluginsfix.grindmcancientrace.text.Messages;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AncientRaceCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final Messages messages;
    private final EggManager eggManager;
    private final VillagerSpawnListener spawnListener;
    private final FancyHologramsHook hologramsHook;
    private final Runnable reloadAction;

    public AncientRaceCommand(
            Plugin plugin,
            Messages messages,
            EggManager eggManager,
            VillagerSpawnListener spawnListener,
            FancyHologramsHook hologramsHook,
            Runnable reloadAction
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.eggManager = eggManager;
        this.spawnListener = spawnListener;
        this.hologramsHook = hologramsHook;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("grindmcancientrace.use")) {
            messages.send(sender, "command.no-permission");
            return true;
        }

        if (args.length == 0) {
            messages.send(sender, "command.usage");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "spawn" -> handleSpawn(sender, args);
            case "give" -> handleGive(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "additem" -> handleAddItem(sender, args);
            case "reload" -> handleReload(sender);
            default -> messages.send(sender, "command.usage");
        }

        return true;
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("grindmcancientrace.admin.spawn")) {
            messages.send(sender, "command.no-permission");
            return;
        }

        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return;
        }

        if (args.length < 2) {
            messages.send(sender, "command.spawn-usage");
            return;
        }

        Profession profession = null;
        if (!args[1].equalsIgnoreCase("random")) {
            Optional<Profession> profOpt = Profession.fromKey(args[1]);
            if (profOpt.isEmpty()) {
                sendInvalidProfession(sender);
                return;
            }
            profession = profOpt.get();
        } else {
            Profession[] values = Profession.values();
            profession = values[(int) (Math.random() * values.length)];
        }

        spawnListener.spawnAncientVillager(player.getLocation(), profession);
        String profDisplayName = messages.getRaw("professions." + profession.key());
        messages.send(player, "command.spawned", messages.tag("profession", profDisplayName));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("grindmcancientrace.admin.give")) {
            messages.send(sender, "command.no-permission");
            return;
        }

        if (args.length < 3) {
            messages.send(sender, "command.give-usage");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messages.send(sender, "command.player-not-found");
            return;
        }

        Profession profession = null;
        if (!args[2].equalsIgnoreCase("random")) {
            Optional<Profession> profOpt = Profession.fromKey(args[2]);
            if (profOpt.isEmpty()) {
                sendInvalidProfession(sender);
                return;
            }
            profession = profOpt.get();
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
                if (amount <= 0) {
                    messages.send(sender, "command.invalid-amount");
                    return;
                }
            } catch (NumberFormatException e) {
                messages.send(sender, "command.invalid-amount");
                return;
            }
        }

        ItemStack egg = eggManager.createEgg(profession, amount);
        target.getInventory().addItem(egg);

        String profName = (profession != null)
                ? messages.getRaw("professions." + profession.key())
                : messages.getRaw("professions.random");

        messages.send(sender, "command.given-egg",
                messages.tag("profession", profName),
                messages.tag("player", target.getName()),
                messages.tag("amount", String.valueOf(amount)));

        messages.send(target, "command.received-egg",
                messages.tag("profession", profName),
                messages.tag("amount", String.valueOf(amount)));
    }

    private void handleAddItem(CommandSender sender, String[] args) {
        if (!sender.hasPermission("grindmcancientrace.admin.additem")) {
            messages.send(sender, "command.no-permission");
            return;
        }

        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return;
        }

        if (args.length < 3) {
            messages.send(sender, "command.additem-usage");
            return;
        }

        Optional<Profession> profOpt = Profession.fromKey(args[1]);
        if (profOpt.isEmpty()) {
            sendInvalidProfession(sender);
            return;
        }

        Profession profession = profOpt.get();
        String rewardId = args[2].toLowerCase().trim();

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR) {
            messages.send(player, "command.no-item-in-hand");
            return;
        }

        Map<String, Object> rewardMap = new LinkedHashMap<>();
        rewardMap.put("id", rewardId);
        rewardMap.put("type", "ITEM");
        rewardMap.put("material", handItem.getType().name());
        rewardMap.put("amount", handItem.getAmount());

        ItemMeta meta = handItem.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                Component comp = meta.displayName();
                if (comp != null) {
                    rewardMap.put("name", messages.miniMessage().serialize(comp));
                }
            }
            if (meta.hasLore()) {
                List<Component> lore = meta.lore();
                if (lore != null) {
                    List<String> serializedLore = lore.stream().map(l -> messages.miniMessage().serialize(l)).toList();
                    rewardMap.put("lore", serializedLore);
                }
            }
            if (meta.hasCustomModelData()) {
                rewardMap.put("custom-model-data", meta.getCustomModelData());
            }
            if (!meta.getEnchants().isEmpty()) {
                Map<String, Integer> enchants = new LinkedHashMap<>();
                for (var entry : meta.getEnchants().entrySet()) {
                    String enchName;
                    try {
                        enchName = entry.getKey().getKey().getKey().toUpperCase();
                    } catch (Throwable ignored) {
                        enchName = entry.getKey().getName();
                    }
                    enchants.put(enchName, entry.getValue());
                }
                rewardMap.put("enchants", enchants);
            }
        }

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);

        String path = "professions." + profession.key() + ".rewards";
        List<Map<?, ?>> currentRewards = yaml.getMapList(path);
        List<Map<?, ?>> updatedRewards = new ArrayList<>(currentRewards);
        updatedRewards.add(rewardMap);

        yaml.set(path, updatedRewards);
        try {
            yaml.save(configFile);
            reloadAction.run();

            String profDisplayName = messages.getRaw("professions." + profession.key());
            messages.send(player, "command.item-added",
                    messages.tag("profession", profDisplayName),
                    messages.tag("id", rewardId));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save config.yml after additem: " + e.getMessage());
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("grindmcancientrace.admin.remove")) {
            messages.send(sender, "command.no-permission");
            return;
        }

        if (!(sender instanceof Player player)) {
            messages.send(sender, "command.players-only");
            return;
        }

        int count = 0;
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Villager villager && eggManager.isAncientVillager(villager)) {
                hologramsHook.removeHologram(villager.getUniqueId());
                villager.remove();
                count++;
            }
        }

        if (count > 0) {
            messages.send(player, "command.removed", messages.tag("count", String.valueOf(count)));
        } else {
            messages.send(player, "command.none-nearby");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("grindmcancientrace.admin.reload")) {
            messages.send(sender, "command.no-permission");
            return;
        }

        reloadAction.run();
        messages.send(sender, "command.reloaded");
    }

    private void sendInvalidProfession(CommandSender sender) {
        String available = String.join(", ", Arrays.stream(Profession.values()).map(Profession::key).toList()) + ", random";
        messages.send(sender, "command.invalid-profession", messages.tag("professions", available));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("grindmcancientrace.use")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("grindmcancientrace.admin.spawn")) subs.add("spawn");
            if (sender.hasPermission("grindmcancientrace.admin.give")) subs.add("give");
            if (sender.hasPermission("grindmcancientrace.admin.additem")) subs.add("additem");
            if (sender.hasPermission("grindmcancientrace.admin.remove")) subs.add("remove");
            if (sender.hasPermission("grindmcancientrace.admin.reload")) subs.add("reload");
            return filterPrefix(subs, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            List<String> profs = new ArrayList<>(Arrays.stream(Profession.values()).map(Profession::key).toList());
            profs.add("random");
            return filterPrefix(profs, args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("additem")) {
            List<String> profs = new ArrayList<>(Arrays.stream(Profession.values()).map(Profession::key).toList());
            return filterPrefix(profs, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("additem")) {
            return List.of("custom_reward_1", "legendary_sword", "special_armor");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return filterPrefix(players, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> profs = new ArrayList<>(Arrays.stream(Profession.values()).map(Profession::key).toList());
            profs.add("random");
            return filterPrefix(profs, args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return List.of("1", "4", "16", "64");
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }
}
