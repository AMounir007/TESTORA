package com.testora.web.components;

import com.testora.web.elements.SmartElement;
import org.openqa.selenium.By;

/** Reusable UI widget scoped by a root locator (e.g. a table, modal, navigation bar). */
public abstract class Component {
    private final By root;

    protected Component(By root) { this.root = root; }

    protected SmartElement child(String css, String name) {
        return SmartElement.web(By.cssSelector(rootCss() + " " + css), name);
    }

    private String rootCss() {
        String s = root.toString();
        int i = s.indexOf(": ");
        return i >= 0 ? s.substring(i + 2) : s;
    }
}
