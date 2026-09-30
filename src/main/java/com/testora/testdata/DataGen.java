package com.testora.testdata;

import java.util.Random;

/**
 * Test data generation. Seeded => reproducible: same seed + same call order = same data.
 * Domain independent: business entities are built by your own data builders using these primitives.
 * Set -Dtestdata.seed=123 to reproduce a failing run.
 */
public final class DataGen {
    private final Random random;

    public DataGen(long seed) { this.random = new Random(seed); }

    public static DataGen fromConfig() {
        return new DataGen(Long.getLong("testdata.seed", System.nanoTime()));
    }

    public String text(String prefix, int length) {
        StringBuilder sb = new StringBuilder(prefix);
        while (sb.length() < prefix.length() + length) sb.append((char) ('a' + random.nextInt(26)));
        return sb.toString();
    }

    public String email() { return text("user", 8) + "@example.test"; }

    public int intBetween(int min, int max) { return min + random.nextInt(max - min + 1); }

    /** Classic boundary set for a length-limited field: min-1, min, max, max+1. */
    public String[] boundaryStrings(int min, int max) {
        return new String[]{"x".repeat(Math.max(0, min - 1)), "x".repeat(min), "x".repeat(max), "x".repeat(max + 1)};
    }
}
