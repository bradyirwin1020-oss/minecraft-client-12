package com.aegis.client.core;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ClickTracker {
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();

    private ClickTracker() {}

    public static synchronized void onMousePress(int button, int action) {
        if (action != GLFW.GLFW_PRESS) return;
        long now = System.nanoTime();
        if (button == GLFW.GLFW_MOUSE_BUTTON_1) LEFT.addLast(now);
        else if (button == GLFW.GLFW_MOUSE_BUTTON_2) RIGHT.addLast(now);
        prune(now);
    }

    public static synchronized int leftCps() { prune(System.nanoTime()); return LEFT.size(); }
    public static synchronized int rightCps() { prune(System.nanoTime()); return RIGHT.size(); }

    private static void prune(long now) {
        while (!LEFT.isEmpty() && now - LEFT.peekFirst() > WINDOW_NANOS) LEFT.removeFirst();
        while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > WINDOW_NANOS) RIGHT.removeFirst();
    }
}
