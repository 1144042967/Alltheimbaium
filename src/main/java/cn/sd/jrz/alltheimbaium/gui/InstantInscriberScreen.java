package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 零刻压印器 GUI（大数版）。
 * <p>
 * 顶部标题 + FE 能量条；两行输入格（18，可投料/取回）、一行输出格（9，取成品）；
 * 中间一排：6 个六面推送开关 + 模式切换按钮（显示当前 压板/组装，点击切换）。格内缩写存量小字。
 */
@OnlyIn(Dist.CLIENT)
public class InstantInscriberScreen extends AbstractContainerScreen<InstantInscriberMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/instant_inscriber_gui.png");

    private static final int ENERGY_X = 8;
    private static final int ENERGY_Y = 16;
    private static final int ENERGY_W = 160;
    private static final int ENERGY_H = 6;
    private static final int ENERGY_TRACK_COLOR = 0xFF373737;
    private static final int ENERGY_FILL_COLOR = 0xFFFF8000;

    private static final int FACE_Y = 63;
    private static final int FACE_SIZE = 16;
    private static final int FACE_STEP = 18;
    private static final int FACE_X_BASE = 8;
    private static final int MODE_X = 121;
    private static final int MODE_Y = 63;
    private static final int MODE_W = 47;
    private static final int MODE_H = 16;

    private static final float COUNT_SCALE = 0.5F;

    // 标题栏右侧两个 "?" 帮助（参考生物农场）：
    //   左：压板模式支持的配方（1 份中间原料 → 它支持的全部压板）
    //   右：组装模式支持的配方（消耗三格全部材料）
    /** 帮助卡每页行数 */
    private static final int HELP_PAGE_LINES = 20;
    private static final int HELP_COLOR = 0xFFFFD24D;
    /** 两个 "?" 之间的水平间距（像素） */
    private static final int HELP_GLYPH_GAP = 4;
    /** "?" 与标题同一行 */
    private static final int HELP_Y = 6;
    /** 箭头两侧的小留白（像素） */
    private static final int HELP_COL_GAP = 4;
    /** 一行里最多列出的输入材料数，超出折叠为"…等 N 项" */
    private static final int HELP_MAX_INPUTS = 3;
    /** 帮助里的箭头符号：输出 ← 输入 */
    private static final String HELP_ARROW = "←";
    /** 自绘帮助卡片的抬升 z，确保盖过槽位里的物品贴图 */
    private static final int HELP_Z = 400;

    /** 左 "?"(压板) 的水平位置（gui 局部坐标，init 计算） */
    private int helpX1 = 0;
    /** 右 "?"(组装) 的水平位置（gui 局部坐标，init 计算） */
    private int helpX2 = 0;
    private int helpPageA = 0;
    private int helpPageB = 0;
    /** 配方摘要缓存：客户端 RecipeManager 在会话内不变，首次用到时取一次 */
    private List<InstantInscriberEntity.PressSummary> pressRecipes;
    private List<InstantInscriberEntity.RecipeSummary> assemblyRecipes;

    private final FaceButton[] faceButtons = new FaceButton[6];
    private Button modeButton;
    private boolean spaceDown = false;

    public InstantInscriberScreen(InstantInscriberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = InstantInscriberMenu.IMAGE_HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // 标题栏右侧两个 "?"，右对齐到内边距 8
        int glyphW = this.font.width("?");
        int right = this.imageWidth - 8;
        this.helpX2 = right - glyphW;
        this.helpX1 = this.helpX2 - HELP_GLYPH_GAP - glyphW;
        this.helpPageA = 0;
        this.helpPageB = 0;
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.faceButtons[i] = new FaceButton(this.leftPos + FACE_X_BASE + i * FACE_STEP + i, this.topPos + FACE_Y,
                    direction, button -> sendButton(InstantInscriberMenu.BUTTON_FACE_BASE + direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        // 模式切换：显示当前模式，点击切换（hover 说明目标模式）
        this.modeButton = Button.builder(Component.literal(""),
                        button -> sendButton(InstantInscriberMenu.BUTTON_MODE))
                .bounds(this.leftPos + MODE_X, this.topPos + MODE_Y, MODE_W, MODE_H)
                .build();
        this.addRenderableWidget(this.modeButton);
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
        if (button == 0) {
            int mx = (int) mouseX;
            int my = (int) mouseY;
            if (isHoverHelpA(mx, my)) {
                cycleHelpA(); // 点击左 "?" 翻压板配方卡（纯客户端）
                return true;
            }
            if (isHoverHelpB(mx, my)) {
                cycleHelpB(); // 点击右 "?" 翻组装配方卡（纯客户端）
                return true;
            }
            int totalCells = InstantInscriberEntity.INPUT_MAX_TYPES + InstantInscriberEntity.OUTPUT_MAX_TYPES;
            for (int i = 0; i < totalCells; i++) {
                Slot slot = this.menu.slots.get(i);
                if (!this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    continue;
                }
                boolean isInput = i < InstantInscriberEntity.INPUT_MAX_TYPES;
                int cell = isInput ? i : i - InstantInscriberEntity.INPUT_MAX_TYPES;
                if (isInput && !this.menu.getCarried().isEmpty()) {
                    sendButton(InstantInscriberMenu.BUTTON_DEPOSIT_INPUT);
                    this.menu.setCarried(ItemStack.EMPTY);
                    return true;
                }
                if (isInput) {
                    sendButton(pickExtractButton(InstantInscriberMenu.BUTTON_INPUT_ONE_BASE,
                            InstantInscriberMenu.BUTTON_INPUT_STACK_BASE, InstantInscriberMenu.BUTTON_INPUT_ALL_BASE, cell));
                } else if (this.menu.getCarried().isEmpty()) {
                    sendButton(pickExtractButton(InstantInscriberMenu.BUTTON_OUTPUT_ONE_BASE,
                            InstantInscriberMenu.BUTTON_OUTPUT_STACK_BASE, InstantInscriberMenu.BUTTON_OUTPUT_ALL_BASE, cell));
                } else {
                    return true; // 输出格不可投料
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int pickExtractButton(int oneBase, int stackBase, int allBase, int cell) {
        if (hasShiftDown()) {
            return stackBase + cell;
        }
        if (this.spaceDown) {
            return allBase + cell;
        }
        return oneBase + cell;
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 标题：颜色由菜单标题组件携带品级样式决定，此处带阴影保证在浅色底上可读
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, true);
        // 物品栏标签：保持原版观感，不加阴影
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        // 标题栏右侧两个黄色 "?"：左=压板配方、右=组装配方
        guiGraphics.drawString(this.font, "?", this.helpX1, HELP_Y, HELP_COLOR, true);
        guiGraphics.drawString(this.font, "?", this.helpX2, HELP_Y, HELP_COLOR, true);
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        int trackLeft = this.leftPos + ENERGY_X;
        int trackTop = this.topPos + ENERGY_Y;
        guiGraphics.fill(trackLeft, trackTop, trackLeft + ENERGY_W, trackTop + ENERGY_H, ENERGY_TRACK_COLOR);
        int max = Math.max(1, this.menu.getMaxEnergy());
        int energy = Math.max(0, Math.min(max, this.menu.getEnergy()));
        if (energy > 0) {
            int fill = ENERGY_W * energy / max;
            guiGraphics.fill(trackLeft, trackTop, trackLeft + fill, trackTop + ENERGY_H, ENERGY_FILL_COLOR);
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 模式按钮文案随时刷新（显示当前模式）
        this.modeButton.setMessage(Component.translatable("screen.alltheimbaium.instant_inscriber.mode." + modeKey(this.menu.getMode())));
        // 模式按钮点击后无需保持焦点，避免残留原版按钮的白色聚焦描边
        this.modeButton.setFocused(false);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.flush();
        drawSlotCounts(guiGraphics);
        // FE 能量条 hover
        if (this.isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, buildEnergyTooltip(), Optional.empty(), mouseX, mouseY);
        }
        // 模式按钮 hover：目标模式说明
        if (this.modeButton.isHovered()) {
            int next = this.menu.getMode() == InstantInscriberEntity.MODE_ASSEMBLY
                    ? InstantInscriberEntity.MODE_INSCRIBE : InstantInscriberEntity.MODE_ASSEMBLY;
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.mode_tooltip.current",
                    Component.translatable("screen.alltheimbaium.instant_inscriber.mode." + modeKey(this.menu.getMode()))));
            lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.mode_tooltip.next",
                    Component.translatable("screen.alltheimbaium.instant_inscriber.mode." + modeKey(next))));
            guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
        // 六面开关 tooltip
        for (FaceButton faceButton : this.faceButtons) {
            if (faceButton.isHovered()) {
                guiGraphics.renderTooltip(this.font, faceButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
            }
        }
        // 两个 "?" 帮助卡片
        if (isHoverHelpA(mouseX, mouseY)) {
            renderPressCard(guiGraphics, mouseX, mouseY);
        }
        if (isHoverHelpB(mouseX, mouseY)) {
            renderRecipeCard(guiGraphics, mouseX, mouseY, assemblyRecipes(), this.helpPageB,
                    "screen.alltheimbaium.instant_inscriber.help.assembly.header");
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // ==================== 两个 "?" 配方帮助卡 ====================

    /**
     * 压板模式配方摘要。客户端 {@code RecipeManager} 在会话内不变，取到一次就缓存。
     */
    @Nonnull
    private List<InstantInscriberEntity.PressSummary> pressRecipes() {
        if (this.pressRecipes == null && this.minecraft != null && this.minecraft.level != null) {
            this.pressRecipes = InstantInscriberEntity.inscribeSummaries(this.minecraft.level);
            this.assemblyRecipes = InstantInscriberEntity.assemblySummaries(this.minecraft.level);
        }
        return this.pressRecipes == null ? List.of() : this.pressRecipes;
    }

    /**
     * 组装模式配方摘要（与压板一起在首次取用时构建）
     */
    @Nonnull
    private List<InstantInscriberEntity.RecipeSummary> assemblyRecipes() {
        pressRecipes();
        return this.assemblyRecipes == null ? List.of() : this.assemblyRecipes;
    }

    /** 某个 "?" 字形是否被悬停 */
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

    /** 点击左 "?" 翻压板配方卡 */
    private void cycleHelpA() {
        int count = pressRecipes().size();
        if (count > 0) {
            this.helpPageA = (this.helpPageA + 1) % totalPages(count, HELP_PAGE_LINES);
        }
    }

    /** 点击右 "?" 翻组装配方卡 */
    private void cycleHelpB() {
        int count = assemblyRecipes().size();
        if (count > 0) {
            this.helpPageB = (this.helpPageB + 1) % totalPages(count, HELP_PAGE_LINES);
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
     * 把多个输入材料拼成一行；超出上限折叠为"…等 N 项"
     */
    @Nonnull
    private Component joinInputs(@Nonnull List<ItemStack> inputs) {
        if (inputs.isEmpty()) {
            return Component.literal("-").withStyle(ChatFormatting.GRAY);
        }
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(inputs.size(), HELP_MAX_INPUTS);
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                sb.append(" + ");
            }
            sb.append(inputs.get(i).getHoverName().getString());
        }
        if (inputs.size() > shown) {
            sb.append(Component.translatable("screen.alltheimbaium.instant_inscriber.help.more",
                    inputs.size() - shown).getString());
        }
        return Component.literal(sb.toString()).withStyle(ChatFormatting.WHITE);
    }

    /**
     * 自绘配方帮助卡：一行一条配方，格式 {@code 输出物品 ← 输入物品…}。
     * 输出列按本页最长项留白，使各行的箭头与输入列逐行对齐。
     */
    private void renderRecipeCard(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY,
                                  @Nonnull List<InstantInscriberEntity.RecipeSummary> recipes,
                                  int page, @Nonnull String headerKey) {
        Component header = Component.translatable(headerKey).withStyle(ChatFormatting.GRAY);
        int hpad = 4;
        int vpad = 4;
        int lineH = this.font.lineHeight + 1;

        if (recipes.isEmpty()) {
            renderEmptyCard(guiGraphics, mouseX, mouseY, header);
            return;
        }

        int total = recipes.size();
        int pages = totalPages(total, HELP_PAGE_LINES);
        int from = Math.min(page * HELP_PAGE_LINES, total);
        int to = Math.min(total, from + HELP_PAGE_LINES);

        List<Component> outputs = new ArrayList<>();
        List<Component> inputLines = new ArrayList<>();
        int maxOutW = 0;
        int maxInW = 0;
        for (int i = from; i < to; i++) {
            InstantInscriberEntity.RecipeSummary summary = recipes.get(i);
            ItemStack out = summary.output();
            Component outName = Component.literal(out.getHoverName().getString()
                    + (out.getCount() > 1 ? " ×" + out.getCount() : "")).withStyle(ChatFormatting.WHITE);
            Component inLine = joinInputs(summary.inputs());
            outputs.add(outName);
            inputLines.add(inLine);
            maxOutW = Math.max(maxOutW, this.font.width(outName));
            maxInW = Math.max(maxInW, this.font.width(inLine));
        }
        if (outputs.isEmpty()) {
            return;
        }

        Component footer = Component.translatable("screen.alltheimbaium.instant_inscriber.help.page",
                Math.min(page + 1, pages), pages, total).withStyle(ChatFormatting.GRAY);
        int arrowW = this.font.width(HELP_ARROW);
        int arrowX = maxOutW + HELP_COL_GAP;
        int inX = arrowX + arrowW + HELP_COL_GAP;
        int rowW = inX + maxInW;
        int contentArea = Math.max(Math.max(this.font.width(header), this.font.width(footer)), rowW);
        int boxW = contentArea + hpad * 2;
        int boxH = vpad * 2 + (1 + outputs.size() + 1) * lineH;
        int bx = cardX(mouseX, boxW);
        int by = cardY(mouseY, boxH);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, HELP_Z);
        drawHelpPanel(guiGraphics, bx, by, boxW, boxH);
        int left = bx + hpad;
        int y = by + vpad;
        guiGraphics.drawString(this.font, header, left, y, 0xFFFFFF, true);
        y += lineH;
        for (int i = 0; i < outputs.size(); i++) {
            guiGraphics.drawString(this.font, outputs.get(i), left, y, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, HELP_ARROW, left + arrowX, y, 0xFFAAAAAA, true);
            guiGraphics.drawString(this.font, inputLines.get(i), left + inX, y, 0xFFFFFF, true);
            y += lineH;
        }
        guiGraphics.drawString(this.font, footer, left, y, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    /**
     * 自绘**压板**帮助卡：一行一条 {@code 原料 → 产物}。
     * <p>
     * 方向与组装卡相反——压板是"1 份原料吃出多种压板"，所以**输入排在左侧、输出排在右侧**；
     * 同一份原料支持的多个压板并排在该行后半段（数据侧已在 {@code inscribeSummaries} 里按输入聚合去重）。
     */
    private void renderPressCard(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component header = Component.translatable("screen.alltheimbaium.instant_inscriber.help.press.header").withStyle(ChatFormatting.GRAY);
        List<InstantInscriberEntity.PressSummary> recipes = pressRecipes();
        int hpad = 4;
        int vpad = 4;
        int lineH = this.font.lineHeight + 1;

        if (recipes.isEmpty()) {
            renderEmptyCard(guiGraphics, mouseX, mouseY, header);
            return;
        }

        int total = recipes.size();
        int pages = totalPages(total, HELP_PAGE_LINES);
        int from = Math.min(this.helpPageA * HELP_PAGE_LINES, total);
        int to = Math.min(total, from + HELP_PAGE_LINES);

        List<Component> inputNames = new ArrayList<>();
        List<Component> outputNames = new ArrayList<>();
        int maxInW = 0;
        int maxOutW = 0;
        for (int i = from; i < to; i++) {
            InstantInscriberEntity.PressSummary summary = recipes.get(i);
            Component inName = joinNames(summary.inputs());
            Component outName = joinNames(summary.outputs());
            inputNames.add(inName);
            outputNames.add(outName);
            maxInW = Math.max(maxInW, this.font.width(inName));
            maxOutW = Math.max(maxOutW, this.font.width(outName));
        }
        if (inputNames.isEmpty()) {
            return;
        }

        Component footer = Component.translatable("screen.alltheimbaium.instant_inscriber.help.page",
                Math.min(this.helpPageA + 1, pages), pages, total).withStyle(ChatFormatting.GRAY);
        int arrowW = this.font.width(HELP_ARROW);
        int arrowX = maxInW + HELP_COL_GAP;
        int outX = arrowX + arrowW + HELP_COL_GAP;
        int rowW = outX + maxOutW;
        int contentArea = Math.max(Math.max(this.font.width(header), this.font.width(footer)), rowW);
        int boxW = contentArea + hpad * 2;
        int boxH = vpad * 2 + (1 + inputNames.size() + 1) * lineH;
        int bx = cardX(mouseX, boxW);
        int by = cardY(mouseY, boxH);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, HELP_Z);
        drawHelpPanel(guiGraphics, bx, by, boxW, boxH);
        int left = bx + hpad;
        int y = by + vpad;
        guiGraphics.drawString(this.font, header, left, y, 0xFFFFFF, true);
        y += lineH;
        for (int i = 0; i < inputNames.size(); i++) {
            guiGraphics.drawString(this.font, inputNames.get(i), left, y, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, HELP_ARROW, left + arrowX, y, 0xFFAAAAAA, true);
            guiGraphics.drawString(this.font, outputNames.get(i), left + outX, y, 0xFFFFFF, true);
            y += lineH;
        }
        guiGraphics.drawString(this.font, footer, left, y, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    /** 空态卡片：未装 AE2 或没有可用配方时，只画标题 + 一句提示 */
    private void renderEmptyCard(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, @Nonnull Component header) {
        int hpad = 4;
        int vpad = 4;
        int lineH = this.font.lineHeight + 1;
        Component empty = Component.translatable("screen.alltheimbaium.instant_inscriber.help.empty").withStyle(ChatFormatting.GRAY);
        int boxW = Math.max(this.font.width(header), this.font.width(empty)) + hpad * 2;
        int boxH = vpad * 2 + lineH * 2;
        int bx = cardX(mouseX, boxW);
        int by = cardY(mouseY, boxH);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, HELP_Z);
        drawHelpPanel(guiGraphics, bx, by, boxW, boxH);
        guiGraphics.drawString(this.font, header, bx + hpad, by + vpad, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, empty, bx + hpad, by + vpad + lineH, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    /** 把一组物品名用 {@code " / "} 连起来，数量 > 1 时带 ×N */
    @Nonnull
    private static Component joinNames(@Nonnull List<ItemStack> stacks) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack stack : stacks) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(stack.getHoverName().getString());
            if (stack.getCount() > 1) {
                sb.append(" ×").append(stack.getCount());
            }
        }
        return Component.literal(sb.toString()).withStyle(ChatFormatting.WHITE);
    }

    private static String modeKey(int mode) {
        return mode == InstantInscriberEntity.MODE_ASSEMBLY ? "assemble" : "press";
    }

    @Override
    protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = findRowSlot(mouseX, mouseY);
        if (slot != null) {
            int idx = slot.index;
            boolean isInput = idx < InstantInscriberEntity.INPUT_MAX_TYPES;
            int cell = isInput ? idx : idx - InstantInscriberEntity.INPUT_MAX_TYPES;
            ItemStack cur = isInput ? this.menu.getInputStack(cell) : this.menu.getOutputStack(cell);
            List<Component> lines = new ArrayList<>();
            if (isInput && !this.menu.getCarried().isEmpty()) {
                lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.deposit_hint",
                        this.menu.getCarried().getHoverName()));
            } else if (!cur.isEmpty()) {
                lines.add(cur.getHoverName().copy().withStyle(style -> style.withColor(0xFFFFFF)));
                lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.row_count",
                        isInput ? this.menu.getInputStock(cell) : this.menu.getOutputStock(cell)));
                lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.extract_usage"));
            } else {
                return;
            }
            guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
            return;
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private Slot findRowSlot(int mouseX, int mouseY) {
        int totalCells = InstantInscriberEntity.INPUT_MAX_TYPES + InstantInscriberEntity.OUTPUT_MAX_TYPES;
        for (int i = 0; i < totalCells; i++) {
            Slot slot = this.menu.slots.get(i);
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private void drawSlotCounts(GuiGraphics guiGraphics) {
        for (int i = 0; i < InstantInscriberEntity.INPUT_MAX_TYPES; i++) {
            drawCount(guiGraphics, this.menu.getInputStock(i), this.menu.slots.get(i));
        }
        for (int i = 0; i < InstantInscriberEntity.OUTPUT_MAX_TYPES; i++) {
            drawCount(guiGraphics, this.menu.getOutputStock(i),
                    this.menu.slots.get(InstantInscriberEntity.INPUT_MAX_TYPES + i));
        }
    }

    private void drawCount(GuiGraphics guiGraphics, long stock, Slot slot) {
        if (stock <= 0) {
            return;
        }
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y + 12;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        guiGraphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
        guiGraphics.drawString(this.font, formatCount(stock), (int) (x / COUNT_SCALE), (int) (y / COUNT_SCALE), 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    private List<Component> buildEnergyTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.energy_tooltip",
                String.format("%,d", this.menu.getEnergy()), String.format("%,d", this.menu.getMaxEnergy())));
        lines.add(Component.translatable("screen.alltheimbaium.instant_inscriber.energy_usage",
                String.format("%,d", this.menu.getEnergyPerOp())));
        return lines;
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
        if (value < 1_000_000_000_000_000_000L) {
            return String.format("%.1fP", value / 1_000_000_000_000_000.0);
        }
        return String.format("%.1fE", value / 1_000_000_000_000_000_000.0);
    }

    private static String directionArrow(Direction direction) {
        return switch (direction) {
            case DOWN -> "↓";
            case UP -> "↑";
            case NORTH -> "▲";
            case SOUTH -> "▼";
            case WEST -> "◀";
            case EAST -> "▶";
        };
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
     * 获取指定方向相邻方块的物品图标（无方块或方块无对应物品时返回空）
     */
    private ItemStack getNeighborIcon(Direction direction) {
        Item item = getNeighborState(direction).getBlock().asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    /**
     * 获取指定方向相邻方块的显示名
     */
    private Component getNeighborName(Direction direction) {
        return getNeighborState(direction).getBlock().getName();
    }

    private class FaceButton extends Button {
        private final Direction direction;

        FaceButton(int x, int y, Direction direction, OnPress onPress) {
            super(x, y, FACE_SIZE, FACE_SIZE, Component.literal(""), onPress, DEFAULT_NARRATION);
            this.direction = direction;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int state = InstantInscriberScreen.this.menu.getDirectionState(this.direction);
            int color = state == InstantInscriberEntity.STATE_DISABLED ? 0xFFAA0000 : 0xFF00AA00;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            ItemStack neighborIcon = InstantInscriberScreen.this.getNeighborIcon(this.direction);
            if (!neighborIcon.isEmpty()) {
                // 有相邻方块：按钮 16x16 与贴图等大，直接铺满显示，不再画方向箭头
                guiGraphics.renderItem(neighborIcon, this.getX(), this.getY());
            } else {
                guiGraphics.drawCenteredString(InstantInscriberScreen.this.font, directionArrow(this.direction),
                        this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
            }
        }

        /**
         * hover tooltip：内容（启用/禁用）/ 输出方向 / 输出目标
         */
        @Nonnull
        List<Component> buildTooltip() {
            int state = InstantInscriberScreen.this.menu.getDirectionState(this.direction);
            ItemStack neighborIcon = InstantInscriberScreen.this.getNeighborIcon(this.direction);
            String dirName = Component.translatable("screen.alltheimbaium.instant_inscriber.face." + this.direction.getName()).getString();
            String content = Component.translatable(state == InstantInscriberEntity.STATE_DISABLED
                    ? "screen.alltheimbaium.output.disabled"
                    : "screen.alltheimbaium.output.enabled").getString();
            String target = neighborIcon.isEmpty()
                    ? null
                    : InstantInscriberScreen.this.getNeighborName(this.direction).getString();
            return FaceTooltip.build(dirName, target, content);
        }
    }
}
