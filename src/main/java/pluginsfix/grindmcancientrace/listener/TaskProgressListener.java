package pluginsfix.grindmcancientrace.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import pluginsfix.grindmcancientrace.storage.TaskProgressRepository;

public final class TaskProgressListener implements Listener {
    private final TaskProgressRepository taskRepository;

    public TaskProgressListener(TaskProgressRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        String mobType = event.getEntityType().name();
        String taskKey = "KILL_MOB:" + mobType;
        taskRepository.incrementProgress(killer.getUniqueId(), taskKey, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        String blockType = event.getBlock().getType().name();
        String taskKey = "MINE_BLOCK:" + blockType;
        taskRepository.incrementProgress(player.getUniqueId(), taskKey, 1);
    }
}
