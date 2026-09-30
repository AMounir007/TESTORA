package com.testora.core.context;

/** Thread-isolated access to the current {@link TestContext}. */
public final class TestContextHolder {
    private static final ThreadLocal<TestContext> CURRENT = new ThreadLocal<>();

    private TestContextHolder() { }

    public static void set(TestContext ctx) { CURRENT.set(ctx); }

    public static TestContext current() {
        TestContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new IllegalStateException("No TestContext bound to this thread. "
                    + "Is the TestoraExtension registered?");
        }
        return ctx;
    }

    /** Returns the bound context or null (used by framework code that may run outside a test). */
    public static TestContext optional() { return CURRENT.get(); }

    public static void clear() { CURRENT.remove(); }
}
