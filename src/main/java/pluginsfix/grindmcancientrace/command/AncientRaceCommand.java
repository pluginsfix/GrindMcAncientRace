package pluginsfix.grindmcancientrace.command;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pluginsfix.grindmcancientrace.domain.EggManager;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.hook.FancyHologramsHook;
import pluginsfix.grindmcancientrace.listener.VillagerSpawnListener;
import pluginsfix.grindmcancientrace.text.Messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class AncientRaceCommand implements CommandExecutor, TabCompleter {
    private final Messages messages;
    private final EggManager eggManager;
    private final VillagerSpawnListener spawnListener;
    private final FancyHologramsHook hologramsHook;
    private final Runnable reloadAction;

    public AncientRaceCommand(
            Messages messages,
            EggManager eggManager,
            VillagerSpawnListener spawnListener,
            FancyHologramsHook hologramsHook,
            Runnable reloadAction
    ) {
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
        messages.send(player, "command.spawned", Placeholder.parsed("profession", profDisplayName));
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
                Placeholder.parsed("profession", profName),
                Placeholder.parsed("player", target.getName()),
                Placeholder.parsed("amount", String.valueOf(amount)));

        messages.send(target, "command.received-egg",
                Placeholder.parsed("profession", profName),
                Placeholder.parsed("amount", String.valueOf(amount)));
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
            messages.send(player, "command.removed", Placeholder.parsed("count", String.valueOf(count)));
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
        messages.send(sender, "command.invalid-profession", Placeholder.parsed("professions", available));
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
            if (sender.hasPermission("grindmcancientrace.admin.remove")) subs.add("remove");
            if (sender.hasPermission("grindmcancientrace.admin.reload")) subs.add("reload");
            return filterPrefix(subs, args[0]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            List<String> profs = new ArrayList<>(Arrays.stream(Profession.values()).map(Profession::key).toList());
            profs.add("random");
            return filterPrefix(profs, args[1]);
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
