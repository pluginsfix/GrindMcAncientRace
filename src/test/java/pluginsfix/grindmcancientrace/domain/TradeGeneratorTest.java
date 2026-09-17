package pluginsfix.grindmcancientrace.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TradeGeneratorTest {

    @Test
    void generatesRequestedNumberOfTradesWithUniquePrices() {
        TradeGenerator generator = new TradeGenerator();

        List<TradeReward> rewards = new ArrayList<>();
        List<PriceRequirement> prices = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            rewards.add(new TradeReward.ItemReward("reward_" + i, "DIAMOND", 1, "Reward " + i, List.of(), null, null));
            prices.add(new PriceRequirement.MoneyPrice("price_" + i, (i + 1) * 1000.0, "Price " + i));
        }

        List<VillagerTrade> trades = generator.generateTrades(rewards, prices, 8);

        assertThat(trades).hasSize(8);

        Set<String> seenPriceIds = new HashSet<>();
        for (VillagerTrade trade : trades) {
            assertThat(seenPriceIds).doesNotContain(trade.price().id());
            seenPriceIds.add(trade.price().id());
        }
        assertThat(seenPriceIds).hasSize(8);
    }

    @Test
    void handlesEmptyInputGracefully() {
        TradeGenerator generator = new TradeGenerator();
        List<VillagerTrade> trades = generator.generateTrades(List.of(), List.of(), 8);
        assertThat(trades).isEmpty();
    }
}
