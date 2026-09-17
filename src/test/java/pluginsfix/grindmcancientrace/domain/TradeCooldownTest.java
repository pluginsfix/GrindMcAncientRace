package pluginsfix.grindmcancientrace.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeCooldownTest {

    @Test
    void formatsHoursAndMinutesProperly() {
        long millis = (1 * 3600_000L) + (30 * 60_000L);
        long totalSeconds = millis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        String formatted = String.format("%02d ч. %02d мин.", hours, minutes);

        assertThat(formatted).isEqualTo("01 ч. 30 мин.");
    }

    @Test
    void formatsMinutesAndSecondsProperly() {
        long millis = (45 * 60_000L) + (12 * 1000L);
        long totalSeconds = millis / 1000L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        String formatted = String.format("%02d мин. %02d сек.", minutes, seconds);

        assertThat(formatted).isEqualTo("45 мин. 12 сек.");
    }
}
