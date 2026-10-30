package com.aegis.client.gui;

import com.aegis.client.AegisClient;
import com.aegis.client.core.AegisClientSettings;
import com.aegis.client.core.ClientModule;
import com.aegis.client.core.ModuleCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** A custom client menu with independent module toggles and persisted HUD controls. */
public final class AegisMenuScreen extends Screen {
    private ModuleCategory selected = ModuleCategory.HUD;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int listX;
    private int listY;
    private final int rowHeight = 43;

    public AegisMenuScreen() { super(Component.literal("Aegis Client")); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0x99040A12);
        panelW = Math.min(690, this.width - 32);
        panelH = Math.min(470, this.height - 32);
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0111B2A);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF65C7FF);
        graphics.fill(panelX, panelY + 55, panelX + panelW, panelY + 56, 0xFF26384B);

        graphics.text(this.font, "AEGIS", panelX + 20, panelY + 16, AegisClientSettings.accentColor(), true);
        graphics.text(this.font, "CLIENT CONTROL CENTER", panelX + 91, panelY + 18, 0xFF98A9BC, true);
        graphics.text(this.font, "RIGHT SHIFT  •  CLOSE", panelX + panelW - 160, panelY + 18, 0xFF7C8EA3, true);

        int sideX = panelX + 14;
        int sideY = panelY + 72;
        int sideW = 140;
        int tabHeight = 34;
        for (ModuleCategory category : ModuleCategory.values()) {
            boolean active = category == selected;
            int color = active ? 0xFF203E58 : 0xFF151F2D;
            graphics.fill(sideX, sideY, sideX + sideW, sideY + tabHeight - 3, color);
            if (active) graphics.fill(sideX, sideY, sideX + 3, sideY + tabHeight - 3, 0xFF71CEFF);
            graphics.text(this.font, category.title(), sideX + 12, sideY + 10, active ? 0xFFFFFFFF : 0xFFB0BDCA, true);
            sideY += tabHeight;
        }

        listX = panelX + 174;
        listY = panelY + 73;
        graphics.text(this.font, selected.title().toUpperCase(), listX, listY - 17, AegisClientSettings.accentColor(), true);
        if (selected == ModuleCategory.VISUALS) {
            graphics.fill(listX, listY, panelX + panelW - 20, listY + 60, 0xFF182433);
            graphics.text(this.font, "Accent theme: " + AegisClientSettings.themeName(), listX + 12, listY + 13, 0xFFF0F5FA, true);
            graphics.text(this.font, "Palette controls", listX + 12, listY + 32, 0xFF91A2B5, false);
            graphics.text(this.font, "PREV", panelX + panelW - 136, listY + 13, AegisClientSettings.accentColor(), true);
            graphics.text(this.font, "NEXT", panelX + panelW - 79, listY + 13, AegisClientSettings.accentColor(), true);
            graphics.fill(listX, listY + 70, panelX + panelW - 20, listY + 130, 0xFF182433);
            graphics.text(this.font, "HUD panel opacity: " + AegisClientSettings.hudOpacity + "%", listX + 12, listY + 83, 0xFFF0F5FA, true);
            graphics.text(this.font, "−", panelX + panelW - 116, listY + 83, 0xFFFFFFFF, true);
            graphics.text(this.font, "+", panelX + panelW - 78, listY + 83, 0xFFFFFFFF, true);
            graphics.text(this.font, "Controls affect the HUD background only.", listX + 12, listY + 102, 0xFF91A2B5, false);
        }
        List<ClientModule> modules = AegisClient.MODULES.inCategory(selected);
        int rowY = listY;
        for (ClientModule module : modules) {
            int rowW = panelX + panelW - 20 - listX;
            boolean hovered = mouseX >= listX && mouseX <= listX + rowW && mouseY >= rowY && mouseY < rowY + rowHeight - 5;
            graphics.fill(listX, rowY, listX + rowW, rowY + rowHeight - 5, hovered ? 0xFF1F3042 : 0xFF182433);
            graphics.text(this.font, module.name(), listX + 11, rowY + 7, 0xFFF0F5FA, true);
            graphics.text(this.font, trim(module.description(), 64), listX + 11, rowY + 22, 0xFF91A2B5, false);
            int switchX = listX + rowW - 50;
            int switchColor = module.enabled() ? 0xFF2C9BC6 : 0xFF455466;
            graphics.fill(switchX, rowY + 11, switchX + 38, rowY + 28, switchColor);
            graphics.fill(module.enabled() ? switchX + 22 : switchX + 2, rowY + 13,
                    module.enabled() ? switchX + 36 : switchX + 16, rowY + 26, 0xFFFFFFFF);
            rowY += rowHeight;
        }

        int footerY = panelY + panelH - 57;
        graphics.fill(panelX + 15, footerY, panelX + panelW - 15, footerY + 1, 0xFF26384B);
        graphics.text(this.font, "HUD X", panelX + 20, footerY + 13, 0xFF9EADBC, true);
        graphics.text(this.font, "−", panelX + 74, footerY + 12, 0xFFFFFFFF, true);
        graphics.text(this.font, "+", panelX + 98, footerY + 12, 0xFFFFFFFF, true);
        graphics.text(this.font, "Y", panelX + 128, footerY + 13, 0xFF9EADBC, true);
        graphics.text(this.font, "−", panelX + 146, footerY + 12, 0xFFFFFFFF, true);
        graphics.text(this.font, "+", panelX + 170, footerY + 12, 0xFFFFFFFF, true);
        graphics.text(this.font, "SIZE", panelX + 202, footerY + 13, 0xFF9EADBC, true);
        graphics.text(this.font, "−", panelX + 238, footerY + 12, 0xFFFFFFFF, true);
        graphics.text(this.font, "+", panelX + 262, footerY + 12, 0xFFFFFFFF, true);
        graphics.text(this.font, "X " + AegisClientSettings.hudX + " Y " + AegisClientSettings.hudY + "  " + AegisClientSettings.hudScale + "%", panelX + 292, footerY + 13, AegisClientSettings.accentColor(), true);
        graphics.text(this.font, "Settings save automatically", panelX + 20, panelY + panelH - 24, 0xFF708297, false);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int sideX = panelX + 14;
        int sideY = panelY + 72;
        for (ModuleCategory category : ModuleCategory.values()) {
            if (mouseX >= sideX && mouseX <= sideX + 140 && mouseY >= sideY && mouseY <= sideY + 31) {
                selected = category;
                return true;
            }
            sideY += 34;
        }

        if (selected == ModuleCategory.VISUALS) {
            if (mouseY >= listY && mouseY <= listY + 60) {
                if (mouseX >= panelX + panelW - 145 && mouseX <= panelX + panelW - 88) AegisClientSettings.nextTheme(-1);
                else if (mouseX >= panelX + panelW - 88 && mouseX <= panelX + panelW - 20) AegisClientSettings.nextTheme(1);
                AegisClient.saveConfig();
                return true;
            }
            if (mouseY >= listY + 70 && mouseY <= listY + 130) {
                if (mouseX >= panelX + panelW - 126 && mouseX <= panelX + panelW - 93) AegisClientSettings.hudOpacity = Math.max(35, AegisClientSettings.hudOpacity - 5);
                else if (mouseX >= panelX + panelW - 89 && mouseX <= panelX + panelW - 50) AegisClientSettings.hudOpacity = Math.min(100, AegisClientSettings.hudOpacity + 5);
                AegisClient.saveConfig();
                return true;
            }
        }

        List<ClientModule> modules = AegisClient.MODULES.inCategory(selected);
        int rowY = listY;
        int rowW = panelX + panelW - 20 - listX;
        for (ClientModule module : modules) {
            if (mouseX >= listX && mouseX <= listX + rowW && mouseY >= rowY && mouseY < rowY + rowHeight - 5) {
                module.toggle();
                AegisClient.saveConfig();
                return true;
            }
            rowY += rowHeight;
        }

        int footerY = panelY + panelH - 57;
        if (mouseY >= footerY && mouseY <= footerY + 32) {
            if (mouseX >= panelX + 68 && mouseX <= panelX + 93) AegisClientSettings.hudX = Math.max(0, AegisClientSettings.hudX - 4);
            else if (mouseX >= panelX + 94 && mouseX <= panelX + 119) AegisClientSettings.hudX += 4;
            else if (mouseX >= panelX + 140 && mouseX <= panelX + 165) AegisClientSettings.hudY = Math.max(0, AegisClientSettings.hudY - 4);
            else if (mouseX >= panelX + 166 && mouseX <= panelX + 191) AegisClientSettings.hudY += 4;
            else if (mouseX >= panelX + 232 && mouseX <= panelX + 257) AegisClientSettings.hudScale = Math.max(70, AegisClientSettings.hudScale - 5);
            else if (mouseX >= panelX + 258 && mouseX <= panelX + 283) AegisClientSettings.hudScale = Math.min(150, AegisClientSettings.hudScale + 5);
            AegisClient.saveConfig();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
