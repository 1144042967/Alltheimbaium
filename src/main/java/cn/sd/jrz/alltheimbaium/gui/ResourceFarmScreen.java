package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.block.MobFarmBlock;
import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
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
 * 通用资源农场 GUI（复用 mob_farm 布局贴图）。
 * 顶部：标题/当前标记与等级；右上标记槽；中部 27 个产物行虚拟槽（单击取1/Shift取组/空格取满，左下角存量缩写）；
 * 右上角一个 "?" 帮助分页展示"标记→其产物"；下方玩家背包。
 */
@OnlyIn(Dist.CLIENT)
public class ResourceFarmScreen extends AbstractContainerScreen<ResourceFarmMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/mob_farm_gui.png");

    private static final int HELP_PAGE_LINES = 20;
    private static final int HELP_MAX_PRODUCTS_SHOWN = 8;
    private static final int HELP_COLOR = 0xFFFFD24D;
    private static final String HELP_ARROW = "→";
    private static final int HELP_Z = 400;
    /** 单个 "?" 纵向：标记槽上方，横向对标记槽中心(152+8) */
    private static final int HELP_Y = 12;
    private int helpX = 0;
    private int helpPage = 0;
    private boolean spaceDown = false;

    private static final float COUNT_SCALE = 0.5F;

    // 六面输出状态按钮 + 总开关 + 进度条（复用 mob_farm 几何）
    private static final int BTN_W = 48;
    private static final int BTN_H = 16;
    private static final int[] BTN_XS = {8, 64, 120};
    private static final int[] BTN_YS = {102, 123};
    private static final int TOOL_BTN_W = 48;
    private static final int TOOL_BTN_H = 12;
    private static final int OUTPUT_BTN_X = 176 - TOOL_BTN_W - 8;
    private static final int OUTPUT_BTN_Y = 145;
    private static final int PROGRESS_X = 8;
    private static final int PROGRESS_Y = 34;
    private static final int PROGRESS_W = 132;
    private static final int PROGRESS_H = 4;

    private final FaceButton[] faceButtons = new FaceButton[6];
    private StateButton outputButton;

    public ResourceFarmScreen(ResourceFarmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 242;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.helpX = 152 + 8 - this.font.width("?") / 2;
        this.helpPage = 0;
        for (int i = 0; i < 6; i++) {
            Direction direction = Direction.values()[i];
            this.faceButtons[i] = new FaceButton(this.leftPos + BTN_XS[i % 3], this.topPos + BTN_YS[i / 3], direction,
                    button -> sendButton(ResourceFarmMenu.BUTTON_DIR_BASE + direction.ordinal()));
            this.addRenderableWidget(this.faceButtons[i]);
        }
        this.outputButton = new StateButton(this.leftPos + OUTPUT_BTN_X, this.topPos + OUTPUT_BTN_Y, TOOL_BTN_W, TOOL_BTN_H,
                this.menu.isOutputEnabled(),
                Component.translatable("screen.alltheimbaium.mob_farm.output"),
                button -> sendButton(ResourceFarmMenu.BUTTON_OUTPUT));
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
            if (isHoverHelp(mx, my)) {
                cycleHelpPage();
                return true;
            }
            for (int i = 0; i < ResourceFarmMenu.MAX_PRODUCTS; i++) {
                Slot slot = this.menu.slots.get(ResourceFarmMenu.SLOT_PRODUCT_BASE + i);
                if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    int id;
                    if (hasShiftDown()) {
                        id = ResourceFarmMenu.BUTTON_EXTRACT_STACK_BASE + i;
                    } else if (this.spaceDown) {
                        id = ResourceFarmMenu.BUTTON_EXTRACT_ALL_BASE + i;
                    } else {
                        id = ResourceFarmMenu.BUTTON_EXTRACT_ONE_BASE + i;
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
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFAA00, true);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x000000, false);
        // 当前标记
        Component markerText = Component.translatable("screen.alltheimbaium.resource_farm.marked",
                markerDisplay());
        guiGraphics.drawString(this.font, markerText, 8, 14, 0xFFFFFF, true);
        // 等级 + 进度
        guiGraphics.drawString(this.font, Component.translatable("screen.alltheimbaium.resource_farm.level_progress",
                this.menu.getLevel(), growthPercent()), 8, 24, 0xFFFFFF, true);
        // 右上 "?"（对标记槽中心，槽上方）
        guiGraphics.drawString(this.font, "?", this.helpX, HELP_Y, HELP_COLOR, true);
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
        renderMarkerSlotTooltip(guiGraphics, mouseX, mouseY);
        if (isHoverHelp(mouseX, mouseY)) {
            renderHelpCard(guiGraphics, mouseX, mouseY);
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /** 产物槽/槽数量 tooltip */
    @Override
    protected void renderTooltip(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot slot = findHoveredSlot(mouseX, mouseY);
        if (slot != null && !slot.getItem().isEmpty()) {
            int idx = slot.index;
            if (idx >= ResourceFarmMenu.SLOT_PRODUCT_BASE && idx < ResourceFarmMenu.SLOT_PRODUCT_BASE + ResourceFarmMenu.MAX_PRODUCTS) {
                int i = idx - ResourceFarmMenu.SLOT_PRODUCT_BASE;
                long stock = this.menu.getProductStock(i);
                long weight = this.menu.getProductWeight(i);
                List<Component> lines = new ArrayList<>();
                lines.add(slot.getItem().getHoverName().copy().withStyle(ChatFormatting.WHITE));
                lines.add(Component.translatable("screen.alltheimbaium.resource_farm.count", stock));
                if (weight > 0) {
                    lines.add(Component.translatable("screen.alltheimbaium.resource_farm.rate",
                            formatSpeed(weight, this.menu.getLevel())));
                }
                guiGraphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderMarkerSlotTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Slot marker = this.menu.slots.get(ResourceFarmMenu.SLOT_MARKER);
        if (!marker.hasItem() && this.isHovering(marker.x, marker.y, 16, 16, mouseX, mouseY)) {
            boolean marked = this.menu.getMarkerItemId() > 0;
            guiGraphics.renderTooltip(this.font, List.of(
                    Component.translatable(marked
                            ? "screen.alltheimbaium.resource_farm.marker_locked.1"
                            : "screen.alltheimbaium.resource_farm.marker_tooltip.1"),
                    Component.translatable(marked
                            ? "screen.alltheimbaium.resource_farm.marker_locked.2"
                            : "screen.alltheimbaium.resource_farm.marker_tooltip.2")
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    private boolean isHoverHelp(int mouseX, int mouseY) {
        return mouseX >= this.leftPos + this.helpX - 2
                && mouseX <= this.leftPos + this.helpX + this.font.width("?") + 2
                && mouseY >= this.topPos + HELP_Y - 2 && mouseY <= this.topPos + HELP_Y + 9;
    }

    private void cycleHelpPage() {
        if (this.menu.helpRowCount() > 0) {
            int pages = totalPages(this.menu.helpRowCount(), HELP_PAGE_LINES);
            this.helpPage = (this.helpPage + 1) % pages;
        }
    }

    private int totalPages(int total, int per) {
        return Math.max(1, (total + per - 1) / per);
    }

    /** 自绘帮助卡片：每行 "标记物 → 产物…"（一行一个资源，产物过多截断） */
    private void renderHelpCard(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int total = this.menu.helpRowCount();
        if (total <= 0) {
            return;
        }
        int pages = totalPages(total, HELP_PAGE_LINES);
        int from = Math.min(this.helpPage * HELP_PAGE_LINES, total);
        int to = Math.min(total, from + HELP_PAGE_LINES);
        List<Component> markNames = new ArrayList<>();
        List<Component> prodTexts = new ArrayList<>();
        List<Integer> markW = new ArrayList<>();
        List<Integer> prodW = new ArrayList<>();
        for (int r = from; r < to; r++) {
            Item marker = this.menu.helpRowMarker(r);
            if (marker == null || marker == Items.AIR) {
                continue;
            }
            Component mn = new ItemStack(marker).getHoverName().copy().withStyle(ChatFormatting.WHITE);
            markNames.add(mn);
            markW.add(this.font.width(mn));
            Component pt = buildProductText(r);
            prodTexts.add(pt);
            prodW.add(this.font.width(pt));
        }
        int rows = markNames.size();
        if (rows == 0) {
            return;
        }
        int maxMark = 0, maxProd = 0;
        for (int i = 0; i < rows; i++) {
            maxMark = Math.max(maxMark, markW.get(i));
            maxProd = Math.max(maxProd, prodW.get(i));
        }
        Component header = Component.translatable("screen.alltheimbaium.resource_farm.help.header").withStyle(ChatFormatting.GRAY);
        Component footer = Component.translatable("screen.alltheimbaium.resource_farm.help.page",
                Math.min(this.helpPage + 1, pages), pages, total).withStyle(ChatFormatting.GRAY);
        int arrowW = this.font.width(HELP_ARROW);
        int hpad = 4, vpad = 4, lineH = this.font.lineHeight + 1;
        int arrowLeft = maxMark + 3;
        int itemsLeft = arrowLeft + arrowW + 3;
        int content = Math.max(Math.max(this.font.width(header), this.font.width(footer)), itemsLeft + maxProd);
        int boxW = content + hpad * 2;
        int totalLines = 1 + rows + 1;
        int boxH = vpad * 2 + totalLines * lineH;
        int bx = mouseX + 8;
        if (bx + boxW > this.width) {
            bx = mouseX - 8 - boxW;
        }
        bx = Math.max(2, bx);
        int by = mouseY + 8;
        if (by + boxH > this.height) {
            by = mouseY - 8 - boxH;
        }
        by = Math.max(2, by);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0D, 0.0D, HELP_Z);
        guiGraphics.fill(bx - 1, by - 1, bx + boxW + 1, by, 0xFF000000);
        guiGraphics.fill(bx - 1, by + boxH, bx + boxW + 1, by + boxH + 1, 0xFF000000);
        guiGraphics.fill(bx - 1, by, bx, by + boxH, 0xFF000000);
        guiGraphics.fill(bx + boxW, by, bx + boxW + 1, by + boxH, 0xFF000000);
        guiGraphics.fill(bx, by, bx + boxW, by + boxH, 0xF0100010);
        int contentLeft = bx + hpad;
        int y = by + vpad;
        guiGraphics.drawString(this.font, header, contentLeft, y, 0xFFFFFF, true);
        y += lineH;
        for (int i = 0; i < rows; i++) {
            guiGraphics.drawString(this.font, markNames.get(i), contentLeft, y, 0xFFFFFF, true);
            guiGraphics.drawString(this.font, HELP_ARROW, contentLeft + arrowLeft, y, 0xFFAAAAAA, true);
            guiGraphics.drawString(this.font, prodTexts.get(i), contentLeft + itemsLeft, y, 0xFFFFFF, true);
            y += lineH;
        }
        guiGraphics.drawString(this.font, footer, contentLeft, y, 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    private Component buildProductText(int row) {
        int count = this.menu.helpRowItemCount(row);
        MutableComponent text = Component.literal("");
        boolean first = true;
        int shown = Math.min(count, HELP_MAX_PRODUCTS_SHOWN);
        for (int k = 0; k < shown; k++) {
            Item item = this.menu.helpRowItem(row, k);
            if (item == null || item == Items.AIR) {
                continue;
            }
            if (!first) {
                text.append(Component.literal("、").withStyle(ChatFormatting.GRAY));
            }
            text.append(new ItemStack(item).getHoverName().copy().withStyle(ChatFormatting.WHITE));
            first = false;
        }
        if (count > shown) {
            if (!first) {
                text.append(Component.literal("、").withStyle(ChatFormatting.GRAY));
            }
            text.append(Component.translatable("screen.alltheimbaium.resource_farm.help.more",
                    count - shown).withStyle(ChatFormatting.GRAY));
        }
        if (first) {
            text.append(Component.literal("-").withStyle(ChatFormatting.GRAY));
        }
        return text;
    }

    @Nullable
    private Slot findHoveredSlot(int mouseX, int mouseY) {
        for (Slot slot : this.menu.slots) {
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private void drawSlotCounts(GuiGraphics guiGraphics) {
        for (int i = 0; i < ResourceFarmMenu.MAX_PRODUCTS; i++) {
            long stock = this.menu.getProductStock(i);
            if (stock <= 0) {
                continue;
            }
            Slot slot = this.menu.slots.get(ResourceFarmMenu.SLOT_PRODUCT_BASE + i);
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y + 12;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 300);
            guiGraphics.pose().scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
            guiGraphics.drawString(this.font, formatCount(stock), (int) (x / COUNT_SCALE), (int) (y / COUNT_SCALE), 0xFFFFFF, true);
            guiGraphics.pose().popPose();
        }
    }

    private Component markerDisplay() {
        int id = this.menu.getMarkerItemId();
        if (id > 0) {
            Item item = BuiltInRegistries.ITEM.byId(id);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item).getHoverName();
            }
        }
        return Component.translatable("screen.alltheimbaium.resource_farm.not_marked");
    }

    private int growthPercent() {
        long second = Math.max(1, MobFarmBlock.getLevelUpIntervalSeconds()); // 与 MobFarm 同参数
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
    }

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

    private static boolean isChinese() {
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("zh");
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

    /** 六面输出状态按钮（显示相邻目标 + 输出材料，风格同生物农场） */
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
                ResourceFarmScreen.this.sendButton(ResourceFarmMenu.BUTTON_DIR_REVERSE_BASE + this.direction.ordinal());
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int state = ResourceFarmScreen.this.menu.getDirectionState(this.direction);
            int color;
            if (state == ResourceFarmEntity.STATE_DISABLED) {
                color = 0xFFAA0000;
            } else if (state == ResourceFarmEntity.STATE_RANDOM) {
                color = 0xFF00AA00;
            } else {
                color = 0xFF3A3A6B;
            }
            renderButton(guiGraphics, color);
            ItemStack neighborIcon = ResourceFarmScreen.this.getNeighborIcon(this.direction);
            boolean isChinese = isChinese();
            String dirName = Component.translatable("screen.alltheimbaium.mob_farm.face." + this.direction.getName()).getString();
            if (!isChinese) {
                dirName = directionSymbol(this.direction);
            }
            String stateText = null;
            ItemStack slotIcon = ItemStack.EMPTY;
            String slotText = null;
            if (state >= ResourceFarmEntity.STATE_SLOT_BASE) {
                int slot = state - ResourceFarmEntity.STATE_SLOT_BASE;
                slotIcon = ResourceFarmScreen.this.menu.getProductStack(slot);
                if (slotIcon.isEmpty()) {
                    slotText = String.valueOf(slot + 1);
                }
            } else if (state == ResourceFarmEntity.STATE_RANDOM) {
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
            contentW += hasTargetIcon ? 16 : ResourceFarmScreen.this.font.width(dirName);
            contentW += ResourceFarmScreen.this.font.width("←") + 2;
            contentW += hasSlotIcon ? 16 : ResourceFarmScreen.this.font.width(rightText);
            contentW += 2;
            int x = this.getX() + Math.max(1, (BTN_W - contentW) / 2);
            int iconY = this.getY();
            int textY = this.getY() + 4;
            if (hasTargetIcon) {
                guiGraphics.renderItem(neighborIcon, x, iconY);
                x += 18;
            } else {
                guiGraphics.drawString(ResourceFarmScreen.this.font, dirName, x, textY, 0xFFFFFFFF, true);
                x += ResourceFarmScreen.this.font.width(dirName) + 2;
            }
            guiGraphics.drawString(ResourceFarmScreen.this.font, "←", x, textY, 0xFFFFFFFF, true);
            x += ResourceFarmScreen.this.font.width("←") + 2;
            if (hasSlotIcon) {
                guiGraphics.renderItem(slotIcon, x, iconY);
            } else {
                guiGraphics.drawString(ResourceFarmScreen.this.font, rightText, x, textY, 0xFFFFFFFF, true);
            }
        }

        List<Component> buildTooltip() {
            int state = ResourceFarmScreen.this.menu.getDirectionState(this.direction);
            ItemStack neighborIcon = ResourceFarmScreen.this.getNeighborIcon(this.direction);
            String dirName = Component.translatable("screen.alltheimbaium.mob_farm.face." + this.direction.getName()).getString();
            String content;
            if (state >= ResourceFarmEntity.STATE_SLOT_BASE) {
                int slot = state - ResourceFarmEntity.STATE_SLOT_BASE;
                ItemStack materialIcon = ResourceFarmScreen.this.menu.getProductStack(slot);
                content = materialIcon.isEmpty()
                        ? Component.translatable("screen.alltheimbaium.output.slot", slot + 1).getString()
                        : materialIcon.getHoverName().getString();
            } else if (state == ResourceFarmEntity.STATE_RANDOM) {
                content = Component.translatable("screen.alltheimbaium.output.random").getString();
            } else {
                content = Component.translatable("screen.alltheimbaium.output.disabled").getString();
            }
            String target = neighborIcon.isEmpty()
                    ? null
                    : ResourceFarmScreen.this.getNeighborName(this.direction).getString();
            return FaceTooltip.build(dirName, target, content);
        }
    }

    /** 带状态的按钮（输出总开关） */
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
            guiGraphics.drawCenteredString(ResourceFarmScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
