package com.testora.platform;

import com.testora.config.WaitPolicy;
import com.testora.core.exceptions.TestoraException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("smoke")
class WaitPolicyTest {

    private static WaitPolicy web(Map<String, String> settings) {
        return WaitPolicy.resolve("web", WaitPolicy.webDefaults(), settings::get);
    }

    @Test
    void noSettingsKeepsDefaults() {
        assertThat(web(Map.of())).isEqualTo(WaitPolicy.webDefaults());
    }

    @Test
    void parsesDurationUnits() {
        assertThat(WaitPolicy.parseDuration("k", "300ms")).isEqualTo(Duration.ofMillis(300));
        assertThat(WaitPolicy.parseDuration("k", "15s")).isEqualTo(Duration.ofSeconds(15));
        assertThat(WaitPolicy.parseDuration("k", "2m")).isEqualTo(Duration.ofMinutes(2));
        assertThat(WaitPolicy.parseDuration("k", "20")).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void appliesOverridesPerChannelOnly() {
        Map<String, String> settings = Map.of("wait.web.timeout", "5s", "wait.web.polling", "100ms",
                "wait.web.ignoreStale", "false", "wait.mobile.timeout", "99s");
        WaitPolicy web = web(settings);
        assertThat(web.timeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(web.pollingInterval()).isEqualTo(Duration.ofMillis(100));
        assertThat(web.ignoreStale()).isFalse();
        WaitPolicy mobile = WaitPolicy.resolve("mobile", WaitPolicy.mobileDefaults(), settings::get);
        assertThat(mobile.timeout()).isEqualTo(Duration.ofSeconds(99));
        assertThat(mobile.pollingInterval()).isEqualTo(WaitPolicy.mobileDefaults().pollingInterval());
    }

    @Test
    void mobileIsSlowerThanWebByDefault() {
        assertThat(WaitPolicy.mobileDefaults().timeout()).isGreaterThan(WaitPolicy.webDefaults().timeout());
    }

    @Test
    void rejectsBadValuesWithActionableMessages() {
        assertThatThrownBy(() -> web(Map.of("wait.web.timeout", "abc")))
                .isInstanceOf(TestoraException.class).hasMessageContaining("wait.web.timeout").hasMessageContaining("abc");
        assertThatThrownBy(() -> web(Map.of("wait.web.polling", "20s")))
                .isInstanceOf(TestoraException.class).hasMessageContaining("longer than timeout");
        assertThatThrownBy(() -> web(Map.of("wait.web.timeout", "0s")))
                .isInstanceOf(TestoraException.class).hasMessageContaining("greater than 0");
        assertThatThrownBy(() -> web(Map.of("wait.web.ignoreStale", "maybe")))
                .isInstanceOf(TestoraException.class).hasMessageContaining("true or false");
        assertThatThrownBy(() -> web(Map.of("wait.web.maxStaleRecoveries", "-1")))
                .isInstanceOf(TestoraException.class);
    }
}
