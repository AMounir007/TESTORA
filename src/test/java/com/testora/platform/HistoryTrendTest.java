package com.testora.platform;

import com.testora.execution.history.HistoryRecord;
import com.testora.execution.history.TrendAnalyzer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
class HistoryTrendTest {

    private static HistoryRecord rec(String run, String test, boolean pass, int retries, String fp) {
        return new HistoryRecord(run, test, pass ? "PASSED" : "FAILED", "t", 10, retries, 0, "UNKNOWN", fp);
    }

    @Test
    void detectsFlakyBrokenAndHiddenRetries() {
        List<HistoryRecord> all = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            all.add(rec("old" + i, "Flaky.test", i != 2, 0, i != 2 ? "-" : "fpA"));
            all.add(rec("old" + i, "Broken.test", false, 0, "fpB"));
        }
        all.add(rec("now", "Helped.test", true, 2, "-"));
        all.add(rec("now", "Broken.test", false, 0, "fpB"));
        all.add(rec("now", "Brand.new", false, 0, "fpC"));

        var t = TrendAnalyzer.compute(all, "now");

        assertThat(t.flaky()).anyMatch(s -> s.startsWith("Flaky.test"));
        assertThat(t.broken()).anyMatch(s -> s.startsWith("Broken.test"));
        assertThat(t.flaky()).noneMatch(s -> s.startsWith("Broken.test"));
        assertThat(t.passedWithHelp()).anyMatch(s -> s.startsWith("Helped.test"));
        assertThat(t.recurringFingerprints()).containsExactly("fpB");
        assertThat(t.newFingerprints()).containsExactly("fpC");
    }
}
