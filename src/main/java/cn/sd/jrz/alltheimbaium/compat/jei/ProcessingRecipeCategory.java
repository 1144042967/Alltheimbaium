package cn.sd.jrz.alltheimbaium.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * "输入 → 产物 + 耗能" 类配方在 JEI 里的统一样式（零刻熔炉 / 零刻压印器共用）。
 * <p>
 * 布局由 {@link JeiLayout} 决定：左侧一列输入格（可为 0 列）、中间 JEI 箭头、右侧整片产物网格。
 * 两条固定规则：
 * <ul>
 *     <li><b>输入格只画实际有的</b>——压印器的输入可能是 1/2/3 个，按数量在卡片高度里**上下居中**。</li>
 *     <li><b>产物格始终整片画出</b>：{@code cols × rows} 个槽位全部建出来，有产物的填物品、没产物的留空槽背景，
 *         按自然顺序排列。零刻压印器因此固定显示 3×3 槽位（它的产物上限就是 9，超出的机器也产不出来）。</li>
 * </ul>
 * 零刻熔炉只有 1 格原料 → 1 个产物、且不留说明文字，所以卡片很窄，JEI 会自动在一页里并排显示成两栏小卡片。
 * <p>
 * 26.x 渲染迁移与 {@link MarkerRecipeCategory} 相同：{@code GuiGraphics} 换成 {@code GuiGraphicsExtractor}、
 * {@code getBackground()} 已删除（卡片范围改由 getWidth/getHeight 决定），说明文字仍在 draw() 里自己画。
 */
public class ProcessingRecipeCategory implements IRecipeCategory<ProcessingRecipe> {
    private static final Logger log = LoggerFactory.getLogger(ProcessingRecipeCategory.class);
    /** 说明文字颜色：JEI 卡片底衬偏亮，用原版深灰（26.x 必须是带 alpha 的 ARGB） */
    private static final int NOTE_COLOR = 0xFF404040;

    private final IRecipeType<ProcessingRecipe> recipeType;
    private final Component title;
    private final IDrawable icon;
    private final JeiLayout layout;
    /** 输入格是否横排（false = 竖着一列）。横排时卡片更矮，一页能放下更多配方 */
    private final boolean horizontalInputs;

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

    /**
     * @param horizontalInputs 输入格是否横排：竖排适合"1 个原料 → 1 个产物"（熔炉），
     *                         横排适合"多个原料 → 少量产物"（压印器组装模式，横排后卡片明显变矮，一页能多放配方）
     */
    public ProcessingRecipeCategory(@Nonnull IGuiHelper guiHelper, @Nonnull IRecipeType<ProcessingRecipe> recipeType,
                                    @Nonnull Component title, @Nonnull ItemStack icon, @Nonnull JeiLayout layout,
                                    boolean horizontalInputs) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableItemStack(icon);
        this.layout = layout;
        this.horizontalInputs = horizontalInputs;
        int inputLen = layout.inputSlots() * JeiLayout.SLOT;                 // 输入列/行的总长度
        this.bodyH = Math.max(horizontalInputs ? JeiLayout.SLOT : inputLen, layout.rows() * JeiLayout.SLOT);
        this.arrowX = inputX + (horizontalInputs ? inputLen : JeiLayout.SLOT) + 6;
        this.arrowY = gridY + (bodyH - JeiLayout.ARROW_H) / 2;
        this.gridX = arrowX + JeiLayout.ARROW_W + 6;
        this.gridStartY = gridY + (bodyH - layout.rows() * JeiLayout.SLOT) / 2;
        this.noteY = gridY + bodyH + 4;
        this.width = gridX + layout.cols() * JeiLayout.SLOT + JeiLayout.PAD;
        this.height = noteY + (layout.hasNote() ? JeiLayout.NOTE_LINES * JeiLayout.LINE_H : 0) + JeiLayout.PAD;
    }

    @Override
    @Nonnull
    public IRecipeType<ProcessingRecipe> getRecipeType() {
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

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setRecipe(@Nonnull IRecipeLayoutBuilder builder, @Nonnull ProcessingRecipe recipe, @Nonnull IFocusGroup focuses) {
        List<ItemStack> inputs = new ArrayList<>();
        for (ItemStack stack : recipe.inputs()) {
            if (stack != null && !stack.isEmpty()) {
                inputs.add(stack);
            }
        }
        if (horizontalInputs) {
            // 横排：整行在卡片高度里居中，槽位从左往右排（占不满时右边留空，箭头位置固定不动）
            int rowY = gridY + (bodyH - JeiLayout.SLOT) / 2;
            for (int i = 0; i < inputs.size(); i++) {
                builder.addInputSlot(inputX + i * JeiLayout.SLOT, rowY)
                        .add(inputs.get(i))
                        .setStandardSlotBackground();
            }
        } else {
            // 竖排：整列上下居中（1 个输入时就落在正中间）
            int startY = gridY + (bodyH - inputs.size() * JeiLayout.SLOT) / 2;
            for (int i = 0; i < inputs.size(); i++) {
                builder.addInputSlot(inputX, startY + i * JeiLayout.SLOT)
                        .add(inputs.get(i))
                        .setStandardSlotBackground();
            }
        }
        // 产物：整片网格都建出来（空槽 JEI 也只画背景），按自然顺序排列
        List<ItemStack> outputs = recipe.outputs();
        int total = layout.maxShown();
        for (int i = 0; i < total; i++) {
            // 产物格用标准槽背景（18×18）：JEI 的 setOutputSlotBackground() 贴图是 26×26，与 18px 网格间距不兼容
            var slot = builder
                    .addOutputSlot(gridX + (i % layout.cols()) * JeiLayout.SLOT,
                            gridStartY + (i / layout.cols()) * JeiLayout.SLOT)
                    .setStandardSlotBackground();
            if (i < outputs.size()) {
                slot.add(outputs.get(i));
            }
        }
    }

    @Override
    public void createRecipeExtras(@Nonnull IRecipeExtrasBuilder extras, @Nonnull ProcessingRecipe recipe, @Nonnull IFocusGroup focuses) {
        // 这里只放箭头：说明文字见 draw()，原因见类注释（addText 的语义在不同 JEI 小版本里是反的）。
        try {
            extras.addRecipeArrow().setPosition(arrowX, arrowY);
        } catch (Throwable e) {
            log.warn("JEI：{} 的配方箭头绘制失败", recipeType.getUid(), e);
        }
    }

    @Override
    public void draw(@Nonnull ProcessingRecipe recipe, @Nonnull IRecipeSlotsView slotsView,
                     @Nonnull GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
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
                guiGraphics.text(font, lines.get(i), JeiLayout.PAD, noteY + i * JeiLayout.LINE_H, NOTE_COLOR, false);
            }
        } catch (Throwable e) {
            log.warn("JEI：{} 的配方说明文字绘制失败", recipeType.getUid(), e);
        }
    }
}
