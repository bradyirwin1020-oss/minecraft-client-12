package com.aegis.client.core;

public final class AegisClientSettings {
    private static final int[] THEMES = {0xFF66C7FF, 0xFFB28CFF, 0xFF78D7A3, 0xFFFFB36B, 0xFFFF7F9F};
    private static final String[] THEME_NAMES = {"Arctic", "Violet", "Mint", "Amber", "Rose"};
    public static int hudX = 8;
    public static int hudY = 8;
    public static int hudScale = 100;
    public static int hudOpacity = 73;
    public static int themeIndex = 0;

    private AegisClientSettings() {}

    public static int accentColor() { return THEMES[Math.floorMod(themeIndex, THEMES.length)]; }
    public static String themeName() { return THEME_NAMES[Math.floorMod(themeIndex, THEME_NAMES.length)]; }
    public static void nextTheme(int direction) { themeIndex = Math.floorMod(themeIndex + direction, THEMES.length); }
}
