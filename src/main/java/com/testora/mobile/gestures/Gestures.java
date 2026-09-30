package com.testora.mobile.gestures;

import com.testora.web.driver.DriverManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.List;

/** W3C touch gestures (works on Android and iOS). */
public final class Gestures {
    private Gestures() { }

    public static void swipeUp() { swipe(0.5, 0.75, 0.5, 0.25); }

    public static void swipeDown() { swipe(0.5, 0.25, 0.5, 0.75); }

    public static void swipeLeft() { swipe(0.8, 0.5, 0.2, 0.5); }

    public static void swipeRight() { swipe(0.2, 0.5, 0.8, 0.5); }

    /** Coordinates are fractions (0..1) of the screen size so gestures are device independent. */
    public static void swipe(double fromX, double fromY, double toX, double toY) {
        WebDriver driver = DriverManager.mobile();
        Dimension size = driver.manage().window().getSize();
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(),
                (int) (size.width * fromX), (int) (size.height * fromY)));
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(),
                (int) (size.width * toX), (int) (size.height * toY)));
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        ((Interactive) driver).perform(List.of(seq));
    }
}
