package pluginsfix.grindmcancientrace.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pluginsfix.grindmcancientrace.config.PluginConfig;
import pluginsfix.grindmcancientrace.domain.ActiveEffect;
import pluginsfix.grindmcancientrace.storage.EffectRepository;

import java.util.List;

public final class PlayerEffectListener implements Listener {
    private final Plugin plugin;
    private final PluginConfig config;
    private final EffectRepository effectRepository;
    private BukkitTask task;

    public PlayerEffectListener(Plugin plugin, PluginConfig config, EffectRepository effectRepository) {
        this.plugin = plugin;
        this.config = config;
        this.effectRepository = effectRepository;
        startTask();
    }

    public void startTask() {
        if (task != null) {
            task.cancel();
        }
        long intervalTicks = Math.max(20L, config.effectsCheckIntervalSeconds() * 20L);
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickEffects, intervalTicks, intervalTicks);
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyAllEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyAllEffects(event.getPlayer()), 5L);
    }

    private void tickEffects() {
        long now = System.currentTimeMillis();
        effectRepository.removeExpiredEffects(now);

        for (Player player : Bukkit.getOnlinePlayers()) {
            applyAllEffects(player);
        }
    }

    private void applyAllEffects(Player player) {
        if (player == null || !player.isOnline()) return;
        long now = System.currentTimeMillis();
        List<ActiveEffect> effects = effectRepository.getAllActiveEffects(player.getUniqueId());

        for (ActiveEffect effect : effects) {
            if (effect.isExpired(now)) continue;
            PotionEffectType type = PotionEffectType.getByName(effect.effectType());
            if (type == null) continue;

            int durationTicks = (int) Math.min(72000, effect.remainingSeconds(now) * 20L);
            if (durationTicks > 0) {
                player.addPotionEffect(new PotionEffect(type, durationTicks, effect.amplifier(), false, false, true));
            }
        }
    }
}
