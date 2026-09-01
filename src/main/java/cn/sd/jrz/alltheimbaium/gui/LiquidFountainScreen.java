package cn.sd.jrz.alltheimbaium.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 液体无限制造机 GUI（新材质 GUI，176 宽亮色主题）。
 * <p>
 * 背景图已绘制：左上 + 槽（输入）与 - 槽（输出）、右侧信息面板、玩家物品栏槽位；
 * 代码动态绘制：面板顶部进度条与百分比、面板中部信息描述（mB/B/KB 单位缩写）、
 * 面板下方六个主动输出开关按钮（auto-resource 风格：绿=开 / 红=关，随开关状态着色）。
 */
@OnlyIn(Dist.CLIENT)
public class LiquidFountainScreen extends AbstractContainerScreen<LiquidFountainMenu> {
    private static final ResourceLocation TEXTURE_BASE = new ResourceLocation("alltheimbaium", "textures/gui/liquid_fountain_gui.png");
    private static final int TEXT_COLOR = 0xC6C6C6;

    // 右侧面板/进度条/信息（面板 x=39~168, y=17~56）
    private static final int RIGHT_X = 39;
    private static final int PROGRESS_X = RIGHT_X + 4;
    private static final int PROGRESS_Y = 22;
    private static final int PROGRESS_W = 100;
    /**
     * 最低进度效果：只要有液体就至少显示该百分比，让用户一眼看出有液体
     */
    private static final int MIN_PERCENT = 2;
    private static final int INFO_Y = 32;
    private static final int INFO_LINE = 12;
    // 六个面开关按钮（面板下方 3 列 x 2 行）
    private static final int BTN_W = 38;
    private static final int BTN_H = 13;
    private static final int BTN_X1 = 43;
    private static final int BTN_X2 = 85;
    private static final int BTN_X3 = 127;
    private static final int BTN_Y1 = 62;
    private static final int BTN_Y2 = 78;
    // "输出"总开关按钮（物品栏标签右侧靠右，绿=开 / 红=关）
    private static final int OUTPUT_BTN_W = 38;
    private static final int OUTPUT_BTN_H = 13;
    private static final int OUTPUT_BTN_X = 176 - OUTPUT_BTN_W - 11;
    private static final int OUTPUT_BTN_Y = 94;

    // 六个传输面开关按钮，索引与 Direction.values() 顺序一致
    private final FaceButton[] faceButtons = new FaceButton[6];
    private StateButton outputButton;
    // 各面按钮位置（3 列 x 2 行），顺序与 Direction.values() 一致：下/上/北/南/西/东
    private static final int[] BTN_XS = {BTN_X1, BTN_X2, BTN_X3, BTN_X1, BTN_X2, BTN_X3};
    private static final int[] BTN_YS = {BTN_Y1, BTN_Y1, BTN_Y1, BTN_Y2, BTN_Y2, BTN_Y2};

    public LiquidFountainScreen(LiquidFountainMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 192;
        // 物品栏标签放在容器下方（与箱子界面一致）
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 面板下方六个传输面开关（3 列 x 2 行，宽 38 以容纳英文面名）
        // Direction.values() 顺序与 Menu.BUTTON_TRANSFER_* 一致，直接用作按钮 id
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.faceButtons[i] = new FaceButton(this.leftPos + BTN_XS[i], this.topPos + BTN_YS[i], direction,
                    button -> sendButton(direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        // 主动输出总开关（物品栏标签右侧靠右）
        this.outputButton = new StateButton(this.leftPos + OUTPUT_BTN_X, this.topPos + OUTPUT_BTN_Y, OUTPUT_BTN_W, OUTPUT_BTN_H,
                this.menu.isOutputEnabled(),
                Component.translatable("screen.alltheimbaium.liquid_fountain.output"),
                button -> sendButton(LiquidFountainMenu.BUTTON_OUTPUT));
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
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        // 主背景（槽位框、+/- 图标、信息面板、物品栏槽位均已绘制在图上）
        guiGraphics.blit(TEXTURE_BASE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 面板顶部进度条（阈值 / 1 万桶进度），右侧留白显示百分比
        int trackLeft = this.leftPos + PROGRESS_X;
        int trackTop = this.topPos + PROGRESS_Y;
        guiGraphics.fill(trackLeft, trackTop, trackLeft + PROGRESS_W, trackTop + 4, 0xFF555555);
        int percent = progressPercent();
        if (percent > 0) {
            int fill = PROGRESS_W * percent / 100;
            guiGraphics.fill(trackLeft, trackTop, trackLeft + fill, trackTop + 4, progressColor());
        }
        // 百分比文字：显示真实百分比，固定 3 位宽右对齐（不足前补空格），如 "  0%"
        guiGraphics.drawString(this.font, String.format("%3d%%", realPercent()), trackLeft + PROGRESS_W + 5, trackTop, TEXT_COLOR, false);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题与物品栏标签（亮色 GUI 上用白色/灰色文字）
        // 标题与物品栏标签：GUI 亮色背景（浅灰 0xC6C6C6），需用深色文字才能可见
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        // 面板中部信息描述两行（单位缩写：mB / B / KB）；renderLabels 使用相对 GUI 的局部坐标
        boolean infinite = this.menu.isInfinity();
        Component fluidName = fluidName();
        if (infinite) {
            guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.liquid_fountain.infinite", fluidName), RIGHT_X + 4, INFO_Y, TEXT_COLOR, false);
            guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.liquid_fountain.infinite_reached"), RIGHT_X + 4, INFO_Y + INFO_LINE, TEXT_COLOR, false);
        } else {
            long max = this.menu.getMax();
            long amount = Math.min(this.menu.getAmount(), max);
            guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.liquid_fountain.amount", formatVolume(amount), formatVolume(max), fluidName), RIGHT_X + 4, INFO_Y, TEXT_COLOR, false);
            guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.liquid_fountain.need", formatVolume(Math.max(0, max - amount))), RIGHT_X + 4, INFO_Y + INFO_LINE, TEXT_COLOR, false);
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染六面按钮 tooltip（按钮显示相邻方块贴图时提示输出目的/输出方向）
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton.isHovered()) {
                List<Component> lines = faceButton.buildTooltip();
                if (lines != null) {
                    guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
                }
            }
        }
        // 渲染鼠标悬浮物品的信息提示窗
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        // 刷新各开关状态
        for (int i = 0; i < 6; i++) {
            this.faceButtons[i].setState(this.menu.isFaceEnabled(Direction.values()[i]));
        }
        // 刷新主动输出总开关：只有达到无限后才允许开启（未无限时按钮禁用并强制显示关闭）
        boolean infinite = this.menu.isInfinity();
        this.outputButton.active = infinite;
        this.outputButton.setState(infinite && this.menu.isOutputEnabled());
    }

    /**
     * 当前流体显示名
     */
    private Component fluidName() {
        Fluid fluid = this.menu.getFluid();
        if (fluid == null || fluid == Fluids.EMPTY) {
            return Component.translatable("screen.alltheimbaium.liquid_fountain.none");
        }
        return new FluidStack(fluid, 1).getDisplayName();
    }

    /**
     * 真实进度百分比（0-100，不含最低效果），无限时固定 100%
     */
    private int realPercent() {
        long max = this.menu.getMax();
        if (max <= 0) {
            return 0;
        }
        long amount = this.menu.getAmount();
        if (amount >= max) {
            return 100;
        }
        return (int) Math.max(0, Math.min(100, amount * 100 / max));
    }

    /**
     * 进度条填充百分比：真实进度 + 最低效果（有液体时至少显示最低进度，避免空条）
     */
    private int progressPercent() {
        long amount = this.menu.getAmount();
        if (amount <= 0) {
            return 0;
        }
        return Math.max(MIN_PERCENT, realPercent());
    }

    /**
     * 进度条填充色：根据本机流体返回对应颜色（水=蓝、岩浆=橙），其余默认绿色
     */
    private int progressColor() {
        Fluid fluid = this.menu.getFluid();
        if (fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER) {
            return 0xFF3F76E4;
        }
        if (fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA) {
            return 0xFFFF8800;
        }
        return 0xFF00AA00;
    }

    /**
     * 体积单位缩写：&lt;1000 mB 用 mB；&lt;1000 B（1,000,000 mB）用 B（桶）；更大用 KB（千桶）。
     * 例如 10,000,000 mB → "10 KB"。
     */
    private static String formatVolume(long mb) {
        if (mb < 1000) {
            return mb + " mB";
        }
        double bucket = mb / 1000.0;
        if (mb < 1_000_000) {
            return trim(bucket) + " B";
        }
        return trim(bucket / 1000.0) + " KB";
    }

    /**
     * 整数去掉小数尾，否则保留一位小数
     */
    private static String trim(double value) {
        long whole = (long) value;
        if (value == whole) {
            return String.valueOf(whole);
        }
        return String.format("%.1f", value);
    }

    /**
     * 获取指定方向相邻方块的物品图标（无方块或方块无对应物品时返回空）。
     * 用于六面按钮显示"输出目的"贴图。
     */
    private ItemStack getNeighborIcon(Direction direction) {
        Item item = getNeighborState(direction).getBlock().asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * 获取指定方向相邻方块的显示名（无方块时返回方块自身名，仅供有贴图时使用）
     */
    private Component getNeighborName(Direction direction) {
        return getNeighborState(direction).getBlock().getName();
    }

    /**
     * 获取指定方向相邻方块状态（客户端世界不可用/无机器时返回空气）
     */
    private BlockState getNeighborState(Direction direction) {
        if (this.minecraft != null && this.minecraft.level != null && this.menu.entity != null) {
            return this.minecraft.level.getBlockState(this.menu.entity.getBlockPos().relative(direction));
        }
        return Blocks.AIR.defaultBlockState();
    }

    /**
     * 六面主动输出开关按钮：显示为 [相邻方向方块贴图 + 方向名]，无相邻方块时仅方向名居中。
     * 有贴图时 hover 显示 tooltip（输出目的/输出方向）。
     */
    private class FaceButton extends SimpleButton {
        private final Direction direction;
        private boolean state;

        FaceButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, BTN_W, BTN_H, Component.literal(""), onPress);
            this.direction = direction;
        }

        void setState(boolean state) {
            this.state = state;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (!this.active) {
                renderButton(guiGraphics, 0xFF666666);
                return;
            }
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
            ItemStack neighborIcon = LiquidFountainScreen.this.getNeighborIcon(this.direction);
            String dirName = Component.translatable("screen.alltheimbaium.liquid_fountain.face." + this.direction.getName()).getString();
            if (!neighborIcon.isEmpty()) {
                // 有贴图：缩放到按钮内部（高 13 的按钮放 12px 贴图）并居中，不显示方向文字
                int scaled = 13;
                float scale = scaled / 16.0F;
                int x = this.getX() + (this.getWidth() - scaled) / 2;
                int y = this.getY() + (this.getHeight() - scaled) / 2;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(x, y, 0);
                guiGraphics.pose().scale(scale, scale, 1.0F);
                guiGraphics.renderItem(neighborIcon, 0, 0);
                guiGraphics.pose().popPose();
            } else {
                // 无贴图：方向名居中
                guiGraphics.drawCenteredString(LiquidFountainScreen.this.font, dirName, this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }

        /**
         * 构建 hover tooltip：按钮显示相邻方块贴图时返回 [输出目的/输出方向]，否则返回 null
         */
        @Nullable
        List<Component> buildTooltip() {
            ItemStack neighborIcon = LiquidFountainScreen.this.getNeighborIcon(this.direction);
            if (neighborIcon.isEmpty()) {
                return null;
            }
            String dirName = Component.translatable("screen.alltheimbaium.liquid_fountain.face." + this.direction.getName()).getString();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.alltheimbaium.liquid_fountain.tooltip_target",
                    LiquidFountainScreen.this.getNeighborName(this.direction)));
            lines.add(Component.translatable("screen.alltheimbaium.liquid_fountain.tooltip_direction", dirName));
            return lines;
        }
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
            // 禁用时灰色显示且不可点击（vanilla Button 在 active=false 时不响应点击）
            if (!this.active) {
                renderButton(guiGraphics, 0xFF666666);
                return;
            }
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
            guiGraphics.drawCenteredString(LiquidFountainScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
