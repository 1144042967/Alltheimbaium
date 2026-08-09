package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.item.EternalSwordItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * 永恒之剑配置界面（客户端）。
 * <p>
 * 使用类箱子的背景纹理：
 * - 顶部 3 行：27 格剑槽
 * - 中间：模式切换与攻击距离按钮（下移，避免遮挡物品槽位）
 * - 底部：玩家背包与快捷栏
 * 按钮点击通过 handleInventoryButtonClick 发送给服务端菜单处理。
 */
@OnlyIn(Dist.CLIENT)
public class EternalSwordScreen extends AbstractContainerScreen<EternalSwordMenu> {

    /**
     * 原版箱子（54 格）背景纹理，176×222
     */
    private static final ResourceLocation CONTAINER_BACKGROUND = ResourceLocation.tryBuild("alltheimbaium", "textures/gui/eternal_sword_gui.png");

    private Button modeButton;
    private final Button[] rangeButtons = new Button[EternalSwordItem.RANGES.length];

    public EternalSwordScreen(EternalSwordMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 125;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        // 模式切换按钮：0=敌对生物，1=所有生物（位于剑槽 3 行与玩家背包之间）
        this.modeButton = this.addRenderableWidget(
                Button.builder(Component.literal(""), b -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0))
                        .bounds(x + 8, y + 76, 160, 20)
                        .build());
        // 攻击距离按钮：4 个
        for (int i = 0; i < EternalSwordItem.RANGES.length; i++) {
            final int buttonId = i + 1;
            this.rangeButtons[i] = this.addRenderableWidget(Button.builder(Component.literal(String.valueOf(EternalSwordItem.RANGES[i])), b -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId))
                    .bounds(x + 8 + i * 41, y + 100, 38, 20)
                    .build());
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // 刷新模式按钮文字
        this.modeButton.setMessage(Component.translatable(this.menu.getKillAll() == 1
                ? "screen.alltheimbaium.eternal_sword.mode_all"
                : "screen.alltheimbaium.eternal_sword.mode_hostile"));
        // 高亮当前选中的距离
        int currentRange = this.menu.getRange();
        for (int i = 0; i < this.rangeButtons.length; i++) {
            ChatFormatting color = EternalSwordItem.RANGES[i] == currentRange
                    ? ChatFormatting.AQUA : ChatFormatting.WHITE;
            this.rangeButtons[i].setMessage(Component.literal(
                    String.valueOf(EternalSwordItem.RANGES[i])).withStyle(color));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 箱子背景纹理（自带槽位底与边框）
        guiGraphics.blit(CONTAINER_BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 1.20.1 的 AbstractContainerScreen.render 不会主动调用 renderTooltip，
        // 这里显式渲染物品 tooltip（super.render 已设置 hoveredSlot）
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // 剑槽物品：标准物品 tooltip + 追加该物品对剑伤害的贡献
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()
                && this.hoveredSlot.index < EternalSwordItem.INVENTORY_SIZE) {
            ItemStack stack = this.hoveredSlot.getItem();
            List<Component> lines = new ArrayList<>(this.getTooltipFromContainerItem(stack));
            float contribution = EternalSwordItem.getDamageContribution(stack);
            if (contribution > 0F) {
                lines.add(Component.translatable("screen.alltheimbaium.eternal_sword.slot_damage", contribution));
            }
            guiGraphics.renderTooltip(this.font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
        } else {
            // 其它槽位：标准物品 tooltip
            super.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }
}
