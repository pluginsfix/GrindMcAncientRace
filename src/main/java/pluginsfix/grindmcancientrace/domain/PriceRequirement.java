package pluginsfix.grindmcancientrace.domain;

public sealed interface PriceRequirement {
    String id();
    PriceType type();
    String description();

    record ItemPrice(
            String id,
            String material,
            int amount,
            Integer customModelData,
            String description
    ) implements PriceRequirement {
        @Override
        public PriceType type() {
            return PriceType.ITEM;
        }
    }

    record MoneyPrice(
            String id,
            double amount,
            String description
    ) implements PriceRequirement {
        @Override
        public PriceType type() {
            return PriceType.MONEY;
        }
    }

    record DonatePointsPrice(
            String id,
            int amount,
            String description
    ) implements PriceRequirement {
        @Override
        public PriceType type() {
            return PriceType.DONATE_POINTS;
        }
    }

    record TaskPrice(
            String id,
            String taskType,
            String target,
            int requiredCount,
            String description
    ) implements PriceRequirement {
        @Override
        public PriceType type() {
            return PriceType.TASK;
        }
    }
}
