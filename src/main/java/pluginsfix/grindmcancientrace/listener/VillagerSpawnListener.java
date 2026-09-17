package pluginsfix.grindmcancientrace.listener;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import pluginsfix.grindmcancientrace.config.PluginConfig;
import pluginsfix.grindmcancientrace.config.ProfessionConfig;
import pluginsfix.grindmcancientrace.domain.EggManager;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.domain.TradeGenerator;
import pluginsfix.grindmcancientrace.domain.VillagerTrade;
import pluginsfix.grindmcancientrace.hook.FancyHologramsHook;
import pluginsfix.grindmcancientrace.storage.TradeRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class VillagerSpawnListener implements Listener {
    private final Plugin plugin;
    private final PluginConfig config;
    private final EggManager eggManager;
    private final TradeRepository tradeRepository;
    private final TradeGenerator tradeGenerator;
    private final FancyHologramsHook hologramsHook;

    public VillagerSpawnListener(
            Plugin plugin,
            PluginConfig config,
            EggManager eggManager,
            TradeRepository tradeRepository,
            TradeGenerator tradeGenerator,
            FancyHologramsHook hologramsHook
    ) {
        this.plugin = plugin;
        this.config = config;
        this.eggManager = eggManager;
        this.tradeRepository = tradeRepository;
        this.tradeGenerator = tradeGenerator;
        this.hologramsHook = hologramsHook;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerUseAncientEgg(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() == Material.SPAWNER) return;

        ItemStack item = event.getItem();
        if (item == null || !eggManager.isAncientEgg(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            item.subtract(1);
        }

        Location spawnLoc = clickedBlock.getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        Optional<Profession> profOpt = eggManager.getEggProfession(item);
        Profession profession = profOpt.orElseGet(this::getRandomProfession);

        spawnAncientVillager(spawnLoc, profession);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() != EntityType.VILLAGER) return;
        if (!(event.getEntity() instanceof Villager villager)) return;

        if (eggManager.isAncientVillager(villager)) return;

        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (reason == CreatureSpawnEvent.SpawnReason.CUSTOM || reason == CreatureSpawnEvent.SpawnReason.COMMAND) {
            return;
        }

        double roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0);
        if (roll < config.spawnChancePercent()) {
            Profession profession = getRandomProfession();
            convertVillager(villager, profession);
        }
    }

    public Villager spawnAncientVillager(Location location, Profession profession) {
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        convertVillager(villager, profession);
        return villager;
    }

    public void convertVillager(Villager villager, Profession profession) {
        eggManager.tagAncientVillager(villager, profession);
        setVillagerProfession(villager, profession);

        Optional<ProfessionConfig> profConfigOpt = config.getProfessionConfig(profession);
        if (profConfigOpt.isPresent()) {
            ProfessionConfig pConfig = profConfigOpt.get();
            List<VillagerTrade> trades = tradeGenerator.generateTrades(pConfig.rewardsPool(), pConfig.pricesPool(), 8);
            tradeRepository.saveTrades(villager.getUniqueId(), profession, trades);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (villager.isValid()) {
                hologramsHook.createOrUpdateHologram(villager, profession);
            }
        }, 2L);
    }

    private void setVillagerProfession(Villager villager, Profession profession) {
        villager.setAge(0);
        villager.setAgeLock(true);
        try {
            Villager.Profession vProf = Villager.Profession.valueOf(profession.vanillaProfession());
            villager.setProfession(vProf);
        } catch (IllegalArgumentException ignored) {
            villager.setProfession(Villager.Profession.NONE);
        }
    }

    private Profession getRandomProfession() {
        Profession[] values = Profession.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }
}
