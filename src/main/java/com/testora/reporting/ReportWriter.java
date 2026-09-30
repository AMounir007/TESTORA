package com.testora.reporting;

import com.testora.api.contracts.ApiCoverage;
import com.testora.core.utilities.Jsonl;
import com.testora.core.utilities.Masker;
import com.testora.execution.history.TrendAnalyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/** Writes report.jsonl (machine readable) and report.html (humans). Failures are grouped by fingerprint. */
public final class ReportWriter {
    private static final List<TestSummary> ALL = new CopyOnWriteArrayList<>();

    private ReportWriter() { }

    public static void add(TestSummary summary) {
        ALL.add(summary);
        Jsonl.append(Jsonl.outputDir().resolve("report.jsonl"), summary);
    }

    public static void writeHtml() {
        StringBuilder html = new StringBuilder("<html><head><meta charset='utf-8'><title>TESTORA Report</title>"
                + "<style>body{font-family:sans-serif}td,th{border:1px solid #ccc;padding:4px}.FAILED{background:#fdd}</style>"
                + "</head><body><h1>TESTORA Report</h1>");
        long failed = ALL.stream().filter(s -> s.status().equals("FAILED")).count();
        html.append("<p>Tests: ").append(ALL.size()).append(" &middot; Failed: ").append(failed).append("</p>");

        Map<String, List<TestSummary>> groups = ALL.stream().filter(s -> s.status().equals("FAILED"))
                .collect(Collectors.groupingBy(TestSummary::fingerprint, TreeMap::new, Collectors.toList()));
        html.append("<h2>Failure fingerprints</h2><ul>");
        groups.forEach((fp, list) -> html.append("<li><b>").append(fp).append("</b>: ").append(list.size())
                .append(" test(s), ").append(list.get(0).category()).append(" (rule confidence ")
                .append(list.get(0).confidence()).append(")</li>"));
        html.append("</ul>").append(TrendAnalyzer.html()).append(ApiCoverage.html());
        html.append("<h2>Tests</h2><table><tr><th>Test</th><th>Status</th><th>ms</th><th>Retries</th>"
                + "<th>Recoveries</th><th>Category</th><th>Evidence</th><th>Why</th></tr>");
        for (TestSummary s : ALL) {
            html.append("<tr class='").append(s.status()).append("'><td>").append(esc(s.testId())).append("</td><td>")
                    .append(s.status()).append("</td><td>").append(s.durationMs()).append("</td><td>")
                    .append(s.retries()).append("</td><td>").append(s.recoveries()).append("</td><td>")
                    .append(s.category()).append("</td><td>").append(s.evidence().size()).append(" file(s)</td><td>")
                    .append(s.why().stream().map(ReportWriter::esc).collect(Collectors.joining("<br>")))
                    .append("</td></tr>");
        }
        html.append("</table></body></html>");
        try {
            Path file = Jsonl.outputDir().resolve("report.html");
            Files.createDirectories(file.getParent());
            Files.writeString(file, Masker.mask(html.toString()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("TESTORA: could not write HTML report: " + e);
        }
    }

    private static String esc(String s) { return s.replace("&", "&amp;").replace("<", "&lt;"); }
}
