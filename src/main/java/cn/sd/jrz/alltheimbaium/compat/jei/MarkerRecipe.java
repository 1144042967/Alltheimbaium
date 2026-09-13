package cn.sd.jrz.alltheimbaium.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * "标记物 → 产物" 的 JEI 展示配方（资源农场 / 生物农场 / 存储方块制造机共用一套样式）。
 * <p>
 * {@code inputs} 按**槽位顺序**给出左侧输入列（空栈表示该位不画槽）：
 * 资源农场是 1 个标记物；生物农场是"特征物 + 刷怪蛋"两个；存储方块制造机没有输入列（只展示支持的物品）。
 * {@code noteLines} 是卡片底部的说明文字，**每个元素占一行**（生物农场用它写"生物名 共 N 项产出"，
 * 存储方块制造机用它写页码）。列表为空即不画说明——每个元素一行，不依赖换行符的拆分行为。
 */
public record MarkerRecipe(@Nonnull List<ItemStack> inputs, @Nonnull List<MarkerProduct> products,
                           @Nonnull List<Component> noteLines) {

    /** 单个产物：物品 + 悬浮说明（资源农场用它显示权重折算出的速率） */
    public record MarkerProduct(@Nonnull ItemStack stack, @Nullable Component tooltip) {
    }
}
