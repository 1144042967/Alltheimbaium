package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 生物农场 GUI（176 宽）。
 * <p>
 * 顶部：标题、收容生物与等级/升级进度；右上标记槽(刷怪蛋/特征物收容)与使用槽(物品自动模拟右击)。
 * 中部：27 个产物行虚拟槽（单击取 1、Shift 取 1 组、空格取到背包满，左下角缩写存量）。
 * 下部：六面输出状态按钮 + 主动输出开关 + 清空收容物按钮；最下方玩家背包。
 */
@OnlyIn(Dist.CLIENT)
public class MobFarmScreen extends AbstractContainerScreen<MobFarmMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/mob_farm_gui.png");

    // 六面状态按钮
    private static final int BTN_W = 48;
    private static final int BTN_H = 16;
    private static final int[] BTN_XS = {8, 64, 120};
    private static final int[] BTN_YS = {102, 123};
    // 输出按钮：放在物品栏标签行右侧，右缘与物品栏最右侧(176-8)对齐
    private static final int TOOL_BTN_W = 48;
    private static final int TOOL_BTN_H = 12;
    private static final int OUTPUT_BTN_X = 176 - TOOL_BTN_W - 8;
    private static final int OUTPUT_BTN_Y = 145;

    // 信息/进度条（renderBg 屏幕坐标用这些常量 + leftPos/topPos）
    private static final int INFO_X = 8;
    private static final int PROGRESS_X = 8;
    private static final int PROGRESS_Y = 37;
    private static final int PROGRESS_W = 132;
    private static final int PROGRESS_H = 4;

    private static final float COUNT_SCALE = 0.5F;

    // 右上角两个 "?" 帮助（整体对标记槽中心线）：
    //   A(左)：标记物 → 收容生物（每行左右两组，一页 20 行 → 40 条）
    //   B(右)：生物 → 其全部产物（一行一个生物，同样分页）
    private static final int HELP_PAGE_LINES = 20;                 // 每页行数
    private static final int HELP_PAGE_PAIRS = HELP_PAGE_LINES * 2; // A 每页映射条数（左右两栏）
    private static final int HELP_COLOR = 0xFFFFD24D;
    /** 两个 "?" 之间的水平间距（像素） */
    private static final int HELP_GLYPH_GAP = 4;
    /** 收容/使用合一槽（标记槽）中心 x：152 + 16/2 */
    private static final int HELP_SLOT_CENTER_X = 160;
    /** "?" 纵向位置：标记槽上方 */
    private static final int HELP_Y = 12;
    /** 箭头两侧的小留白（像素） */
    private static final int HELP_COL_GAP = 3;
    /** A 卡片左右两栏映射之间的大间隙（像素） */
    private static final int HELP_GROUP_GAP = 26;
    /** B 卡片：单个生物至多列出的产物数，超出加 "…等 N 项" */
    private static final int HELP_MAX_PRODUCTS_SHOWN = 6;
    /** 帮助里的箭头符号 */
    private static final String HELP_ARROW = "→";
    /** 自绘帮助卡片的抬升 z，确保盖过槽位里的物品贴图 */
    private static final int HELP_Z = 400;
    /** 左 "?"(标记物→生物) 的水平位置（gui 局部坐标，init 计算） */
    private int helpX1 = 0;
    /** 右 "?"(生物→产物) 的水平位置（gui 局部坐标，init 计算） */
    private int helpX2 = 0;
    /** A 卡当前页（从 0 起） */
    private int helpPageA = 0;
    /** B 卡当前页（从 0 起） */
    private int helpPageB = 0;

    private final FaceButton[] faceButtons = new FaceButton[6];
    private StateButton outputButton;
    private boolean spaceDown = false;

    public MobFarmScreen(MobFarmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 242;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 两个 "?" 作为一个整体在标记槽中心线上居中
        int glyphW = this.font.width("?");
        int groupW = glyphW * 2 + HELP_GLYPH_GAP;
        int groupLeft = HELP_SLOT_CENTER_X - groupW / 2;
        this.helpX1 = groupLeft;
        this.helpX2 = groupLeft + glyphW + HELP_GLYPH_GAP;
        this.helpPageA = 0;
        this.helpPageB = 0;
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.faceButtons[i] = new FaceButton(this.leftPos + BTN_XS[i % 3], this.topPos + BTN_YS[i / 3], direction,
                    button -> sendButton(MobFarmMenu.BUTTON_DIR_BASE + direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        this.outputButton = new StateButton(this.leftPos + OUTPUT_BTN_X, this.topPos + OUTPUT_BTN_Y, TOOL_BTN_W, TOOL_BTN_H,
                this.menu.isOutputEnabled(),
                Component.translatable("screen.alltheimbaium.mob_farm.output"),
                button -> sendButton(MobFarmMenu.BUTTON_OUTPUT));
        this.addRenderableWidget(this.outputButton);
    }

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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (button == 0) {
            if (isHoverHelpA(mx, my)) {
                cycleHelpA(); // 点击左 "?" 翻 A 卡（纯客户端）
                return true;
            }
            if (isHoverHelpB(mx, my)) {
                cycleHelpB(); // 点击右 "?" 翻 B 卡（纯客户端）
                return true;
            }
        }
        if (button == 0) {
            for (int i = 0; i < MobFarmMenu.MAX_PRODUCTS; i++) {
                Slot slot = this.menu.slots.get(MobFarmMenu.SLOT_PRODUCT_BASE + i);
                if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    int id;
                    if (hasShiftDown()) {
                        id = MobFarmMenu.BUTTON_EXTRACT_STACK_BASE + i;
                    } else if (this.spaceDown) {
                        id = MobFarmMenu.BUTTON_EXTRACT_ALL_BASE + i;
                    } else {
                        id = MobFarmMenu.BUTTON_EXTRACT_ONE_BASE + i;
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
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 升级进度条
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
        // 标题：金色，无阴影；收容/等级描述：白字 + 黑阴影（同补给箱风格）
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFAA00, true);
        // 物品栏标签：黑色、不加阴影
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x000000, false);
        // 收容生物
        int containedId = this.menu.getContainedEntityId();
        Component contained;
        if (containedId > 0) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byId(containedId);
            contained = type == null ? Component.literal("?") : type.getDescription();
        } else {
            contained = Component.translatable("screen.alltheimbaium.mob_farm.empty");
        }
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.mob_farm.contained", contained), INFO_X, 16, 0xFFFFFF, true);
        // 等级 + 升级百分比
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.mob_farm.level_progress",
                this.menu.getLevel(), growthPercent()), INFO_X, 27, 0xFFFFFF, true);
        // 右上标记槽上方两个黄色 "?"：左=A(标记物→生物)、右=B(生物→产物)，整体对槽中心线
        guiGraphics.drawString(this.font, "?", this.helpX1, HELP_Y, HELP_COLOR, true);
        guiGraphics.drawString(this.font, "?", this.helpX2, HELP_Y, HELP_COLOR, true);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.flush();
        drawSlotCounts(guiGraphics);
        this.outputButton.setState(this.menu.isOutputEnabled());
        // 六面按钮 tooltip
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton.isHovered()) {
                guiGraphics.renderTooltip(this.font, faceButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
            }
        }
        renderSpecialSlotTooltips(guiGraphics, mouseX, mouseY);
        // 两个 "?" 帮助卡片
        if (isHoverHelpA(mouseX, mouseY)) {
            renderHelpCardA(guiGraphics, mouseX, mouseY);
        }
        if (isHoverHelpB(mouseX, mouseY)) {
            renderHelpCardB(guiGraphics, mouseX, mouseY);
        }
        // 经由本类重载：产物槽显示"数量+速度"，其它槽走默认
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** 某 "?" 字形是否被悬停 */
    private boolean hoverGlyph(int glyphX, int mouseX, int mouseY) {
        return mouseX >= this.leftPos + glyphX - 2
                && mouseX <= this.leftPos + glyphX + this.font.width("?") + 2
                && mouseY >= this.topPos + HELP_Y - 2 && mouseY <= this.topPos + HELP_Y + 9;
    }

    private boolean isHoverHelpA(int mouseX, int mouseY) {
        return hoverGlyph(this.helpX1, mouseX, mouseY);
    }

    private boolean isHoverHelpB(int mouseX, int mouseY) {
        return hoverGlyph(this.helpX2, mouseX, mouseY);
    }

    /** 总页数（每页 per 条，至少 1 页） */
    private int totalPages(int total, int per) {
        return Math.max(1, (total + per - 1) / per);
    }

    /** 点击左 "?" 翻 A 卡 */
    private void cycleHelpA() {
        if (this.menu.markerCount() > 0) {
            this.helpPageA = (this.helpPageA + 1) % totalPages(this.menu.markerCount(), HELP_PAGE_PAIRS);
        }
    }

    /** 点击右 "?" 翻 B 卡 */
    private void cycleHelpB() {
        if (this.menu.productRowCount() > 0) {
            this.helpPageB = (this.helpPageB + 1) % totalPages(this.menu.productRowCount(), HELP_PAGE_LINES);
        }
    }

    /** 半透明卡片底板：黑色 1px 边框 + 半透明底 */
    private void drawHelpPanel(GuiGraphics guiGraphics, int bx, int by, int boxW, int boxH) {
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
     * 自绘 卡片A：标记物 → 收容生物，每行两组（左右两栏），"→" 符号。
     * 各栏的 物品名/生物名 按本页该栏最长项的像素宽度留白，因此两栏箭头与生物名逐行对齐；
     * 全角（中文等）/半角由字体实际像素宽自然区分。自绘前抬升 z，盖过槽位里的物品贴图。
     */
    private void renderHelpCardA(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int count = this.menu.markerCount();
        if (count <= 0) {
            return;
        }
        int pages = totalPages(count, HELP_PAGE_PAIRS);
        int from = Math.min(this.helpPageA * HELP_PAGE_PAIRS, count);
        int to = Math.min(count, from + HELP_PAGE_PAIRS);

        List<Component> itemC = new ArrayList<>();
        List<Component> mobC = new ArrayList<>();
        List<Integer> itemW = new ArrayList<>();
        List<Integer> mobW = new ArrayList<>();
        for (int i = from; i < to; i++) {
            Item item = this.menu.markerItem(i);
            EntityType<?> type = this.menu.markerType(i);
            if (item == null || item == Items.AIR || type == null) {
                continue;
            }
            Component itemName = new ItemStack(item).getHoverName().copy().withStyle(ChatFormatting.WHITE);
            Component mobName = Component.translatable(type.getDescriptionId()).withStyle(ChatFormatting.YELLOW);
            itemC.add(itemName);
            mobC.add(mobName);
            itemW.add(this.font.width(itemName));
            mobW.add(this.font.width(mobName));
        }
        int n = itemC.size();
        if (n == 0) {
            return;
        }
        // 条目按顺序 2 个一组：每行第 0、2、4… 个在左栏，第 1、3、5… 个在右栏
        int rows = (n + 1) / 2;
        boolean hasRight = n > 1;
        int maxLItem = 0, maxLMob = 0, maxRItem = 0, maxRMob = 0;
        for (int r = 0; r < rows; r++) {
            int li = r * 2;
            maxLItem = Math.max(maxLItem, itemW.get(li));
            maxLMob = Math.max(maxLMob, mobW.get(li));
            int ri = li + 1;
            if (ri < n) {
                maxRItem = Math.max(maxRItem, itemW.get(ri));
                maxRMob = Math.max(maxRMob, mobW.get(ri));
            }
        }
        Component header = Component.translatable("screen.alltheimbaium.mob_farm.help.header").withStyle(ChatFormatting.GRAY);
        Component footer = Component.translatable("screen.alltheimbaium.mob_farm.help.page",
                Math.min(this.helpPageA + 1, pages), pages, count).withStyle(ChatFormatting.GRAY);
        int arrowW = this.font.width(HELP_ARROW);

        int hpad = 4;
        int vpad = 4;
        int lineH = this.font.lineHeight + 1;
        // 各栏相对内容起点的 x（掉落物列按最长项留白，生物列起点固定）
        int lItemX = 0;
        int lArrowX = maxLItem + HELP_COL_GAP;
        int lMobX = lArrowX + arrowW + HELP_COL_GAP;
        int rItemX = lMobX + maxLMob + HELP_GROUP_GAP;
        int rArrowX = rItemX + maxRItem + HELP_COL_GAP;
        int rMobX = rArrowX + arrowW + HELP_COL_GAP;
        int rowW = hasRight ? (rMobX + maxRMob) : (lMobX + maxLMob);
        int contentArea = Math.max(Math.max(this.font.width(header), this.font.width(footer)), rowW);
        int boxW = contentArea + hpad * 2;
        int totalLines = 1 + rows + 1;
        int boxH = vpad * 2 + totalLines * lineH;
        int bx = cardX(mouseX, boxW);
        int by = cardY(mouseY, boxH);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, HELP_Z);
        drawHelpPanel(guiGraphics, bx, by, boxW, boxH);
        int contentLeft = bx + hpad;
        int y = by + vpad;
        guiGraphics.drawString(this.font, header, contentLeft, y, 0xFFFFFF, true);
        y += lineH;
        for (int r = 0; r < rows; r++) {
            int li = r * 2;
            drawHelpPair(guiGraphics, contentLeft + lItemX, contentLeft + lArrowX, contentLeft + lMobX,
                    itemC.get(li), mobC.get(li), y);
            int ri = li + 1;
            if (ri < n) {
                drawHelpPair(guiGraphics, contentLeft + rItemX, contentLeft + rArrowX, contentLeft + rMobX,
                        itemC.get(ri), mobC.get(ri), y);
            }
            y += lineH;
        }
        guiGraphics.drawString(this.font, footer, contentLeft, y, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    /** 画一组 "物品 → 生物"（按传入的三列起点绘制，保证逐行对齐） */
    private void drawHelpPair(GuiGraphics guiGraphics, int itemX, int arrowX, int mobX,
                              Component item, Component mob, int y) {
        guiGraphics.drawString(this.font, item, itemX, y, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, HELP_ARROW, arrowX, y, 0xFFAAAAAA, true);
        guiGraphics.drawString(this.font, mob, mobX, y, 0xFFFFFF, true);
    }

    /**
     * 自绘 卡片B：生物 → 其全部产物（一行一个生物）。
     * 生物名列按本页最长生物像素宽留白使箭头对齐；产物列左对齐，过多时截断。
     */
    private void renderHelpCardB(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int total = this.menu.productRowCount();
        if (total <= 0) {
            return;
        }
        int pages = totalPages(total, HELP_PAGE_LINES);
        int from = Math.min(this.helpPageB * HELP_PAGE_LINES, total);
        int to = Math.min(total, from + HELP_PAGE_LINES);

        List<Component> bios = new ArrayList<>();
        List<Component> prodTexts = new ArrayList<>();
        List<Integer> bioW = new ArrayList<>();
        List<Integer> prodW = new ArrayList<>();
        for (int r = from; r < to; r++) {
            EntityType<?> type = this.menu.productRowType(r);
            if (type == null) {
                continue;
            }
            Component bio = Component.translatable(type.getDescriptionId()).withStyle(ChatFormatting.YELLOW);
            bios.add(bio);
            bioW.add(this.font.width(bio));
            Component prodText = buildProductText(r);
            prodTexts.add(prodText);
            prodW.add(this.font.width(prodText));
        }
        int rows = bios.size();
        if (rows == 0) {
            return;
        }
        int maxBioW = 0;
        int maxProdW = 0;
        for (int i = 0; i < rows; i++) {
            maxBioW = Math.max(maxBioW, bioW.get(i));
            maxProdW = Math.max(maxProdW, prodW.get(i));
        }
        Component header = Component.translatable("screen.alltheimbaium.mob_farm.help2.header").withStyle(ChatFormatting.GRAY);
        Component footer = Component.translatable("screen.alltheimbaium.mob_farm.help.page",
                Math.min(this.helpPageB + 1, pages), pages, total).withStyle(ChatFormatting.GRAY);
        int arrowW = this.font.width(HELP_ARROW);

        int hpad = 4;
        int vpad = 4;
        int lineH = this.font.lineHeight + 1;
        int arrowLeft = maxBioW + HELP_COL_GAP;
        int itemsLeft = arrowLeft + arrowW + HELP_COL_GAP;
        int rowW = itemsLeft + maxProdW;
        int contentArea = Math.max(Math.max(this.font.width(header), this.font.width(footer)), rowW);
        int boxW = contentArea + hpad * 2;
        int totalLines = 1 + rows + 1;
        int boxH = vpad * 2 + totalLines * lineH;
        int bx = cardX(mouseX, boxW);
        int by = cardY(mouseY, boxH);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, HELP_Z);
        drawHelpPanel(guiGraphics, bx, by, boxW, boxH);
        int contentLeft = bx + hpad;
        int y = by + vpad;
        guiGraphics.drawString(this.font, header, contentLeft, y, 0xFFFFFF, true);
        y += lineH;
        for (int i = 0; i < rows; i++) {
            guiGraphics.drawString(this.font, bios.get(i), contentLeft, y, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, HELP_ARROW, contentLeft + arrowLeft, y, 0xFFAAAAAA, true);
            guiGraphics.drawString(this.font, prodTexts.get(i), contentLeft + itemsLeft, y, 0xFFFFFF, true);
            y += lineH;
        }
        guiGraphics.drawString(this.font, footer, contentLeft, y, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    /** 组装某行的产物文本：前 HELP_MAX_PRODUCTS_SHOWN 个产物名 顿号连接，超出加 "…等 N 项" */
    private Component buildProductText(int row) {
        int itemCount = this.menu.productRowItemCount(row);
        MutableComponent text = Component.literal("");
        boolean first = true;
        int shown = Math.min(itemCount, HELP_MAX_PRODUCTS_SHOWN);
        for (int k = 0; k < shown; k++) {
            Item item = this.menu.productRowItem(row, k);
            if (item == null || item == Items.AIR) {
                continue;
            }
            if (!first) {
                text.append(Component.literal("、").withStyle(ChatFormatting.GRAY));
            }
            text.append(new ItemStack(item).getHoverName().copy().withStyle(ChatFormatting.WHITE));
            first = false;
        }
        if (itemCount > shown) {
            if (!first) {
                text.append(Component.literal("、").withStyle(ChatFormatting.GRAY));
            }
            text.append(Component.translatable("screen.alltheimbaium.mob_farm.help2.more",
                    itemCount - shown).withStyle(ChatFormatting.GRAY));
        }
        if (first) {
            text.append(Component.literal("-").withStyle(ChatFormatting.GRAY));
        }
        return text;
    }

    /** 找到鼠标悬浮的槽位（在渲染 tooltip 时机，容器内部 hoveredSlot 不可靠，自行用 isHovering 判断） */
    @Nullable
    private Slot findHoveredSlot(int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    /** 产物槽自定义 tooltip（数量 + 生成速度），覆盖默认单件 tooltip */
    @Override
    protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = this.findHoveredSlot(mouseX, mouseY);
        if (slot != null && !slot.getItem().isEmpty()) {
            int idx = slot.index;
            if (idx >= MobFarmMenu.SLOT_PRODUCT_BASE && idx < MobFarmMenu.SLOT_PRODUCT_BASE + MobFarmMenu.MAX_PRODUCTS) {
                int i = idx - MobFarmMenu.SLOT_PRODUCT_BASE;
                long stock = this.menu.getProductStock(i);
                long weight = this.menu.getProductWeight(i);
                int mode = this.menu.getRowMode(i);
                int toolStatus = this.menu.getToolStatus();
                List<Component> lines = new ArrayList<>();
                lines.add(slot.getItem().getHoverName().copy().withStyle(ChatFormatting.WHITE));
                lines.add(Component.translatable("screen.alltheimbaium.mob_farm.count", stock));
                if (mode == 1) {
                    // 使用槽正在产出该物品：标注来源 + 速度与刷怪蛋一致
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.tool_from"));
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.rate",
                            formatSpeed(Math.max(1, weight), this.menu.getLevel())));
                } else if (mode == 2) {
                    // 使用槽工具行但当前不产出（缺少工具/工具不符）：生成速度 0
                    lines.add(Component.translatable(toolStatus == 1
                            ? "screen.alltheimbaium.mob_farm.missing_tool"
                            : "screen.alltheimbaium.mob_farm.wrong_tool"));
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.rate_zero"));
                } else if (weight > 0 && this.menu.getContainedEntityId() > 0) {
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.rate", formatSpeed(weight, this.menu.getLevel())));
                } else if (weight <= 0) {
                    lines.add(Component.translatable("screen.alltheimbaium.mob_farm.manual"));
                }
                guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderSpecialSlotTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 收容/使用合一槽（槽空时按状态显示说明）
        Slot special = this.menu.slots.get(MobFarmMenu.SLOT_SPECIAL);
        if (!special.hasItem() && this.isHovering(special.x, special.y, 16, 16, mouseX, mouseY)) {
            boolean contained = this.menu.getContainedEntityId() > 0;
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.translatable(contained
                            ? "screen.alltheimbaium.mob_farm.use_tooltip.1"
                            : "screen.alltheimbaium.mob_farm.marker_tooltip.1"),
                    Component.translatable(contained
                            ? "screen.alltheimbaium.mob_farm.use_tooltip.2"
                            : "screen.alltheimbaium.mob_farm.marker_tooltip.2")
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    /** 绘制 27 个产物槽左下角的存量缩写 */
    private void drawSlotCounts(GuiGraphics guiGraphics) {
        for (int i = 0; i < MobFarmMenu.MAX_PRODUCTS; i++) {
            long stock = this.menu.getProductStock(i);
            if (stock <= 0) {
                continue;
            }
            String text = formatCount(stock);
            Slot slot = this.menu.slots.get(MobFarmMenu.SLOT_PRODUCT_BASE + i);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y + 12;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            guiGraphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
            guiGraphics.drawString(this.font, text, (int) (x / COUNT_SCALE), (int) (y / COUNT_SCALE), 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }
    }

    private int growthPercent() {
        long second = Math.max(1, MobFarmBlock.getLevelUpIntervalSeconds());
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
    }

    /** 生成速度格式化：重量×等级 /500 (件/秒) */
    private static String formatSpeed(long weight, long level) {
        BigDecimal speed = new BigDecimal(weight).multiply(new BigDecimal(level))
                .divide(new BigDecimal(500), 3, RoundingMode.HALF_UP);
        return speed.stripTrailingZeros().toPlainString();
    }

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
        return String.format("%.1fP", value / 1_000_000_000_000_000.0);
    }

    private ItemStack getNeighborIcon(Direction direction) {
        Item item = getNeighborState(direction).getBlock().asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private Component getNeighborName(Direction direction) {
        return getNeighborState(direction).getBlock().getName();
    }

    private BlockState getNeighborState(Direction direction) {
        if (this.minecraft != null && this.minecraft.level != null && this.menu.entity != null) {
            return this.minecraft.level.getBlockState(this.menu.entity.getBlockPos().relative(direction));
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static String directionSymbol(Direction direction) {
        return switch (direction) {
            case DOWN -> "↓";
            case UP -> "↑";
            case NORTH -> "▲";
            case SOUTH -> "▼";
            case WEST -> "◀";
            case EAST -> "▶";
        };
    }

    private static boolean isChinese() {
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("zh");
    }

    /** 六面输出状态按钮（随机/禁用/槽1~槽27 循环），显示相邻目标贴图 + 输出材料贴图/文字 */
    private class FaceButton extends SimpleButton {
        private final Direction direction;

        FaceButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, BTN_W, BTN_H, Component.literal(""), onPress);
            this.direction = direction;
        }

        /**
         * 左键沿用 onPress 的正向循环；右键发反向 id，由菜单侧反向循环。
         */
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1 && this.active && this.visible && this.clicked(mouseX, mouseY)) {
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                MobFarmScreen.this.sendButton(MobFarmMenu.BUTTON_DIR_REVERSE_BASE + this.direction.ordinal());
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int state = MobFarmScreen.this.menu.getDirectionState(this.direction);
            int color;
            if (state == MobFarmEntity.STATE_DISABLED) {
                color = 0xFFAA0000;
            } else if (state == MobFarmEntity.STATE_RANDOM) {
                color = 0xFF00AA00;
            } else {
                color = 0xFF3A3A6B;
            }
            renderButton(guiGraphics, color);
            ItemStack neighborIcon = MobFarmScreen.this.getNeighborIcon(this.direction);
            boolean isChinese = isChinese();
            String dirName = Component.translatable("screen.alltheimbaium.mob_farm.face." + this.direction.getName()).getString();
            if (!isChinese) {
                dirName = directionSymbol(this.direction);
            }
            String stateText = null;
            ItemStack slotIcon = ItemStack.EMPTY;
            String slotText = null;
            if (state >= MobFarmEntity.STATE_SLOT_BASE) {
                int slot = state - MobFarmEntity.STATE_SLOT_BASE;
                slotIcon = MobFarmScreen.this.menu.getProductStack(slot);
                if (slotIcon.isEmpty()) {
                    slotText = String.valueOf(slot + 1);
                }
            } else if (state == MobFarmEntity.STATE_RANDOM) {
                stateText = isChinese
                        ? Component.translatable("screen.alltheimbaium.mob_farm.random").getString()
                        : "?";
            } else {
                stateText = isChinese
                        ? Component.translatable("screen.alltheimbaium.mob_farm.disabled").getString()
                        : "×";
            }
            boolean hasTargetIcon = !neighborIcon.isEmpty();
            boolean hasSlotIcon = !slotIcon.isEmpty();
            String rightText = hasSlotIcon ? null : (slotText != null ? slotText : stateText);
            int contentW = 0;
            contentW += hasTargetIcon ? 16 : MobFarmScreen.this.font.width(dirName);
            contentW += MobFarmScreen.this.font.width("←") + 2;
            contentW += hasSlotIcon ? 16 : MobFarmScreen.this.font.width(rightText);
            contentW += 2;
            int x = this.getX() + Math.max(1, (BTN_W - contentW) / 2);
            int iconY = this.getY();
            int textY = this.getY() + 4;
            if (hasTargetIcon) {
                guiGraphics.renderItem(neighborIcon, x, iconY);
                x += 18;
            } else {
                guiGraphics.drawString(MobFarmScreen.this.font, dirName, x, textY, 0xFFFFFFFF, true);
                x += MobFarmScreen.this.font.width(dirName) + 2;
            }
            guiGraphics.drawString(MobFarmScreen.this.font, "←", x, textY, 0xFFFFFFFF, true);
            x += MobFarmScreen.this.font.width("←") + 2;
            if (hasSlotIcon) {
                guiGraphics.renderItem(slotIcon, x, iconY);
            } else {
                guiGraphics.drawString(MobFarmScreen.this.font, rightText, x, textY, 0xFFFFFFFF, true);
            }
        }

        List<Component> buildTooltip() {
            int state = MobFarmScreen.this.menu.getDirectionState(this.direction);
            ItemStack neighborIcon = MobFarmScreen.this.getNeighborIcon(this.direction);
            String dirName = Component.translatable("screen.alltheimbaium.mob_farm.face." + this.direction.getName()).getString();
            String content;
            if (state >= MobFarmEntity.STATE_SLOT_BASE) {
                int slot = state - MobFarmEntity.STATE_SLOT_BASE;
                ItemStack materialIcon = MobFarmScreen.this.menu.getProductStack(slot);
                content = materialIcon.isEmpty()
                        ? Component.translatable("screen.alltheimbaium.output.slot", slot + 1).getString()
                        : materialIcon.getHoverName().getString();
            } else if (state == MobFarmEntity.STATE_RANDOM) {
                content = Component.translatable("screen.alltheimbaium.output.random").getString();
            } else {
                content = Component.translatable("screen.alltheimbaium.output.disabled").getString();
            }
            String target = neighborIcon.isEmpty()
                    ? null
                    : MobFarmScreen.this.getNeighborName(this.direction).getString();
            return FaceTooltip.build(dirName, target, content);
        }
    }

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

    private abstract class SimpleButton extends Button {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color) {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.drawCenteredString(MobFarmScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
