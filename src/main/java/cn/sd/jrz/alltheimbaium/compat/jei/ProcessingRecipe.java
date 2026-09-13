package cn.sd.jrz.alltheimbaium.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * "输入 → 产物" 的 JEI 展示配方（零刻熔炉 / 零刻压印器共用一套样式）。
 * <p>
 * {@code inputs} 按**槽位下标**给出：空栈表示该位置不画槽——零刻压印器的压板模式只吃中间那格原料，
 * 就把上/下两格留空，界面上一眼能看出"只有中间被消耗"。
 * <p>
 * 耗能写在 {@code noteLines} 里（如"压板模式，耗能 1000 FE"），由注册时拼好；每个元素占一行。
 */
public record ProcessingRecipe(@Nonnull List<ItemStack> inputs, @Nonnull List<ItemStack> outputs,
                               @Nonnull List<Component> noteLines) {
}
