package pluginsfix.grindmcancientrace.config;

import pluginsfix.grindmcancientrace.domain.PriceRequirement;
import pluginsfix.grindmcancientrace.domain.Profession;
import pluginsfix.grindmcancientrace.domain.TradeReward;

import java.util.List;

public record ProfessionConfig(
        Profession profession,
        String displayName,
        String villagerProfession,
        List<TradeReward> rewardsPool,
        List<PriceRequirement> pricesPool
) {
}
