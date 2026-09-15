package cn.sd.jrz.alltheimbaium.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;

/**
 * 永恒图腾配置界面（客户端）。
 * <p>
 * 顶部 3 行为 27 格药水 / 食物槽，底部为玩家背包与快捷栏。悬停物品显示 tooltip。
 */
public class EternalTotemScreen extends AbstractContainerScreen<EternalTotemMenu> {

    /**
     * 永恒图腾 GUI 背景纹理，176×167
     */
    private static final Identifier CONTAINER_BACKGROUND =
            Identifier.fromNamespaceAndPath("alltheimbaium", "textures/gui/eternal_totem_gui.png");

    public EternalTotemScreen(EternalTotemMenu menu, Inventory playerInventory, Component title) {
        // 26.x：imageWidth/imageHeight 是 final 字段，尺寸经由超类构造器传入
        super(menu, playerInventory, title, 176, 167);
        // 玩家背包采用标准四行布局
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(@Nonnull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 26.x：背景贴图改在 extractBackground 里绘制（renderBg 已删除），先让父类画好暗化/模糊底
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, true);
        guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    // 26.x：AbstractContainerScreen#extractRenderState 已内含 extractTooltip，
    // 旧版为绕开 1.20.1 缺陷而手写的重复调用不再需要
}
