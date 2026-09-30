package com.testora.intelligence.knowledge;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight quality knowledge graph: nodes are plain ids ("api:customer", "test:CustomerWebTest.create"),
 * edges mean "is depended on by" / "is covered by". Answers "what is affected by X?" via reachability.
 * Edges are populated by your own metadata or test annotations; nothing is guessed.
 */
public final class KnowledgeGraph {
    private final Map<String, Set<String>> edges = new HashMap<>();

    public KnowledgeGraph link(String from, String to) {
        edges.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
        return this;
    }

    /** Everything reachable from the changed node, optionally filtered by id prefix (e.g. "test:"). */
    public Set<String> affectedBy(String changed, String prefix) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(changed);
        while (!queue.isEmpty()) {
            for (String next : edges.getOrDefault(queue.poll(), Set.of())) {
                if (seen.add(next)) queue.add(next);
            }
        }
        seen.removeIf(n -> !n.startsWith(prefix));
        return seen;
    }
}
