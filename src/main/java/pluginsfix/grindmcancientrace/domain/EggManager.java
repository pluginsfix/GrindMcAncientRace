package pluginsfix.grindmcancientrace.domain;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import pluginsfix.grindmcancientrace.text.Messages;

import java.util.List;
import java.util.Optional;

public final class EggManager {
    private final Plugin plugin;
    private final Messages messages;
    private final NamespacedKey eggKey;
    private final NamespacedKey professionKey;
    private final NamespacedKey villagerKey;

    public EggManager(Plugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.eggKey = new NamespacedKey(plugin, "ancient_egg");
        this.professionKey = new NamespacedKey(plugin, "profession");
        this.villagerKey = new NamespacedKey(plugin, "ancient_villager");
    }

    public ItemStack createEgg(Profession profession, int amount) {
        ItemStack item = new ItemStack(Material.VILLAGER_SPAWN_EGG, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        MiniMessage mm = messages.miniMessage();
        String profName = (profession != null)
                ? messages.getRaw("professions." + profession.key())
                : messages.getRaw("professions.random");

        meta.displayName(mm.deserialize(messages.getRaw("egg.name")));

        List<Component> lore = messages.getRawList("egg.lore").stream()
                .map(line -> mm.deserialize(line, Placeholder.parsed("profession", profName)))
                .toList();
        meta.lore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(eggKey, PersistentDataType.BYTE, (byte) 1);
        if (profession != null) {
            pdc.set(professionKey, PersistentDataType.STRING, profession.key());
        }

        item.setItemMeta(meta);
        return item;
    }

    public boolean isAncientEgg(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.has(eggKey, PersistentDataType.BYTE);
    }

    public Optional<Profession> getEggProfession(ItemStack item) {
        if (!isAncientEgg(item)) return Optional.empty();
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String key = pdc.get(professionKey, PersistentDataType.STRING);
        if (key == null) return Optional.empty();
        return Profession.fromKey(key);
    }

    public void tagAncientVillager(org.bukkit.entity.Villager villager, Profession profession) {
        if (villager == null) return;
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        pdc.set(villagerKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(professionKey, PersistentDataType.STRING, profession.key());
    }

    public boolean isAncientVillager(org.bukkit.entity.Entity entity) {
        if (!(entity instanceof org.bukkit.entity.Villager villager)) return false;
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        return pdc.has(villagerKey, PersistentDataType.BYTE);
    }

    public Optional<Profession> getVillagerProfession(org.bukkit.entity.Villager villager) {
        if (!isAncientVillager(villager)) return Optional.empty();
        PersistentDataContainer pdc = villager.getPersistentDataContainer();
        String key = pdc.get(professionKey, PersistentDataType.STRING);
        if (key == null) return Optional.empty();
        return Profession.fromKey(key);
    }

    public NamespacedKey villagerKey() {
        return villagerKey;
    }
}
