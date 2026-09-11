package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * 创造物品质变器的固定配方表。
 * <p>
 * 配方<b>写死在代码里</b>，不是数据包配方——因此不会出现在 JEI / 配方书里，
 * 只能在物品 tooltip 的参数段里列出。新增配方时在 {@link #ENTRIES} 里加一条即可。
 * <p>
 * 每条配方都是"<b>9 个同种材料 → 1 个产物</b>"：输入栏是 3×3 九个格、每格 1 个，
 * 九格填满且为同一种材料时才触发转化，这也是 {@link #INGREDIENT_COUNT} 的含义。
 * <p>
 * 配方用注册名书写并<b>延迟解析</b>：联动模组（Mekanism）未安装时对应条目会被静默跳过，
 * 这样本模组对它们只做可选联动，不需要任何编译期依赖。
 */
public final class TransmuteCatalog {

    /** 输入栏格数（3×3），同时也是每份配方消耗的材料总数 */
    public static final int INPUT_SLOTS = 9;
    /** 输出栏格数 */
    public static final int OUTPUT_SLOTS = 1;
    /** 每份配方消耗的材料个数，与 {@link #INPUT_SLOTS} 一致 */
    public static final int INGREDIENT_COUNT = INPUT_SLOTS;

    /** 配方条目：输入材料的注册名 → 产物注册名 */
    private record Entry(ResourceLocation input, ResourceLocation output) {
    }

    /** 固定配方表，自上而下即 tooltip 中的展示顺序 */
    private static final List<Entry> ENTRIES = List.of(
            new Entry(new ResourceLocation("mekanism", "ultimate_chemical_tank"),
                    new ResourceLocation("mekanism", "creative_chemical_tank"))
    );

    /** 已解析的配方（输入物品 + 产物模板）；首次使用时构建，之后不再重算 */
    private static volatile List<Resolved> resolved;

    private TransmuteCatalog() {
    }

    /** 一条已解析的配方 */
    public record Resolved(@Nonnull Item input, @Nonnull ItemStack output) {
    }

    /**
     * 解析配方表，跳过注册名不存在（未安装对应模组）的条目
     */
    @Nonnull
    private static List<Resolved> resolved() {
        List<Resolved> cache = resolved;
        if (cache == null) {
            List<Resolved> list = new ArrayList<>();
            for (Entry entry : ENTRIES) {
                Item input = BuiltInRegistries.ITEM.getOptional(entry.input()).orElse(Items.AIR);
                Item output = BuiltInRegistries.ITEM.getOptional(entry.output()).orElse(Items.AIR);
                if (input == Items.AIR || output == Items.AIR) {
                    continue;
                }
                list.add(new Resolved(input, new ItemStack(output)));
            }
            cache = List.copyOf(list);
            resolved = cache;
        }
        return cache;
    }

    /**
     * 该物品能否作为输入材料。管道插入与玩家手动放入共用这一判定，
     * 因此无关物品既塞不进输入栏，也不会把机器堵死。
     */
    public static boolean isValidInput(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (Resolved recipe : resolved()) {
            if (stack.is(recipe.input())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 九格输入是否构成一条配方。九格必须全部非空且为同一种材料，
     * 命中则返回一份产物模板（调用方自行 copy），否则返回空。
     */
    @Nonnull
    public static ItemStack match(@Nonnull List<ItemStack> inputs) {
        if (inputs.size() < INPUT_SLOTS) {
            return ItemStack.EMPTY;
        }
        Item first = null;
        for (int i = 0; i < INPUT_SLOTS; i++) {
            ItemStack stack = inputs.get(i);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            if (first == null) {
                first = stack.getItem();
            } else if (first != stack.getItem()) {
                // 九格必须是同一种材料
                return ItemStack.EMPTY;
            }
        }
        for (Resolved recipe : resolved()) {
            if (first == recipe.input()) {
                return recipe.output().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 一条配方摘要：产物 / 材料 / 消耗个数。
     * 物品名取的是运行时显示名，因此跟随语言文件自动翻译。
     */
    public record Summary(@Nonnull ItemStack output, @Nonnull ItemStack input, int count) {
    }

    /**
     * 配方摘要列表，供 GUI 帮助卡按列对齐地绘制
     */
    @Nonnull
    public static List<Summary> summaries() {
        List<Summary> list = new ArrayList<>();
        for (Resolved recipe : resolved()) {
            list.add(new Summary(recipe.output().copy(), new ItemStack(recipe.input()), INGREDIENT_COUNT));
        }
        return list;
    }

    /**
     * tooltip 用的配方行：{@code 产物 ← 材料 ×9}。
     * 颜色码与译文拼在同一串里——{@code §} 的格式状态不跨组件传递（同 {@code Tip.translate}）。
     */
    @Nonnull
    public static List<String> tooltipLines() {
        List<String> lines = new ArrayList<>();
        for (Summary summary : summaries()) {
            lines.add("§f" + summary.output().getHoverName().getString()
                    + " §7←§e " + summary.input().getHoverName().getString()
                    + " §7×" + summary.count());
        }
        return lines;
    }
}
