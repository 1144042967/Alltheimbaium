package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.PlatformBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 生成平台配置 GUI（纯代码绘制，无物品栏，宽度仅容纳 3×3 九宫格）。
 * <p>
 * 顶部：全局伪装开关按钮（绿=开/红=关，全局生效并保存，hover 有说明）；
 * 中部：3×3 九宫格，每格 = 一个以本平台所在区块为中心的区块，
 * 该区块四角都是生成平台即为"已生成"（绿），未生成为红；
 * 点击任意格向服务端请求生成/重建该区块；hover 显示该格方位、坐标与状态。
 * <p>
 * 中心格 = 当前（打开 GUI 的平台所在）区块：按打开的平台方块在区块内的相对位置，
 * 在格子内对应比例处绘制一小块白色标记，并在 tooltip 中描述。
 * 格内标注文字：中文环境用不超过两个汉字的方向词（西北/北/中…），其它语言用 NW/N/E 等缩写，白色无阴影绘制。
 */
@OnlyIn(Dist.CLIENT)
public class PlatformScreen extends AbstractContainerScreen<PlatformMenu> {
    /** GUI 背景贴图（占位图，可直接用 PS 修改替换） */
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/platform_gui.png");
    // 面板宽度（略宽，保证英文标题 ATI Generation Platform 可完整显示）
    private static final int IMAGE_WIDTH = 152;
    private static final int IMAGE_HEIGHT = 142;
    // 九宫格（居中布局）
    private static final int CELL = 26;
    private static final int GAP = 8;
    private static final int CELLS_W = CELL * 3 + GAP * 2;
    private static final int GRID_X0 = (IMAGE_WIDTH - CELLS_W) / 2;
    private static final int GRID_Y0 = 40;
    // 伪装开关（与九宫格左对齐、同宽）
    private static final int DISGUISE_X = GRID_X0;
    private static final int DISGUISE_Y = 18;
    private static final int DISGUISE_W = CELLS_W;
    private static final int DISGUISE_H = 14;
    private static final int[] CELL_XS = {GRID_X0, GRID_X0 + CELL + GAP, GRID_X0 + 2 * (CELL + GAP)};
    private static final int[] CELL_YS = {GRID_Y0, GRID_Y0 + CELL + GAP, GRID_Y0 + 2 * (CELL + GAP)};
    /** 中心格标记小块尺寸与边距 */
    private static final int MARK_SIZE = 5;
    private static final int MARK_PAD = 2;
    /** 中文（每格 ≤2 汉字）方位标注 */
    private static final String[] ZH_LABELS = {"西北", "北", "东北", "西", "中", "东", "西南", "南", "东南"};
    /** 非中文（两字符缩写）方位标注 */
    private static final String[] EN_LABELS = {"NW", "N", "NE", "W", "C", "E", "SW", "S", "SE"};

    private final CellButton[] cellButtons = new CellButton[9];
    private SwitchButton disguiseButton;

    public PlatformScreen(PlatformMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.disguiseButton = new SwitchButton(this.leftPos + DISGUISE_X, this.topPos + DISGUISE_Y, DISGUISE_W, DISGUISE_H,
                this.menu.isDisguiseActive(),
                Component.translatable("screen.alltheimbaium.platform.disguise"),
                button -> sendButton(PlatformMenu.BUTTON_DISGUISE));
        this.addRenderableWidget(this.disguiseButton);
        for (int i = 0; i < 9; i++) {
            final int index = i;
            this.cellButtons[i] = new CellButton(this.leftPos + CELL_XS[i % 3], this.topPos + CELL_YS[i / 3],
                    index, button -> sendButton(PlatformMenu.BUTTON_CELL_BASE + index));
            this.addRenderableWidget(this.cellButtons[i]);
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
        // 背景用贴图绘制（尺寸 = imageWidth × imageHeight），按钮等控件绘制在贴图之上
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题：深色面板上用白色文字
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 刷新开关与格子状态
        this.disguiseButton.setState(this.menu.isDisguiseActive());
        for (int i = 0; i < 9; i++) {
            this.cellButtons[i].setGenerated(isCellGenerated(i));
        }
        // 伪装开关 tooltip
        if (this.disguiseButton.isHovered()) {
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.translatable("screen.alltheimbaium.platform.disguise_tooltip")
            ), Optional.empty(), mouseX, mouseY);
        }
        // 九宫格 tooltip（方位 + 具体坐标 + 状态 + 点击动作；中心格另含标记说明）
        for (CellButton cellButton : this.cellButtons) {
            if (cellButton.isHovered()) {
                guiGraphics.renderTooltip(this.font, cellButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 判断某格对应区块（以 GUI 打开的生成平台所在区块为中心）是否已生成
     */
    private boolean isCellGenerated(int index) {
        if (this.minecraft == null || this.minecraft.level == null) {
            return false;
        }
        BlockPos pos = this.menu.anchorPos;
        int chunkX = (pos.getX() >> 4) + (index % 3 - 1);
        int chunkZ = (pos.getZ() >> 4) + (index / 3 - 1);
        return PlatformBlock.isChunkCellGenerated(this.minecraft.level, chunkX, chunkZ, pos.getY());
    }

    /**
     * 当前 GUI 语言是否为中文
     */
    private static boolean isChinese() {
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("zh");
    }

    /**
     * 当前语言下的方位标注文本
     */
    private static String cellLabel(int index) {
        return (isChinese() ? ZH_LABELS : EN_LABELS)[index];
    }

    /**
     * 带状态颜色的伪装开关按钮（开=绿，关=红）
     */
    private class SwitchButton extends Button {
        private boolean state;
        private final Component label;

        SwitchButton(int x, int y, int width, int height, boolean initial, Component label, OnPress onPress) {
            super(x, y, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
            this.state = initial;
            this.label = label;
        }

        void setState(boolean state) {
            this.state = state;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int color = this.state ? 0xFF00AA00 : 0xFFAA0000;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int border = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            fillBorder(guiGraphics, border);
            String stateText = Component.translatable(this.state
                    ? "screen.alltheimbaium.platform.enabled"
                    : "screen.alltheimbaium.platform.disabled").getString();
            guiGraphics.drawCenteredString(PlatformScreen.this.font,
                    this.label.getString() + ": " + stateText,
                    this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }

        private void fillBorder(GuiGraphics guiGraphics, int border) {
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), border);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, border);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), border);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), border);
        }
    }

    /**
     * 九宫格区块按钮：绿=已生成，红=未生成；点击请求生成/重建该区块。
     * 中心格额外按"打开 GUI 的平台方块在其区块内的位置"绘制一块小标记。
     */
    private class CellButton extends Button {
        private final int index;
        private boolean generated;

        CellButton(int x, int y, int index, OnPress onPress) {
            super(x, y, CELL, CELL, Component.literal(""), onPress, DEFAULT_NARRATION);
            this.index = index;
            this.generated = false;
        }

        void setGenerated(boolean generated) {
            this.generated = generated;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                    this.generated ? 0xFF00AA00 : 0xFFAA0000);
            // 中心格用白色边框标识本区块，其余用黑色；hover 时边框变黄
            int border;
            if (this.isHovered()) {
                border = 0xFFFFFF00;
            } else if (this.index == 4) {
                border = 0xFFFFFFFF;
            } else {
                border = 0xFF000000;
            }
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), border);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, border);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), border);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), border);
            // 方位标注：白色无阴影，横向/纵向均按按钮尺寸精确居中，避免黑字重影/偏移
            String text = cellLabel(this.index);
            int textX = this.getX() + (this.getWidth() - PlatformScreen.this.font.width(text)) / 2;
            int textY = this.getY() + (this.getHeight() - PlatformScreen.this.font.lineHeight) / 2;
            guiGraphics.drawString(PlatformScreen.this.font, text, textX, textY, 0xFFFFFFFF, false);
            // 中心格：绘制"本平台在区块内位置"的标记小块（盖在文字上，白色带黑边）
            if (this.index == 4) {
                drawCenterMarker(guiGraphics);
            }
        }

        /**
         * 中心格标记：按打开 GUI 的平台方块在 16×16 区块内的相对坐标，映射到格子内对应比例位置绘制白色小块。
         */
        private void drawCenterMarker(GuiGraphics guiGraphics) {
            BlockPos anchor = PlatformScreen.this.menu.anchorPos;
            int localX = anchor.getX() & 15;
            int localZ = anchor.getZ() & 15;
            int available = CELL - MARK_SIZE - MARK_PAD * 2;
            int mx = this.getX() + MARK_PAD + (int) Math.round(available * (localX / 15.0F));
            int my = this.getY() + MARK_PAD + (int) Math.round(available * (localZ / 15.0F));
            // 黑色 1px 描边
            guiGraphics.fill(mx - 1, my - 1, mx + MARK_SIZE + 1, my, 0xFF000000);
            guiGraphics.fill(mx - 1, my + MARK_SIZE, mx + MARK_SIZE + 1, my + MARK_SIZE + 1, 0xFF000000);
            guiGraphics.fill(mx - 1, my, mx, my + MARK_SIZE, 0xFF000000);
            guiGraphics.fill(mx + MARK_SIZE, my, mx + MARK_SIZE + 1, my + MARK_SIZE, 0xFF000000);
            // 白色填充
            guiGraphics.fill(mx, my, mx + MARK_SIZE, my + MARK_SIZE, 0xFFFFFFFF);
        }

        /**
         * hover tooltip：方位 + 具体坐标 + 生成状态 + 点击说明；中心格另含标记说明
         */
        List<Component> buildTooltip() {
            BlockPos anchor = PlatformScreen.this.menu.anchorPos;
            int chunkX = (anchor.getX() >> 4) + (this.index % 3 - 1);
            int chunkZ = (anchor.getZ() >> 4) + (this.index / 3 - 1);
            int centerX = chunkX * 16 + 8;
            int centerZ = chunkZ * 16 + 8;
            // tooltip 中的方位用完整名称（如 Northeast），不在 tooltip 里显示 NE 之类缩写
            Component name = this.index == 4
                    ? Component.translatable("screen.alltheimbaium.platform.current")
                    : Component.translatable("screen.alltheimbaium.platform.dir." + EN_LABELS[this.index]);
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.alltheimbaium.platform.cell_position",
                    name, centerX, anchor.getY(), centerZ));
            if (this.index == 4) {
                // 中心格：标记说明 = 打开 GUI 的平台在区块内的相对位置
                int localX = anchor.getX() & 15;
                int localZ = anchor.getZ() & 15;
                lines.add(Component.translatable("screen.alltheimbaium.platform.cell_marker", localX, localZ));
            }
            lines.add(Component.translatable(this.generated
                    ? "screen.alltheimbaium.platform.cell_generated"
                    : "screen.alltheimbaium.platform.cell_not_generated"));
            lines.add(Component.translatable("screen.alltheimbaium.platform.cell_hint"));
            return lines;
        }
    }
}
