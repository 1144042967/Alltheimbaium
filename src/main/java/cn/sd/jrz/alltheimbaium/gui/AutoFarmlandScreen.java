package cn.sd.jrz.alltheimbaium.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * ATI 自动耕地 GUI（原版普通箱子界面，27 槽）。
 */
@OnlyIn(Dist.CLIENT)
public class AutoFarmlandScreen extends AbstractContainerScreen<AutoFarmlandMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/auto_farmland_gui.png");
    // "下方输出"开关按钮（物品栏标签右侧靠右，绿=开 / 红=关）
    private static final int OUTPUT_BTN_W = 66;
    private static final int OUTPUT_BTN_H = 10;
    private static final int OUTPUT_BTN_X = 176 - OUTPUT_BTN_W - 8;
    private static final int OUTPUT_BTN_Y = 72;

    private StateButton outputDownButton;

    public AutoFarmlandScreen(AutoFarmlandMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        // 物品栏标签放在容器下方（与箱子界面一致）
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.outputDownButton = new StateButton(this.leftPos + OUTPUT_BTN_X, this.topPos + OUTPUT_BTN_Y, OUTPUT_BTN_W, OUTPUT_BTN_H,
                this.menu.isOutputToDown(),
                Component.translatable("screen.alltheimbaium.auto_farmland.output_down"),
                button -> sendButton(AutoFarmlandMenu.BUTTON_OUTPUT_DOWN));
        this.addRenderableWidget(this.outputDownButton);
    }

    /**
     * 发送容器按钮点击到服务端
     */
    private void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, id));
        }
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 176, 166);
        // 能量条（靠右、较窄避开机器名、加高包裹电量文字、带边框；能量值居中，白色带阴影）
        int barX = this.leftPos + 110;
        int barY = this.topPos + 5;
        int barW = 58;
        int barH = 10;
        // 边框
        guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        // 背景
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        int stored = this.menu.getEnergyStored();
        int capacity = this.menu.getEnergyCapacity();
        if (capacity > 0 && stored > 0) {
            int fill = barW * stored / capacity;
            guiGraphics.fill(barX, barY, barX + fill, barY + barH, 0xFFE03000);
        }
        // 能量值文字（居中，白色带阴影）
        String energyText = formatEnergy(stored) + " FE";
        guiGraphics.drawString(this.font, energyText, barX + (barW - this.font.width(energyText)) / 2, barY + (barH - 8) / 2, 0xFFFFFFFF, true);
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染鼠标悬浮物品的信息提示窗
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        // 刷新"下方输出"开关状态
        this.outputDownButton.setState(this.menu.isOutputToDown());
    }

    /**
     * 能量值缩写（整数 K/M，缩短文字以适配较窄能量条）：&lt;1000 用数字，&lt;1000000 用整数 K，否则用整数 M
     */
    private static String formatEnergy(int fe) {
        if (fe < 1000) {
            return String.valueOf(fe);
        }
        if (fe < 1_000_000) {
            return (fe + 500) / 1000 + "K";
        }
        return (fe + 500_000) / 1_000_000 + "M";
    }

    /**
     * 带状态颜色的开关按钮（开=绿色，关=红色），auto-resource 风格。
     * override renderWidget 以确保不使用 vanilla 默认按钮渲染。
     */
    private class StateButton extends SimpleButton {
        private boolean state;

        StateButton(int x, int y, int width, int height, boolean initial, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress);
            this.state = initial;
        }

        void setState(boolean state) {
            this.state = state;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
        }
    }

    /**
     * 带边框与居中文字的通用按钮（auto-resource 风格）
     */
    private abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            // 1px 边框（鼠标悬浮时边框变亮，用于指示可交互）
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.drawCenteredString(AutoFarmlandScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
