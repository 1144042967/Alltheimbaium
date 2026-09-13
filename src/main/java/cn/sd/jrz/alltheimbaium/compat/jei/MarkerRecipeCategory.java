package cn.sd.jrz.alltheimbaium.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * "标记物 → 产物" 类配方在 JEI 里的统一样式（资源农场 / 生物农场 / 存储方块制造机共用）。
 * <p>
 * 布局由 {@link JeiLayout} 决定：左侧一列输入格（可为 0 列）、中间 JEI 箭头、右侧整片产物网格，
 * 底部按需留说明文字。两条固定规则：
 * <ul>
 *     <li><b>输入格只画实际有的</b>，并按数量在卡片高度里**上下居中**（1 个就居中那一个，2 个就居中这两个）。</li>
 *     <li><b>产物格始终整片画出</b>：{@code cols × rows} 个槽位全部建出来，有产物的填物品、没产物的留空槽背景，
 *         按自然顺序（从左到右、从上到下）排列，不做居中也不做额外文字描述。</li>
 * </ul>
 * <p>
 * <b>三个跨版本坑</b>（详见 CLAUDE.md 的 JEI 小节）：必须显式给出 {@code getBackground()}；
 * 说明文字不能用 {@code extras.addText}（参数含义在 15.20 / 15.59 之间是反的）；
 * 产物格不能用 {@code setOutputSlotBackground}（那是 26×26 的贴图，与 18px 网格不兼容）。
 */
public class MarkerRecipeCategory implements IRecipeCategory<MarkerRecipe> {
    private static final Logger log = LoggerFactory.getLogger(MarkerRecipeCategory.class);
    /** 说明文字颜色：JEI 卡片底衬偏亮，用原版深灰 */
    private static final int NOTE_COLOR = 0xFF404040;

    private final RecipeType<MarkerRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final IDrawable background;
    private final JeiLayout layout;

    private final int bodyH;
    private final int inputX = JeiLayout.PAD;
    private final int arrowX;
    private final int arrowY;
    private final int gridX;
    private final int gridY = JeiLayout.PAD;
    /** 产物网格整体在卡片高度里居中时的实际起始 y（网格比输入列矮时才有偏移） */
    private final int gridStartY;
    private final int noteY;
    private final int width;
    private final int height;

    public MarkerRecipeCategory(@Nonnull IGuiHelper guiHelper, @Nonnull RecipeType<MarkerRecipe> recipeType,
                                @Nonnull Component title, @Nonnull ItemStack icon, @Nonnull JeiLayout layout) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(icon);
        this.layout = layout;
        this.bodyH = Math.max(layout.inputSlots() * JeiLayout.SLOT, layout.rows() * JeiLayout.SLOT);
        this.arrowX = inputX + JeiLayout.SLOT + 6;
        this.arrowY = gridY + (bodyH - JeiLayout.ARROW_H) / 2;
        // 没有输入列时（存储方块制造机）产物网格直接贴左边，不留箭头位置
        this.gridX = layout.inputSlots() == 0
                ? JeiLayout.PAD
                : inputX + JeiLayout.SLOT + 6 + JeiLayout.ARROW_W + 6;
        this.gridStartY = gridY + (bodyH - layout.rows() * JeiLayout.SLOT) / 2;
        this.noteY = gridY + bodyH + 4;
        this.width = gridX + layout.cols() * JeiLayout.SLOT + JeiLayout.PAD;
        this.height = noteY + (layout.hasNote() ? JeiLayout.NOTE_LINES * JeiLayout.LINE_H : 0) + JeiLayout.PAD;
        this.background = guiHelper.createBlankDrawable(width, height);
    }

    @Override
    @Nonnull
    public RecipeType<MarkerRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    @Nonnull
    public Component getTitle() {
        return title;
    }

    @Override
    @Nonnull
    public IDrawable getIcon() {
        return icon;
    }

    /**
     * 显式给出卡片区域（空白 drawable，尺寸即布局尺寸）。
     * <p>
     * 该方法在 JEI 15.20 起被标记为待删除，但**老版本仍然依赖它来确定配方卡片的范围**：
     * 不覆写时不同小版本画出来的底衬与实际布局不一致（实测 15.20 会把产物格画到底衬外面）。
     */
    @Override
    @Nonnull
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayoutBuilder builder, @Nonnull MarkerRecipe recipe, @Nonnull IFocusGroup focuses) {
        if (layout.inputSlots() > 0) {
            List<ItemStack> inputs = new ArrayList<>();
            for (ItemStack stack : recipe.inputs()) {
                if (stack != null && !stack.isEmpty()) {
                    inputs.add(stack);
                }
            }
            int startY = gridY + (bodyH - inputs.size() * JeiLayout.SLOT) / 2;   // 输入格整组上下居中
            for (int i = 0; i < inputs.size(); i++) {
                builder.addInputSlot(inputX, startY + i * JeiLayout.SLOT)
                        .addItemStack(inputs.get(i))
                        .setStandardSlotBackground();
            }
        }
        // 产物：整片网格都建出来（空槽 JEI 也只画背景），按自然顺序排列
        List<MarkerRecipe.MarkerProduct> products = recipe.products();
        int total = layout.maxShown();
        for (int i = 0; i < total; i++) {
            IRecipeSlotBuilder slot = builder
                    .addOutputSlot(gridX + (i % layout.cols()) * JeiLayout.SLOT,
                            gridStartY + (i / layout.cols()) * JeiLayout.SLOT)
                    .setStandardSlotBackground();
            if (i < products.size()) {
                MarkerRecipe.MarkerProduct product = products.get(i);
                slot.addItemStack(product.stack());
                Component tooltip = product.tooltip();
                if (tooltip != null) {
                    slot.addRichTooltipCallback((view, tip) -> tip.add(tooltip));
                }
            }
        }
    }

    @Override
    public void createRecipeExtras(@Nonnull IRecipeExtrasBuilder extras, @Nonnull MarkerRecipe recipe, @Nonnull IFocusGroup focuses) {
        // 这里只放箭头：说明文字见 draw()，原因见类注释（addText 的语义在不同 JEI 小版本里是反的）。
        if (layout.inputSlots() == 0) {
            return;   // 没有输入列就没有箭头
        }
        try {
            extras.addRecipeArrow().setPosition(arrowX, arrowY);
        } catch (Throwable e) {
            log.warn("JEI：{} 的配方箭头绘制失败", recipeType.getUid(), e);
        }
    }

    @Override
    public void draw(@Nonnull MarkerRecipe recipe, @Nonnull IRecipeSlotsView slotsView, @Nonnull GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        if (!layout.hasNote()) {
            return;
        }
        try {
            Font font = Minecraft.getInstance().font;
            if (font == null) {
                return;
            }
            List<Component> lines = new ArrayList<>();
            for (Component line : recipe.noteLines()) {
                if (lines.size() >= JeiLayout.NOTE_LINES) {
                    break;
                }
                lines.addAll(JeiText.wrap(line, width - JeiLayout.PAD * 2, JeiLayout.NOTE_LINES - lines.size()));
            }
            for (int i = 0; i < lines.size(); i++) {
                // 说明文字整行居中
                int x = (width - font.width(lines.get(i))) / 2;
                guiGraphics.drawString(font, lines.get(i), Math.max(JeiLayout.PAD, x),
                        noteY + i * JeiLayout.LINE_H, NOTE_COLOR, false);
            }
        } catch (Throwable e) {
            log.warn("JEI：{} 的配方说明文字绘制失败", recipeType.getUid(), e);
        }
    }
}
