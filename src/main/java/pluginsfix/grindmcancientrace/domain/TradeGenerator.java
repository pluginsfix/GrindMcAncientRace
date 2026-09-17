package pluginsfix.grindmcancientrace.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class TradeGenerator {
    private final Random random;

    public TradeGenerator() {
        this(new Random());
    }

    public TradeGenerator(Random random) {
        this.random = random;
    }

    public List<VillagerTrade> generateTrades(
            List<TradeReward> availableRewards,
            List<PriceRequirement> availablePrices,
            int count
    ) {
        if (availableRewards == null || availableRewards.isEmpty()) {
            return List.of();
        }
        if (availablePrices == null || availablePrices.isEmpty()) {
            return List.of();
        }

        List<TradeReward> shuffledRewards = new ArrayList<>(availableRewards);
        Collections.shuffle(shuffledRewards, random);

        List<PriceRequirement> shuffledPrices = new ArrayList<>(availablePrices);
        Collections.shuffle(shuffledPrices, random);

        int tradeCount = Math.min(count, Math.min(shuffledRewards.size(), shuffledPrices.size()));
        if (tradeCount <= 0) {
            tradeCount = Math.min(count, shuffledRewards.size());
        }

        List<VillagerTrade> trades = new ArrayList<>(tradeCount);
        for (int i = 0; i < tradeCount; i++) {
            TradeReward reward = shuffledRewards.get(i % shuffledRewards.size());
            PriceRequirement price = shuffledPrices.get(i % shuffledPrices.size());
            trades.add(new VillagerTrade(i, reward, price));
        }

        return Collections.unmodifiableList(trades);
    }
}
