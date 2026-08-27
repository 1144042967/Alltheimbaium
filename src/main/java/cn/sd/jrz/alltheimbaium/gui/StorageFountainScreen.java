package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

/**
 * 存储方块制造机 GUI（新材质 GUI，176 宽）。
 * <p>
 * 黑色背景区域：第一行增长进度条，第二行增长百分比，第三行下次增长数值，第四行产量；
 * 右下角为标记槽（放入物品标记/取消标记）。下方为 9 个已标记物品槽（单击提取 1 个、shift 提取 1 组、空格提取到背包满，
 * 槽位左下角以 AE2 风格缩写显示数量）。再下方为六面输出状态按钮（随机/禁用/槽1~槽9，槽位状态显示对应物品图标），
 * 左下物品栏标签 + 右侧"输出"总开关按钮，最下方为玩家物品栏。
 */
@OnlyIn(Dist.CLIENT)
public class StorageFountainScreen extends AbstractContainerScreen<StorageFountainMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("alltheimbaium", "textures/gui/storage_fountain_gui.png");
    /** 黑色信息面板上的浅色文字 */
    private static final int TEXT_COLOR = 0xC6C6C6;

    // 增长进度条（黑色背景区域第一行）
    private static final int PROGRESS_X = 10;
    private static final int PROGRESS_Y = 21;
    private static final int PROGRESS_W = 152;
    private static final int PROGRESS_H = 4;
    // 信息文字行（第二/三/四行）
    private static final int INFO_X = 10;
    private static final int INFO_Y = 30;
    private static final int INFO_LINE = 10;

    // 六面状态按钮（2 行 x 3 列）
    private static final int BTN_W = 48;
    private static final int BTN_H = 16;
    private static final int[] BTN_XS = {14, 66, 118};
    private static final int[] BTN_YS = {96, 114};
    // "输出"总开关按钮（左下物品栏标签右侧靠右）
    private static final int OUTPUT_BTN_W = 44;
    private static final int OUTPUT_BTN_H = 13;
    private static final int OUTPUT_BTN_X = 176 - OUTPUT_BTN_W - 11;
    private static final int OUTPUT_BTN_Y = 134;

    // 已标记物品槽左下角数量文字偏移
    private static final int COUNT_X = 1;
    private static final int COUNT_Y = 8;

    private final FaceButton[] faceButtons = new FaceButton[6];
    private StateButton outputButton;
    /** 空格键是否按下（空格+单击物品槽 = 提取到背包满） */
    private boolean spaceDown = false;

    public StorageFountainScreen(StorageFountainMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 233;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 六个面状态按钮，按钮 id 与 Direction.values() 顺序一致（0~5）
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.faceButtons[i] = new FaceButton(this.leftPos + BTN_XS[i % 3], this.topPos + BTN_YS[i / 3], direction,
                    button -> sendButton(StorageFountainMenu.BUTTON_DIR_BASE + direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        // "输出"总开关按钮
        this.outputButton = new StateButton(this.leftPos + OUTPUT_BTN_X, this.topPos + OUTPUT_BTN_Y, OUTPUT_BTN_W, OUTPUT_BTN_H,
                this.menu.isOutputEnabled(),
                Component.translatable("screen.alltheimbaium.storage_fountain.output"),
                button -> sendButton(StorageFountainMenu.BUTTON_OUTPUT));
        this.addRenderableWidget(this.outputButton);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            this.spaceDown = true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            this.spaceDown = false;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /**
     * 拦截已标记物品槽的点击：单击提取一个、Shift+单击提取一组、空格+单击提取到背包满
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < 9; i++) {
                Slot slot = this.menu.slots.get(1 + i);
                if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    int id;
                    if (hasShiftDown()) {
                        id = StorageFountainMenu.BUTTON_EXTRACT_STACK_BASE + i;
                    } else if (this.spaceDown) {
                        id = StorageFountainMenu.BUTTON_EXTRACT_ALL_BASE + i;
                    } else {
                        id = StorageFountainMenu.BUTTON_EXTRACT_ONE_BASE + i;
                    }
                    sendButton(id);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        // 主背景（槽位框、黑色信息面板、标记槽、物品栏槽位均已绘制在图上）
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 第一行：增长进度条
        int trackLeft = this.leftPos + PROGRESS_X;
        int trackTop = this.topPos + PROGRESS_Y;
        guiGraphics.fill(trackLeft, trackTop, trackLeft + PROGRESS_W, trackTop + PROGRESS_H, 0xFF555555);
        int percent = growthPercent();
        if (percent > 0) {
            int fill = PROGRESS_W * percent / 100;
            guiGraphics.fill(trackLeft, trackTop, trackLeft + fill, trackTop + PROGRESS_H, 0xFF00AA00);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题与物品栏标签：亮色背景上用深色文字
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        // 黑色信息面板上的四行信息（局部坐标）
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.storage_fountain.growth", growthPercent()), INFO_X, INFO_Y, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.storage_fountain.next", this.menu.getStep()), INFO_X, INFO_Y + INFO_LINE, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.storage_fountain.output_rate", formatRate()), INFO_X, INFO_Y + INFO_LINE * 2, TEXT_COLOR, false);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 已标记物品槽左下角数量（AE2 风格缩写，绘制在物品图标之上）
        drawSlotCounts(guiGraphics);
        // 刷新开关状态
        this.outputButton.setState(this.menu.isOutputEnabled());
        // 渲染鼠标悬浮物品的信息提示窗
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 绘制 9 个已标记物品槽左下角的存量数量（AE2 风格缩写，如 1.1K、2.1M）
     */
    private void drawSlotCounts(GuiGraphics guiGraphics) {
        for (int i = 0; i < 9; i++) {
            Slot slot = this.menu.slots.get(1 + i);
            long units = this.menu.getMarkedCount(i);
            if (units <= 0) {
                continue;
            }
            long items = units / StorageFountainBlock.getCarry();
            if (items <= 0) {
                continue;
            }
            guiGraphics.drawString(this.font, formatCount(items), this.leftPos + slot.x + COUNT_X, this.topPos + slot.y + COUNT_Y, 0xFFFFFF, true);
        }
    }

    /**
     * 计算增长进度百分比（0-100）
     */
    private int growthPercent() {
        long second = Math.max(1, this.menu.getGrowthIntervalSeconds());
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
    }

    /**
     * 产量（每个已标记物品每秒产出的物品数），大数值用单位缩写
     */
    private String formatRate() {
        long output = this.menu.getOutput();
        double itemsPerSecond = output * 20.0 / StorageFountainBlock.getCarry();
        if (itemsPerSecond < 1000) {
            return String.format("%.2f", itemsPerSecond);
        }
        if (itemsPerSecond < 1_000_000) {
            return String.format("%.1fK", itemsPerSecond / 1000.0);
        }
        if (itemsPerSecond < 1_000_000_000) {
            return String.format("%.1fM", itemsPerSecond / 1_000_000.0);
        }
        return String.format("%.1fG", itemsPerSecond / 1_000_000_000.0);
    }

    /**
     * 存量数量单位缩写：参考 AE2，1.1K、2.1M 等
     */
    private static String formatCount(long value) {
        if (value < 1000) {
            return String.valueOf(value);
        }
        if (value < 1_000_000) {
            return String.format("%.1fK", value / 1000.0);
        }
        if (value < 1_000_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        }
        if (value < 1_000_000_000_000L) {
            return String.format("%.1fG", value / 1_000_000_000.0);
        }
        if (value < 1_000_000_000_000_000L) {
            return String.format("%.1fT", value / 1_000_000_000_000.0);
        }
        if (value < 1_000_000_000_000_000_000L) {
            return String.format("%.1fP", value / 1_000_000_000_000_000.0);
        }
        return String.format("%.1fE", value / 1_000_000_000_000_000_000.0);
    }

    /**
     * 六面输出状态按钮：槽位状态显示对应物品图标 + 槽号；随机/禁用显示文字；点击循环切换 11 个状态。
     */
    private class FaceButton extends SimpleButton {
        private final Direction direction;

        FaceButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, BTN_W, BTN_H, Component.literal(""), onPress);
            this.direction = direction;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int state = StorageFountainScreen.this.menu.getDirectionState(this.direction);
            String dirName = Component.translatable("screen.alltheimbaium.storage_fountain.face." + this.direction.getName()).getString();
            // 背景色：禁用=红，随机=绿，槽位=蓝灰
            int color;
            if (state == StorageFountainEntity.STATE_DISABLED) {
                color = 0xFFAA0000;
            } else if (state == StorageFountainEntity.STATE_RANDOM) {
                color = 0xFF00AA00;
            } else {
                color = 0xFF3A3A6B;
            }
            renderButton(guiGraphics, color);
            if (state >= StorageFountainEntity.STATE_SLOT_BASE) {
                // 槽位状态：绘制对应物品图标 + 槽号
                int slot = state - StorageFountainEntity.STATE_SLOT_BASE;
                ItemStack icon = StorageFountainScreen.this.menu.getMarkedStack(slot);
                if (!icon.isEmpty()) {
                    guiGraphics.renderItem(icon, this.getX() + 2, this.getY() + 1);
                }
                guiGraphics.drawString(StorageFountainScreen.this.font, dirName + "·" + (slot + 1), this.getX() + 20, this.getY() + 4, 0xFFFFFFFF, true);
            } else {
                String stateText = state == StorageFountainEntity.STATE_RANDOM
                        ? Component.translatable("screen.alltheimbaium.storage_fountain.random").getString()
                        : Component.translatable("screen.alltheimbaium.storage_fountain.disabled").getString();
                guiGraphics.drawCenteredString(StorageFountainScreen.this.font, dirName + "·" + stateText, this.getX() + BTN_W / 2, this.getY() + 4, 0xFFFFFFFF);
            }
        }
    }

    /**
     * 带状态颜色的开关按钮（开=绿色，关=红色）
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
     * 带边框与居中文字的通用按钮
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
            guiGraphics.drawCenteredString(StorageFountainScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
