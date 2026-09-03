package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.setup.SupplyData;
import cn.sd.jrz.alltheimbaium.setup.SupplyRoll;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ATI 补给箱 GUI（纯代码绘制，176 宽，无物品栏）。
 * <p>
 * 顶部三行：最大补给点 / 已用补给点 / 自动刷新倒计时（到下一个整点游戏小时自动刷新）；
 * 中部两排共 10 个"物品标记"按钮（每排 5 个，按创造物品栏 10 个分类顺序随机出一件物品），
 * 点击某个按钮 = 选中该分类随机出的物品；
 * 下方：物品槽（显示选中的物品）、兑换按钮（消耗 3 点给 1 件选中物品）、刷新按钮（消耗 1 点重新随机 10 件）。
 * 兑换/刷新后服务端按新状态重新随机；条件不足时按钮置灰不可点，原因在 tooltip 中按需分行显示。
 */
@OnlyIn(Dist.CLIENT)
public class SupplyCrateScreen extends AbstractContainerScreen<SupplyCrateMenu> {
    /** GUI 背景贴图（占位图，可直接用 PS 修改替换） */
    private static final ResourceLocation TEXTURE = new ResourceLocation("alltheimbaium", "textures/gui/supply_crate_gui.png");
    private static final int IMAGE_W = 176;
    private static final int IMAGE_H = 150;
    // 两排物品按钮
    private static final int ITEM_BOX = 26;
    private static final int ITEM_GAP = 6;
    private static final int ROW_W = ITEM_BOX * 5 + ITEM_GAP * 4;
    private static final int ITEM_X0 = (IMAGE_W - ROW_W) / 2;
    private static final int ITEM_Y1 = 52;
    private static final int ITEM_Y2 = 84;
    // 底部：物品槽 + 兑换/刷新
    private static final int SLOT_X = 14;
    private static final int SLOT_Y = 120;
    private static final int SLOT_SIZE = 18;
    private static final int ACT_Y = 121;
    private static final int REDEEM_X = 46;
    private static final int REDEEM_W = 54;
    private static final int REFRESH_X = 112;
    private static final int REFRESH_W = 50;
    private static final int ACT_H = 16;
    /** GUI 顶部描述文字颜色（与物品名称一致的白色） */
    private static final int TEXT_COLOR = 0xFFFFFF;
    // 顶部“补给说明”帮助图标位置
    private static final int HELP_X = IMAGE_W - 18;
    private static final int HELP_Y = 18;

    private final ItemButton[] itemButtons = new ItemButton[10];
    private ActionButton redeemButton;
    private ActionButton refreshButton;

    public SupplyCrateScreen(SupplyCrateMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
    }

    @Override
    protected void init() {
        super.init();
        for (int i = 0; i < 10; i++) {
            final int index = i;
            int col = i % 5;
            int row = i / 5;
            int x = this.leftPos + ITEM_X0 + col * (ITEM_BOX + ITEM_GAP);
            int y = this.topPos + (row == 0 ? ITEM_Y1 : ITEM_Y2);
            this.itemButtons[i] = new ItemButton(x, y, index,
                    button -> sendButton(SupplyCrateMenu.BUTTON_SELECT_BASE + index));
            this.addRenderableWidget(this.itemButtons[i]);
        }
        this.redeemButton = new ActionButton(this.leftPos + REDEEM_X, this.topPos + ACT_Y, REDEEM_W, ACT_H,
                Component.translatable("screen.alltheimbaium.supply_crate.redeem"),
                SupplyData.COST_REDEEM, true,
                button -> sendButton(SupplyCrateMenu.BUTTON_REDEEM));
        this.addRenderableWidget(this.redeemButton);
        this.refreshButton = new ActionButton(this.leftPos + REFRESH_X, this.topPos + ACT_Y, REFRESH_W, ACT_H,
                Component.translatable("screen.alltheimbaium.supply_crate.refresh"),
                SupplyData.COST_REFRESH, false,
                button -> sendButton(SupplyCrateMenu.BUTTON_REFRESH));
        this.addRenderableWidget(this.refreshButton);
    }

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
        // 界面文字统一带阴影，与按钮内的文字效果一致
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, true);
        guiGraphics.drawString(this.font,
                Component.translatable("screen.alltheimbaium.supply_crate.max_point", this.menu.getMax()),
                10, 20, TEXT_COLOR, true);
        guiGraphics.drawString(this.font,
                Component.translatable("screen.alltheimbaium.supply_crate.used_point", this.menu.getUsed()),
                10, 30, TEXT_COLOR, true);
        // 兑换列表自动刷新倒计时（到下一个真实小时）
        guiGraphics.drawString(this.font,
                Component.translatable("screen.alltheimbaium.supply_crate.refresh_countdown", countdownText()),
                10, 40, TEXT_COLOR, true);
        // 右上角“补给说明”帮助图标（黄色 ?，悬停看来源/自动刷新说明）
        guiGraphics.drawString(this.font, "?", HELP_X, HELP_Y, 0xFFFFD24D, true);
    }

    /**
     * 鼠标是否悬停在右上角“补给说明”图标上
     */
    private boolean isHoverHelp(int mouseX, int mouseY) {
        return mouseX >= this.leftPos + HELP_X - 2 && mouseX <= this.leftPos + HELP_X + 10
                && mouseY >= this.topPos + HELP_Y - 2 && mouseY <= this.topPos + HELP_Y + 9;
    }

    /**
     * 补给说明 tooltip：只讲补给点来源与列表自动刷新（不含兑换/刷新操作说明）
     */
    private List<Component> helpTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("screen.alltheimbaium.supply_crate.help_title"));
        lines.add(Component.translatable("screen.alltheimbaium.supply_crate.help_max"));
        lines.add(Component.translatable("screen.alltheimbaium.supply_crate.help_achieve"));
        lines.add(Component.translatable("screen.alltheimbaium.supply_crate.help_auto"));
        return lines;
    }

    /**
     * 距下一个真实小时(世界累计时长 % 72000 tick == 0)的剩余时间，格式 m:ss
     */
    private String countdownText() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return "0:00";
        }
        long tickInHour = this.minecraft.level.getGameTime() % (20L * 60 * 60);
        int secondsLeft = (int) Math.ceil((20L * 60 * 60 - tickInHour) / 20.0);
        if (secondsLeft < 0) {
            secondsLeft = 0;
        }
        return (secondsLeft / 60) + ":" + String.format("%02d", secondsLeft % 60);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.flush();
        // 刷新按钮状态（条件不足置灰不可点）
        boolean canRedeem = hasSelection() && this.menu.getRemaining() >= SupplyData.COST_REDEEM;
        boolean canRefresh = this.menu.getRemaining() >= SupplyData.COST_REFRESH;
        this.redeemButton.setCanUse(canRedeem);
        this.refreshButton.setCanUse(canRefresh);
        for (int i = 0; i < 10; i++) {
            this.itemButtons[i].setSelected(this.menu.getSelectedIndex() == i);
        }
        drawSelectedSlot(guiGraphics);
        // 补给说明 tooltip
        if (isHoverHelp(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, helpTooltip(), Optional.empty(), mouseX, mouseY);
        }
        // 其余 tooltip
        for (ItemButton itemButton : this.itemButtons) {
            if (itemButton.isHovered()) {
                guiGraphics.renderTooltip(this.font, itemButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
            }
        }
        if (this.redeemButton.isHovered()) {
            guiGraphics.renderTooltip(this.font, this.redeemButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
        }
        if (this.refreshButton.isHovered()) {
            guiGraphics.renderTooltip(this.font, this.refreshButton.buildTooltip(), Optional.empty(), mouseX, mouseY);
        }
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private boolean hasSelection() {
        int sel = this.menu.getSelectedIndex();
        return sel >= 0 && sel < 10 && !this.menu.getRolledStack(sel).isEmpty();
    }

    /**
     * 绘制底部选中物品槽（槽底 + 选中的物品图标）
     */
    private void drawSelectedSlot(GuiGraphics guiGraphics) {
        int x = this.leftPos + SLOT_X;
        int y = this.topPos + SLOT_Y;
        guiGraphics.fill(x - 1, y - 1, x + SLOT_SIZE + 1, y, 0xFF000000);
        guiGraphics.fill(x - 1, y + SLOT_SIZE, x + SLOT_SIZE + 1, y + SLOT_SIZE + 1, 0xFF000000);
        guiGraphics.fill(x - 1, y, x, y + SLOT_SIZE, 0xFF000000);
        guiGraphics.fill(x + SLOT_SIZE, y, x + SLOT_SIZE + 1, y + SLOT_SIZE, 0xFF000000);
        guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF8B8B8B);
        int selected = this.menu.getSelectedIndex();
        if (selected >= 0 && selected < 10) {
            ItemStack stack = this.menu.getRolledStack(selected);
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, x + 1, y + 1);
            }
        }
    }

    private static void drawBorder(GuiGraphics guiGraphics, int x, int y, int w, int h, int border) {
        guiGraphics.fill(x - 1, y - 1, x + w + 1, y, border);
        guiGraphics.fill(x - 1, y + h, x + w + 1, y + h + 1, border);
        guiGraphics.fill(x - 1, y, x, y + h, border);
        guiGraphics.fill(x + w, y, x + w + 1, y + h, border);
    }

    /**
     * 两排 10 个"物品标记"按钮：显示对应分类随机出的物品图标，点击选中。
     */
    private class ItemButton extends Button {
        private final int index;
        private boolean selected;

        ItemButton(int x, int y, int index, OnPress onPress) {
            super(x, y, ITEM_BOX, ITEM_BOX, Component.literal(""), onPress, DEFAULT_NARRATION);
            this.index = index;
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            ItemStack stack = SupplyCrateScreen.this.menu.getRolledStack(this.index);
            int color = stack.isEmpty() ? 0xFF333333 : (this.selected ? 0xFF00AA00 : 0xFF3A3A6B);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int border;
            if (this.selected) {
                border = 0xFFFFFFFF;
            } else if (this.isHovered()) {
                border = 0xFFFFFF00;
            } else {
                border = 0xFF000000;
            }
            drawBorder(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), border);
            if (!stack.isEmpty()) {
                int off = (ITEM_BOX - 16) / 2;
                guiGraphics.renderItem(stack, this.getX() + off, this.getY() + off);
            }
        }

        List<Component> buildTooltip() {
            ItemStack stack = SupplyCrateScreen.this.menu.getRolledStack(this.index);
            List<Component> lines = new ArrayList<>();
            if (stack.isEmpty()) {
                lines.add(Component.translatable("screen.alltheimbaium.supply_crate.no_item"));
            } else {
                lines.add(stack.getHoverName());
            }
            String token = SupplyRoll.CATEGORY_TOKENS[this.index];
            lines.add(Component.translatable("screen.alltheimbaium.supply_crate.from_category",
                    Component.translatable("screen.alltheimbaium.supply_crate.category." + token)));
            lines.add(Component.translatable("screen.alltheimbaium.supply_crate.select_hint"));
            return lines;
        }
    }

    /**
     * 带条件可用状态的按钮（兑换/刷新）。
     * 文案不含消耗信息（只放按钮标签）；tooltip 中显示消耗，并“按需”列出未满足的条件，条件满足时不额外提示。
     */
    private class ActionButton extends Button {
        private final int cost;
        private final boolean requiresSelection;
        private boolean canUse;

        ActionButton(int x, int y, int width, int height, Component label, int cost, boolean requiresSelection, OnPress onPress) {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
            this.cost = cost;
            this.requiresSelection = requiresSelection;
            this.canUse = false;
        }

        void setCanUse(boolean canUse) {
            this.canUse = canUse;
            this.active = canUse;
        }

        @Override
        protected void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            int color = this.canUse ? 0xFF00AA00 : 0xFF555555;
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int border = (this.isHovered() && this.canUse) ? 0xFFFFFF00 : 0xFF000000;
            drawBorder(guiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight(), border);
            guiGraphics.drawCenteredString(SupplyCrateScreen.this.font, this.getMessage(),
                    this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }

        List<Component> buildTooltip() {
            List<Component> lines = new ArrayList<>();
            lines.add(this.getMessage());
            lines.add(Component.translatable("screen.alltheimbaium.supply_crate.cost_points", this.cost));
            if (this.requiresSelection && !hasSelection()) {
                lines.add(Component.translatable("screen.alltheimbaium.supply_crate.need_select"));
            }
            if (SupplyCrateScreen.this.menu.getRemaining() < this.cost) {
                lines.add(Component.translatable("screen.alltheimbaium.supply_crate.need_points"));
            }
            return lines;
        }
    }
}
