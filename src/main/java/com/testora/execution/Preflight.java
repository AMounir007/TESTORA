package com.testora.execution;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Environment readiness ("Execution Ready" / "Execution Blocked") so an unhealthy environment does not
 * produce hundreds of misleading failures. Call from a suite setup or CI step before running tests.
 */
public final class Preflight {
    public record Report(boolean ready, List<String> blockers) {
        @Override public String toString() {
            return ready ? "Execution Ready" : "Execution Blocked\n  Reason: " + String.join("\n  Reason: ", blockers);
        }
    }

    private Preflight() { }

    public static Report run(String... urlsToCheck) {
        List<String> blockers = new ArrayList<>();
        if (Runtime.version().feature() < 21) blockers.add("Java 21+ required, found " + Runtime.version());
        if (new File(".").getUsableSpace() < 500L * 1024 * 1024) blockers.add("Less than 500 MB free disk space");
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        for (String url : urlsToCheck) {
            try {
                HttpResponse<Void> r = client.send(HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(5)).GET().build(), HttpResponse.BodyHandlers.discarding());
                if (r.statusCode() >= 500) blockers.add(url + " returned HTTP " + r.statusCode());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                blockers.add(url + " check interrupted");
            } catch (Exception e) {
                blockers.add(url + " unreachable: " + e.getClass().getSimpleName());
            }
        }
        return new Report(blockers.isEmpty(), blockers);
    }
}
