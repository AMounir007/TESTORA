package com.testora.synchronization;

import org.openqa.selenium.WebElement;

import java.util.function.Predicate;

/** A named condition on an element; the name is used in wait diagnostics. */
public interface ElementCondition {
    String name();

    boolean test(WebElement element);

    static ElementCondition of(String name, Predicate<WebElement> predicate) {
        return new ElementCondition() {
            @Override public String name() { return name; }
            @Override public boolean test(WebElement e) { return predicate.test(e); }
        };
    }
}
