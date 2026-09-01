package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.ClockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 加速时钟配置 GUI（纯配置界面，176 宽，无物品栏）。
 * <p>
 * 顶部标题条下方：全局开关按钮 + 单独开关按钮（绿=开/红=关）；
 * 中间六个方向按钮（2×3，显示该方向实际相邻方块的物品图标，绿=生效/红=禁用，无相邻方块时显示方向名）；
 * 下方十个倍速档位按钮（快速选择 2~1024，当前档位高亮）。
 */
@OnlyIn(Dist.CLIENT)
public class ClockScreen extends AbstractContainerScreen<ClockMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/clock_gui.png");

    // 顶部两个开关按钮
    private static final int SWITCH_W = 78;
    private static final int SWITCH_H = 12;
    private static final int GLOBAL_BTN_X = 7;
    private static final int SELF_BTN_X = 91;
    private static final int SWITCH_BTN_Y = 18;
    // 六方向按钮（2 行 x 3 列）
    private static final int DIR_BTN_W = 48;
    private static final int DIR_BTN_H = 18;
    private static final int[] DIR_BTN_XS = {7, 64, 121};
    private static final int[] DIR_BTN_YS = {36, 58};
    // 速度档位按钮（2 行 x 5 列）
    private static final int SPEED_BTN_W = 30;
    private static final int SPEED_BTN_H = 14;
    private static final int[] SPEED_BTN_XS = {7, 40, 73, 106, 139};
    private static final int[] SPEED_BTN_YS = {82, 100};

    private final DirectionButton[] directionButtons = new DirectionButton[6];
    private final SpeedSelectButton[] speedButtons = new SpeedSelectButton[ClockEntity.SPEEDS.length];
    private SwitchButton globalButton;
    private SwitchButton selfButton;

    public ClockScreen(ClockMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 120;
    }

    @Override
    protected void init() {
        super.init();
        this.globalButton = new SwitchButton(this.leftPos + GLOBAL_BTN_X, this.topPos + SWITCH_BTN_Y, SWITCH_W, SWITCH_H,
                this.menu.isGlobalActive(), "global",
                button -> sendButton(ClockMenu.BUTTON_GLOBAL));
        this.addRenderableWidget(this.globalButton);
        this.selfButton = new SwitchButton(this.leftPos + SELF_BTN_X, this.topPos + SWITCH_BTN_Y, SWITCH_W, SWITCH_H,
                this.menu.isSelfEnabled(), "self",
                button -> sendButton(ClockMenu.BUTTON_SELF));
        this.addRenderableWidget(this.selfButton);
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.directionButtons[i] = new DirectionButton(this.leftPos + DIR_BTN_XS[i % 3], this.topPos + DIR_BTN_YS[i / 3],
                    direction, button -> sendButton(ClockMenu.BUTTON_DIR_BASE + direction.ordinal()));
            this.addRenderableWidget(this.directionButtons[i]);
        }
        for (int i = 0; i < ClockEntity.SPEEDS.length; i++) {
            final int index = i;
            this.speedButtons[i] = new SpeedSelectButton(this.leftPos + SPEED_BTN_XS[i % 5], this.topPos + SPEED_BTN_YS[i / 5],
                    i, button -> sendButton(ClockMenu.BUTTON_SPEED_BASE + index));
            this.addRenderableWidget(this.speedButtons[i]);
        }
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
        // 主背景（标题条、开关按钮区域、方向按钮区域、速度档位区域均已绘制在图上）
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 深色标题条上用白色文字
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 刷新开关与档位按钮状态
        this.globalButton.setState(this.menu.isGlobalActive());
        this.selfButton.setState(this.menu.isSelfEnabled());
        for (int i = 0; i < ClockEntity.SPEEDS.length; i++) {
            this.speedButtons[i].setSelected(this.menu.getSpeed() == ClockEntity.SPEEDS[i]);
        }
        // 渲染六方向按钮 tooltip（有相邻方块贴图时显示目标方块名称和方向）
        for (DirectionButton directionButton : this.directionButtons) {
            if (directionButton.isHovered()) {
                List<Component> lines = directionButton.buildTooltip();
                if (lines != null) {
                    guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
                }
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 带状态颜色的开关按钮（全局/单独开关）
     */
    private class SwitchButton extends SimpleButton {
        private boolean state;
        private final String key;

        SwitchButton(int x, int y, int width, int height, boolean initial, String key, OnPress onPress) {
            super(x, y, width, height, onPress);
            this.state = initial;
            this.key = key;
        }

        void setState(boolean state) {
            this.state = state;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
            String stateText = Component.translatable(this.state
                    ? "screen.alltheimbaium.clock.enabled"
                    : "screen.alltheimbaium.clock.disabled").getString();
            String label = Component.translatable("screen.alltheimbaium.clock." + this.key).getString() + ": " + stateText;
            guiGraphics.drawCenteredString(ClockScreen.this.font, label, this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }

    /**
     * 六方向按钮：该方向有相邻方块时只显示物品图标（居中）；无相邻方块时显示方向名（居中）。
     * 绿=生效/红=禁用
     */
    private class DirectionButton extends SimpleButton {
        private final Direction direction;

        DirectionButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, DIR_BTN_W, DIR_BTN_H, onPress);
            this.direction = direction;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            boolean on = ClockScreen.this.menu.isDirectionEnabled(this.direction);
            renderButton(guiGraphics, on ? 0xFF00AA00 : 0xFFAA0000);
            ItemStack neighbor = ClockScreen.this.menu.getNeighborStack(this.direction);
            if (!neighbor.isEmpty()) {
                // 有相邻方块：只显示物品图标，整体居中
                int iconX = this.getX() + (this.getWidth() - 16) / 2;
                int iconY = this.getY() + (this.getHeight() - 16) / 2;
                guiGraphics.renderItem(neighbor, iconX, iconY);
            } else {
                // 无相邻方块：显示方向名，居中
                String dirName = Component.translatable("screen.alltheimbaium.clock.face." + this.direction.getName()).getString();
                guiGraphics.drawCenteredString(ClockScreen.this.font, dirName, this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }

        /**
         * 构建 hover tooltip：按钮显示相邻方块贴图时返回 [目标方块名称/方向]，否则返回 null
         */
        @Nullable
        List<Component> buildTooltip() {
            if (ClockScreen.this.menu.getNeighborStack(this.direction).isEmpty()) {
                return null;
            }
            String dirName = Component.translatable("screen.alltheimbaium.clock.face." + this.direction.getName()).getString();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.alltheimbaium.clock.tooltip_target",
                    ClockScreen.this.menu.getNeighborName(this.direction)));
            lines.add(Component.translatable("screen.alltheimbaium.clock.tooltip_direction", dirName));
            return lines;
        }
    }

    /**
     * 速度档位快速选择按钮：点击直接设置该档位，当前档位绿色高亮
     */
    private class SpeedSelectButton extends SimpleButton {
        private final int index;
        private boolean selected;

        SpeedSelectButton(int x, int y, int index, OnPress onPress) {
            super(x, y, SPEED_BTN_W, SPEED_BTN_H, onPress);
            this.index = index;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            renderButton(guiGraphics, this.selected ? 0xFF00AA00 : 0xFF3A3A6B);
            guiGraphics.drawCenteredString(ClockScreen.this.font, String.valueOf(ClockEntity.SPEEDS[this.index]), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }

    /**
     * 带边框与居中文字的通用按钮
     */
    private abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            // 1px 边框（鼠标悬浮时边框变亮，用于指示可交互）
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
        }
    }
}
