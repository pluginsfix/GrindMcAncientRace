package pluginsfix.grindmcancientrace.listener;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import pluginsfix.grindmcancientrace.domain.EggManager;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.hook.FancyHologramsHook;
import pluginsfix.grindmcancientrace.storage.TradeRepository;

import java.util.Optional;

public final class VillagerLifecycleListener implements Listener {
    private final EggManager eggManager;
    private final FancyHologramsHook hologramsHook;
    private final TradeRepository tradeRepository;

    public VillagerLifecycleListener(
            EggManager eggManager,
            FancyHologramsHook hologramsHook,
            TradeRepository tradeRepository
    ) {
        this.eggManager = eggManager;
        this.hologramsHook = hologramsHook;
        this.tradeRepository = tradeRepository;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager)) return;
        if (!eggManager.isAncientVillager(villager)) return;

        hologramsHook.removeHologram(villager.getUniqueId());
        tradeRepository.removeVillager(villager.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Villager villager && eggManager.isAncientVillager(villager)) {
                Optional<Profession> profOpt = eggManager.getVillagerProfession(villager);
                profOpt.ifPresent(profession -> hologramsHook.createOrUpdateHologram(villager, profession));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Villager villager && eggManager.isAncientVillager(villager)) {
                hologramsHook.removeHologram(villager.getUniqueId());
            }
        }
    }
}
