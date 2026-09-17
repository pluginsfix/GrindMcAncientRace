package pluginsfix.grindmcancientrace.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.domain.VillagerTrade;

import java.util.List;
import java.util.UUID;

public final class AncientRaceGuiHolder implements InventoryHolder {
    private final UUID villagerUuid;
    private final Profession profession;
    private final List<VillagerTrade> trades;
    private Inventory inventory;

    public AncientRaceGuiHolder(UUID villagerUuid, Profession profession, List<VillagerTrade> trades) {
        this.villagerUuid = villagerUuid;
        this.profession = profession;
        this.trades = trades;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public UUID villagerUuid() {
        return villagerUuid;
    }

    public Profession profession() {
        return profession;
    }

    public List<VillagerTrade> trades() {
        return trades;
    }
}
