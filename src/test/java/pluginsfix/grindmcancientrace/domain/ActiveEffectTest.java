package pluginsfix.grindmcancientrace.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveEffectTest {

    @Test
    void validatesEffectExpiryCorrectly() {
        UUID playerUuid = UUID.randomUUID();
        long now = 1_000_000L;
        long futureExpiry = now + 86_400_000L;

        ActiveEffect effect = new ActiveEffect(playerUuid, "SPEED", 1, futureExpiry);

        assertThat(effect.isExpired(now)).isFalse();
        assertThat(effect.isExpired(futureExpiry - 1)).isFalse();
        assertThat(effect.isExpired(futureExpiry)).isTrue();
        assertThat(effect.isExpired(futureExpiry + 1000L)).isTrue();
    }

    @Test
    void formatsRemainingTimeNicely() {
        UUID playerUuid = UUID.randomUUID();
        long now = 1_000_000L;
        long expiry = now + (2 * 3600_000L) + (15 * 60_000L);

        ActiveEffect effect = new ActiveEffect(playerUuid, "STRENGTH", 1, expiry);

        assertThat(effect.formatRemainingTime(now)).isEqualTo("02 ч. 15 мин.");
    }
}
