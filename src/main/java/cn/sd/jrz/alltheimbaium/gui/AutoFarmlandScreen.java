package cn.sd.jrz.alltheimbaium.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

/**
 * ATI 自动耕地 GUI（原版普通箱子界面，27 槽）。
 */
@OnlyIn(Dist.CLIENT)
public class AutoFarmlandScreen extends AbstractContainerScreen<AutoFarmlandMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("alltheimbaium", "textures/gui/auto_farmland_gui.png");

    public AutoFarmlandScreen(AutoFarmlandMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        // 物品栏标签放在容器下方（与箱子界面一致）
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderBackground(guiGraphics);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 176, 166);
        // 能量条（靠右、较窄避开机器名、加高包裹电量文字、带边框；能量值居中，白色带阴影）
        int barX = this.leftPos + 110;
        int barY = this.topPos + 5;
        int barW = 58;
        int barH = 10;
        // 边框
        guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF000000);
        // 背景
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF333333);
        int stored = this.menu.getEnergyStored();
        int capacity = this.menu.getEnergyCapacity();
        if (capacity > 0 && stored > 0) {
            int fill = barW * stored / capacity;
            guiGraphics.fill(barX, barY, barX + fill, barY + barH, 0xFFE03000);
        }
        // 能量值文字（居中，白色带阴影）
        String energyText = formatEnergy(stored) + " FE";
        guiGraphics.drawString(this.font, energyText, barX + (barW - this.font.width(energyText)) / 2, barY + (barH - 8) / 2, 0xFFFFFFFF, true);
    }

    @Override
    protected void renderLabels(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        // 渲染鼠标悬浮物品的信息提示窗
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    /**
     * 能量值缩写（整数 K/M，缩短文字以适配较窄能量条）：&lt;1000 用数字，&lt;1000000 用整数 K，否则用整数 M
     */
    private static String formatEnergy(int fe) {
        if (fe < 1000) {
            return String.valueOf(fe);
        }
        if (fe < 1_000_000) {
            return (fe + 500) / 1000 + "K";
        }
        return (fe + 500_000) / 1_000_000 + "M";
    }
}
