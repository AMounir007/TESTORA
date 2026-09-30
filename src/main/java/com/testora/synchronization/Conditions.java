package com.testora.synchronization;

/** Reusable, composable element conditions. */
public final class Conditions {
    public static final ElementCondition VISIBLE = ElementCondition.of("Visible", e -> e.isDisplayed());
    public static final ElementCondition ENABLED = ElementCondition.of("Enabled", e -> e.isEnabled());
    public static final ElementCondition CLICKABLE =
            ElementCondition.of("Clickable", e -> e.isDisplayed() && e.isEnabled());

    private Conditions() { }

    public static ElementCondition text(String expected) {
        return ElementCondition.of("Text=" + expected, e -> expected.equals(e.getText().trim()));
    }

    public static ElementCondition textContains(String fragment) {
        return ElementCondition.of("TextContains=" + fragment, e -> e.getText().contains(fragment));
    }

    public static ElementCondition attribute(String attribute, String value) {
        return ElementCondition.of("Attribute " + attribute + "=" + value,
                e -> value.equals(e.getAttribute(attribute)));
    }

    public static ElementCondition and(ElementCondition a, ElementCondition b) {
        return ElementCondition.of(a.name() + "&" + b.name(), e -> a.test(e) && b.test(e));
    }
}
