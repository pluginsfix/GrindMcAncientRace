package pluginsfix.grindmcancientrace.domain;

public record VillagerTrade(
        int slotIndex,
        TradeReward reward,
        PriceRequirement price
) {
    public VillagerTrade {
        if (slotIndex < 0) {
            throw new IllegalArgumentException("Slot index cannot be negative");
        }
        if (reward == null) {
            throw new IllegalArgumentException("Reward cannot be null");
        }
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
    }
}
