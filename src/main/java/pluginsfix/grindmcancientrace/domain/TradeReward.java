package pluginsfix.grindmcancientrace.domain;

import java.util.List;
import java.util.Map;

public sealed interface TradeReward {
    String id();
    RewardType type();

    record ItemReward(
            String id,
            String material,
            int amount,
            String customName,
            List<String> lore,
            Map<String, Integer> enchants,
            Integer customModelData
    ) implements TradeReward {
        @Override
        public RewardType type() {
            return RewardType.ITEM;
        }
    }

    record EffectReward(
            String id,
            String effectType,
            int amplifier,
            long durationHours,
            String iconMaterial,
            String customName,
            List<String> lore
    ) implements TradeReward {
        @Override
        public RewardType type() {
            return RewardType.EFFECT;
        }
    }

    record MoneyReward(
            String id,
            double amount,
            String iconMaterial,
            String customName,
            List<String> lore
    ) implements TradeReward {
        @Override
        public RewardType type() {
            return RewardType.MONEY;
        }
    }

    record DonatePointsReward(
            String id,
            int amount,
            String iconMaterial,
            String customName,
            List<String> lore
    ) implements TradeReward {
        @Override
        public RewardType type() {
            return RewardType.DONATE_POINTS;
        }
    }

    record CommandReward(
            String id,
            List<String> commands,
            String iconMaterial,
            String customName,
            List<String> lore
    ) implements TradeReward {
        @Override
        public RewardType type() {
            return RewardType.COMMAND;
        }
    }
}
