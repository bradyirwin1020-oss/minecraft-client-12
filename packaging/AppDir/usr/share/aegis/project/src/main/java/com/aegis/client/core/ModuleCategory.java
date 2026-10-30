package com.aegis.client.core;

public enum ModuleCategory {
    HUD("HUD"),
    COMBAT_INFO("PvP Info"),
    VISUALS("Visuals"),
    MOVEMENT("Movement"),
    DEFENSE("Defense"),
    PERFORMANCE("Performance");

    private final String title;
    ModuleCategory(String title) { this.title = title; }
    public String title() { return title; }
}
