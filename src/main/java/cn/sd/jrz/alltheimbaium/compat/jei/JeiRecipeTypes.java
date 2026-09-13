package cn.sd.jrz.alltheimbaium.compat.jei;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import mezz.jei.api.recipe.RecipeType;

/**
 * 本模组注册到 JEI 的配方类型。
 */
public final class JeiRecipeTypes {

    /** 资源农场：标记物 → 该资源的全部产物（带速率） */
    public static final RecipeType<MarkerRecipe> RESOURCE_FARM =
            RecipeType.create(Alltheimbaium.MODID, "resource_farm", MarkerRecipe.class);
    /** 生物农场：特征物 + 刷怪蛋 → 该生物的白名单产物 */
    public static final RecipeType<MarkerRecipe> MOB_FARM =
            RecipeType.create(Alltheimbaium.MODID, "mob_farm", MarkerRecipe.class);
    /** 存储方块制造机：只列出判定为可复制的物品（无输入列，按页拆分） */
    public static final RecipeType<MarkerRecipe> STORAGE_FOUNTAIN =
            RecipeType.create(Alltheimbaium.MODID, "storage_fountain", MarkerRecipe.class);
    /** 零刻熔炉：熔炉/高炉/烟熏配方 → 即刻产物 */
    public static final RecipeType<ProcessingRecipe> INSTANT_FURNACE =
            RecipeType.create(Alltheimbaium.MODID, "instant_furnace", ProcessingRecipe.class);
    /** 零刻压印器 · 压板模式：1 份中间原料 → 它支持的全部压板（最多 9 种） */
    public static final RecipeType<ProcessingRecipe> INSCRIBER_PRESS =
            RecipeType.create(Alltheimbaium.MODID, "inscriber_press", ProcessingRecipe.class);
    /** 零刻压印器 · 组装模式：上/中/下三格材料 → 1 个产物 */
    public static final RecipeType<ProcessingRecipe> INSCRIBER_ASSEMBLY =
            RecipeType.create(Alltheimbaium.MODID, "inscriber_assembly", ProcessingRecipe.class);

    private JeiRecipeTypes() {
    }
}
