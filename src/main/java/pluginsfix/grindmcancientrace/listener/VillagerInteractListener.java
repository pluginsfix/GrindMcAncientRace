package pluginsfix.grindmcancientrace.listener;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import pluginsfix.grindmcancientrace.config.PluginConfig;
import pluginsfix.grindmcancientrace.config.ProfessionConfig;
import pluginsfix.grindmcancientrace.domain.EggManager;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.domain.TradeGenerator;
import pluginsfix.grindmcancientrace.domain.VillagerTrade;
import pluginsfix.grindmcancientrace.gui.AncientRaceGui;
import pluginsfix.grindmcancientrace.storage.TradeRepository;

import java.util.List;
import java.util.Optional;

public final class VillagerInteractListener implements Listener {
    private final PluginConfig config;
    private final EggManager eggManager;
    private final TradeRepository tradeRepository;
    private final TradeGenerator tradeGenerator;
    private final AncientRaceGui gui;

    public VillagerInteractListener(
            PluginConfig config,
            EggManager eggManager,
            TradeRepository tradeRepository,
            TradeGenerator tradeGenerator,
            AncientRaceGui gui
    ) {
        this.config = config;
        this.eggManager = eggManager;
        this.tradeRepository = tradeRepository;
        this.tradeGenerator = tradeGenerator;
        this.gui = gui;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Villager villager)) return;

        if (!eggManager.isAncientVillager(villager)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        Optional<Profession> profOpt = eggManager.getVillagerProfession(villager);
        if (profOpt.isEmpty()) return;

        Profession profession = profOpt.get();
        Optional<List<VillagerTrade>> tradesOpt = tradeRepository.getTrades(villager.getUniqueId(), profession);

        List<VillagerTrade> trades;
        if (tradesOpt.isPresent() && !tradesOpt.get().isEmpty()) {
            trades = tradesOpt.get();
        } else {
            Optional<ProfessionConfig> profConfigOpt = config.getProfessionConfig(profession);
            if (profConfigOpt.isEmpty()) return;
            ProfessionConfig pConfig = profConfigOpt.get();
            trades = tradeGenerator.generateTrades(pConfig.rewardsPool(), pConfig.pricesPool(), 8);
            tradeRepository.saveTrades(villager.getUniqueId(), profession, trades);
        }

        gui.open(player, villager.getUniqueId(), profession, trades);
    }
}
