package cn.sd.jrz.alltheimbaium.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 永恒图腾配置界面（客户端）。
 * <p>
 * 顶部 3 行为 27 格药水槽，中间为输入（终极化学品储罐）/ 输出（创造化学品储罐）槽位，
 * 底部为玩家背包与快捷栏。悬停物品显示 tooltip。
 */
@OnlyIn(Dist.CLIENT)
public class EternalTotemScreen extends AbstractContainerScreen<EternalTotemMenu> {

    /**
     * 永恒图腾 GUI 背景纹理，176×222
     */
    private static final ResourceLocation CONTAINER_BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("alltheimbaium", "textures/gui/eternal_totem_gui.png");

    public EternalTotemScreen(EternalTotemMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 192;
        this.inventoryLabelY = 95;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(CONTAINER_BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        // 输入/输出槽位说明：同一行，靠近各自槽位（输入槽在左、输出槽在右，文字在中间两侧）
        Component input = Component.translatable("screen.alltheimbaium.eternal_totem.input");
        Component output = Component.translatable("screen.alltheimbaium.eternal_totem.output");
        guiGraphics.drawString(this.font, input, 26, 80, 0x404040, false);
        guiGraphics.drawString(this.font, output, 89, 80, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 1.20.1 的 AbstractContainerScreen.render 不会主动调用 renderTooltip，这里显式渲染物品 tooltip
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
