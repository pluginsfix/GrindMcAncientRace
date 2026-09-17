package pluginsfix.grindmcancientrace.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pluginsfix.grindmcancientrace.domain.EggManager;
import pluginsfix.grindmcancientrace.text.Messages;

public final class SpawnerEggListener implements Listener {
    private final EggManager eggManager;
    private final Messages messages;

    public SpawnerEggListener(EggManager eggManager, Messages messages) {
        this.eggManager = eggManager;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractSpawner(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getType() != Material.SPAWNER) return;

        ItemStack item = event.getItem();
        if (item == null || !eggManager.isAncientEgg(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        messages.send(player, "spawner.blocked");
    }
}
