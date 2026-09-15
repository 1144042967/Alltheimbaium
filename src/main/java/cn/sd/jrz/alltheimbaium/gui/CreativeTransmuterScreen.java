package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.setup.TransmuteCatalog;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * 创造物品质变器 GUI（工作台式布局：3×3 输入栏 → 箭头 → 输出栏）。
 * <p>
 * 界面只展示机器的真实物品栏，转化由实体按 tick 结算，因此没有按钮与数据槽；
 * 标题颜色由 {@code getDisplayName()} 上套的品级样式给出，这里传的颜色参数只是兜底。
 * <p>
 * 标题栏右侧有一个 "?" 配方帮助卡（参考零刻压印器与生物农场的同款卡片）：
 * 配方写死在 {@link TransmuteCatalog} 里，JEI 与配方书都看不到，因此这里是游戏内查配方的地方。
 */
public class CreativeTransmuterScreen extends AbstractContainerScreen<CreativeTransmuterMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("alltheimbaium", "textures/gui/creative_transmuter_gui.png");

    // ==================== "?" 配方帮助卡 ====================
    /** 帮助卡每页行数 */
    private static final int HELP_PAGE_LINES = 12;
    private static final int HELP_COLOR = 0xFFFFD24D;
    /** "?" 与标题同一行 */
    private static final int HELP_Y = 6;
    /** 箭头两侧的小留白（像素） */
    private static final int HELP_COL_GAP = 4;
    /** 帮助里的箭头符号：产物 ← 材料 */
    private static final String HELP_ARROW = "←";

    /** "?" 的水平位置（gui 局部坐标，init 计算） */
    private int helpX = 0;
    private int helpPage = 0;
    /** 配方摘要缓存：注册表在会话内不变，首次用到时取一次 */
    private List<TransmuteCatalog.Summary> recipes;

    public CreativeTransmuterScreen(@Nonnull CreativeTransmuterMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        // 26.x：imageWidth/imageHeight 是 final 字段，尺寸经由超类构造器传入
        super(menu, playerInventory, title, 176, 166);
        // 玩家背包采用标准四行布局
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 标题栏右侧的 "?"，右对齐到内边距 8
        this.helpX = this.imageWidth - 8 - this.font.width("?");
        this.helpPage = 0;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && isHoverHelp((int) mouseX, (int) mouseY)) {
            cycleHelp(); // 点击 "?" 翻页（纯客户端）
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 26.x：背景贴图改在 extractBackground 里绘制（renderBg 已删除），先让父类画好暗化/模糊底
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void extractLabels(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        // 原版默认实现用写死的颜色且不画阴影，这里统一覆写，与其余界面保持一致
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, true);
        guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
        guiGraphics.text(this.font, "?", this.helpX, HELP_Y, HELP_COLOR, true);
    }

    @Override
    public void extractRenderState(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 26.x：背景由 AbstractContainerScreen#extractRenderState 内部统一提交，这里不能再手动调
        // extractBackground(...)（它已改为 4 参，且会回调 extractContents，直接递归）
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        if (isHoverHelp(mouseX, mouseY)) {
            renderRecipeCard(guiGraphics, mouseX, mouseY);
        }
        this.extractTooltip(guiGraphics, mouseX, mouseY);
    }

    // ==================== "?" 配方帮助卡 ====================

    /**
     * 配方摘要。配方表是静态的，取到一次就缓存。
     */
    @Nonnull
    private List<TransmuteCatalog.Summary> recipes() {
        if (this.recipes == null) {
            this.recipes = TransmuteCatalog.summaries();
        }
        return this.recipes;
    }

    /** "?" 字形是否被悬停 */
    private boolean isHoverHelp(int mouseX, int mouseY) {
        return mouseX >= this.leftPos + this.helpX - 2
                && mouseX <= this.leftPos + this.helpX + this.font.width("?") + 2
                && mouseY >= this.topPos + HELP_Y - 2 && mouseY <= this.topPos + HELP_Y + 9;
    }

    /** 总页数（每页 {@link #HELP_PAGE_LINES} 条，至少 1 页） */
    private int totalPages(int total, int per) {
        return Math.max(1, (total + per - 1) / per);
    }

    /** 点击 "?" 翻页 */
    private void cycleHelp() {
        int count = recipes().size();
        if (count > 0) {
            this.helpPage = (this.helpPage + 1) % totalPages(count, HELP_PAGE_LINES);
        }
    }

    /** 半透明卡片底板：黑色 1px 边框 + 半透明底 */
    private void drawHelpPanel(@Nonnull GuiGraphicsExtractor guiGraphics, int bx, int by, int boxW, int boxH) {
        guiGraphics.fill(bx - 1, by - 1, bx + boxW + 1, by, 0xFF000000);
        guiGraphics.fill(bx - 1, by + boxH, bx + boxW + 1, by + boxH + 1, 0xFF000000);
        guiGraphics.fill(bx - 1, by, bx, by + boxH, 0xFF000000);
        guiGraphics.fill(bx + boxW, by, bx + boxW + 1, by + boxH, 0xFF000000);
        guiGraphics.fill(bx, by, bx + boxW, by + boxH, 0xF0100010);
    }

    /** 卡片左上角 x（跟随鼠标但不出屏） */
    private int cardX(int mouseX, int boxW) {
        int x = mouseX + 8;
        if (x + boxW > this.width) {
            x = mouseX - 8 - boxW;
        }
        return Math.max(2, x);
    }

    /** 卡片左上角 y（跟随鼠标但不出屏） */
    private int cardY(int mouseY, int boxH) {
        int y = mouseY + 8;
        if (y + boxH > this.height) {
            y = mouseY - 8 - boxH;
        }
        return Math.max(2, y);
    }

    /**
     * 自绘配方帮助卡：一行一条配方，格式 {@code 产物 ← 材料 ×9}。
     * 产物列按本页最长项留白，使各行的箭头与材料列逐行对齐。
     */
    private void renderRecipeCard(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        Component header = Component.translatable("screen.alltheimbaium.creative_transmuter.help.header").withStyle(ChatFormatting.GRAY);
        List<TransmuteCatalog.Summary> list = recipes();
        int hpad = 4;
        int vpad = 4;
        int lineH = this.font.lineHeight + 1;

        if (list.isEmpty()) {
            // 未装 Mekanism：配方表里一条都解析不出来
            Component empty = Component.translatable("screen.alltheimbaium.creative_transmuter.help.empty").withStyle(ChatFormatting.GRAY);
            int boxW = Math.max(this.font.width(header), this.font.width(empty)) + hpad * 2;
            int boxH = vpad * 2 + lineH * 2;
            int bx = cardX(mouseX, boxW);
            int by = cardY(mouseY, boxH);
            guiGraphics.pose().pushMatrix();
            guiGraphics.nextStratum();
            drawHelpPanel(guiGraphics, bx, by, boxW, boxH);
            guiGraphics.text(this.font, header, bx + hpad, by + vpad, 0xFFFFFFFF, true);
            guiGraphics.text(this.font, empty, bx + hpad, by + vpad + lineH, 0xFFFFFFFF, true);
            guiGraphics.pose().popMatrix();
            return;
        }

        int total = list.size();
        int pages = totalPages(total, HELP_PAGE_LINES);
        int from = Math.min(helpPage * HELP_PAGE_LINES, total);
        int to = Math.min(total, from + HELP_PAGE_LINES);

        List<Component> outputs = new ArrayList<>();
        List<Component> inputLines = new ArrayList<>();
        int maxOutW = 0;
        int maxInW = 0;
        for (int i = from; i < to; i++) {
            TransmuteCatalog.Summary summary = list.get(i);
            ItemStack out = summary.output();
            Component outName = Component.literal(out.getHoverName().getString()
                    + (out.getCount() > 1 ? " ×" + out.getCount() : "")).withStyle(ChatFormatting.WHITE);
            Component inLine = Component.literal(summary.input().getHoverName().getString()
                    + " ×" + summary.count()).withStyle(ChatFormatting.WHITE);
            outputs.add(outName);
            inputLines.add(inLine);
            maxOutW = Math.max(maxOutW, this.font.width(outName));
            maxInW = Math.max(maxInW, this.font.width(inLine));
        }
        if (outputs.isEmpty()) {
            return;
        }

        Component footer = Component.translatable("screen.alltheimbaium.creative_transmuter.help.page",
                Math.min(helpPage + 1, pages), pages, total).withStyle(ChatFormatting.GRAY);
        int arrowW = this.font.width(HELP_ARROW);
        int arrowX = maxOutW + HELP_COL_GAP;
        int inX = arrowX + arrowW + HELP_COL_GAP;
        int rowW = inX + maxInW;
        int contentArea = Math.max(Math.max(this.font.width(header), this.font.width(footer)), rowW);
        int boxW = contentArea + hpad * 2;
        int boxH = vpad * 2 + (1 + outputs.size() + 1) * lineH;
        int bx = cardX(mouseX, boxW);
        int by = cardY(mouseY, boxH);

        guiGraphics.pose().pushMatrix();
        guiGraphics.nextStratum();
        drawHelpPanel(guiGraphics, bx, by, boxW, boxH);
        int left = bx + hpad;
        int y = by + vpad;
        guiGraphics.text(this.font, header, left, y, 0xFFFFFFFF, true);
        y += lineH;
        for (int i = 0; i < outputs.size(); i++) {
            guiGraphics.text(this.font, outputs.get(i), left, y, 0xFFFFFFFF, true);
            guiGraphics.text(this.font, HELP_ARROW, left + arrowX, y, 0xFFAAAAAA, true);
            guiGraphics.text(this.font, inputLines.get(i), left + inX, y, 0xFFFFFFFF, true);
            y += lineH;
        }
        guiGraphics.text(this.font, footer, left, y, 0xFFFFFFFF, true);
        guiGraphics.pose().popMatrix();
    }
}
