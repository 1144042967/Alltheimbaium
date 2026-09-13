package cn.sd.jrz.alltheimbaium.compat.jei;

import cn.sd.jrz.alltheimbaium.Alltheimbaium;
import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.entity.InstantFurnaceEntity;
import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
import cn.sd.jrz.alltheimbaium.setup.MobFarmCatalog;
import cn.sd.jrz.alltheimbaium.setup.MobFarmWhitelist;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.ResourceData;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JEI 插件：把本模组的机器配方展示出来。
 * <p>
 * 样式参考主流模组的机器配方页：**输入在左、产物在右、中间 JEI 箭头**，卡片底部按需写一行说明。
 * 版面由 {@link JeiLayout} 参数化（具体数值见下面几个常量），两类卡片：
 * <ul>
 *     <li>{@link MarkerRecipeCategory}：标记物 → 产物（资源农场 4×4 / 生物农场 4×4 / 存储方块制造机 8×6 只列物品）</li>
 *     <li>{@link ProcessingRecipeCategory}：输入 → 产物（零刻熔炉竖排 1 输入 1 产物；零刻压印器压板竖排 + 3×3、
 *         组装横排 3 输入 + 1 产物）——都无底部文字</li>
 * </ul>
 * 两类卡片共同的固定规则：**输入格只画实际有的并上下居中**，**产物格始终整片画出并按自然顺序排列**。
 * <p>
 * <b>数据来源与限制</b>：JEI 跑在客户端，而这些机器大多由 serverconfig 白名单驱动。
 * Forge 会把 SERVER 配置同步给客户端（{@code net.minecraftforge.network.ConfigSync}），所以白名单在客户端读得到；
 * 但"击杀掉落采样"（{@code KillLootEstimator}）需要 ServerLevel，客户端拿不到——
 * 因此生物农场这里只展示**白名单产物 + 刷怪蛋兜底**，白名单之外、只有采样结果的生物不会出现在 JEI 里。
 * <p>
 * 所有数据收集都单独 try-catch：任何一处解析失败只让那张卡空着，绝不让 JEI 崩在配方页上。
 */
@JeiPlugin
public class AtiJeiPlugin implements IModPlugin {
    private static final Logger log = LoggerFactory.getLogger(AtiJeiPlugin.class);

    /** 资源农场：1 个标记物 → 4×4 = 16 个产物格（始终全画）；不画底部说明 */
    private static final JeiLayout RESOURCE_FARM_LAYOUT = new JeiLayout(1, 4, 4, false);
    /** 生物农场：特征物 + 刷怪蛋两个输入 → 5×5 = 25 个产物格（始终全画）；底部居中写"收容物：XXX" */
    private static final JeiLayout MOB_FARM_LAYOUT = new JeiLayout(2, 5, 5, true);
    /** 存储方块制造机：没有输入列，只把支持的物品按 8×6 = 48 件一页列出来 */
    private static final JeiLayout STORAGE_FOUNTAIN_LAYOUT = new JeiLayout(0, 8, 6, true);
    /**
     * 零刻熔炉：1 个原料 → 1 个产物，**不要任何文字**。
     * 卡片因此很窄，JEI 会自动把一页排成两栏小卡片。
     */
    private static final JeiLayout INSTANT_FURNACE_LAYOUT = new JeiLayout(1, 1, 1, false);
    /** 零刻压印器 · 压板模式：1 个输入（居中）→ 固定 3×3 = 9 个产物槽位（产物上限就是 9）；无文字 */
    private static final JeiLayout INSCRIBER_PRESS_LAYOUT = new JeiLayout(3, 3, 3, false);
    /** 零刻压印器 · 组装模式：1~3 个输入**横排**（占不满时留空）→ 1 个产物；无文字 */
    private static final JeiLayout INSCRIBER_ASSEMBLY_LAYOUT = new JeiLayout(3, 1, 1, false);
    /** 存储方块制造机的物品列表最多分多少页（防止极端整合包塞进成千上万条配方） */
    private static final int STORAGE_MAX_PAGES = 128;

    @Override
    @Nonnull
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Alltheimbaium.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(@Nonnull IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new MarkerRecipeCategory(guiHelper, JeiRecipeTypes.RESOURCE_FARM,
                        machineName("resource_farm"), itemIcon(Registration.RESOURCE_FARM_ITEM.get()), RESOURCE_FARM_LAYOUT),
                new MarkerRecipeCategory(guiHelper, JeiRecipeTypes.MOB_FARM,
                        machineName("mob_farm"), itemIcon(Registration.MOB_FARM_ITEM.get()), MOB_FARM_LAYOUT),
                new MarkerRecipeCategory(guiHelper, JeiRecipeTypes.STORAGE_FOUNTAIN,
                        machineName("storage_fountain"), itemIcon(Registration.STORAGE_FOUNTAIN_ITEM.get()), STORAGE_FOUNTAIN_LAYOUT),
                new ProcessingRecipeCategory(guiHelper, JeiRecipeTypes.INSTANT_FURNACE,
                        machineName("instant_furnace"), itemIcon(Registration.INSTANT_FURNACE_ITEM.get()),
                        INSTANT_FURNACE_LAYOUT, false),
                new ProcessingRecipeCategory(guiHelper, JeiRecipeTypes.INSCRIBER_PRESS,
                        modeName("inscriber.press"), itemIcon(Registration.INSTANT_INSCRIBER_ITEM.get()),
                        INSCRIBER_PRESS_LAYOUT, false),
                new ProcessingRecipeCategory(guiHelper, JeiRecipeTypes.INSCRIBER_ASSEMBLY,
                        modeName("inscriber.assembly"), itemIcon(Registration.INSTANT_INSCRIBER_ITEM.get()),
                        INSCRIBER_ASSEMBLY_LAYOUT, true));
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {
        registration.addRecipes(JeiRecipeTypes.RESOURCE_FARM, safe(this::resourceFarmRecipes, "资源农场"));
        registration.addRecipes(JeiRecipeTypes.MOB_FARM, safe(this::mobFarmRecipes, "生物农场"));
        registration.addRecipes(JeiRecipeTypes.STORAGE_FOUNTAIN, safe(this::storageFountainRecipes, "存储方块制造机"));
        registration.addRecipes(JeiRecipeTypes.INSTANT_FURNACE, safe(this::instantFurnaceRecipes, "零刻熔炉"));
        registration.addRecipes(JeiRecipeTypes.INSCRIBER_PRESS, safe(this::inscriberPressRecipes, "零刻压印器 压板模式"));
        registration.addRecipes(JeiRecipeTypes.INSCRIBER_ASSEMBLY, safe(this::inscriberAssemblyRecipes, "零刻压印器 组装模式"));
    }

    @Override
    public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration) {
        // 让这些机器出现在对应配方页的"可制作"列表里
        registration.addRecipeCatalyst(itemIcon(Registration.RESOURCE_FARM_ITEM.get()), JeiRecipeTypes.RESOURCE_FARM);
        registration.addRecipeCatalyst(itemIcon(Registration.MOB_FARM_ITEM.get()), JeiRecipeTypes.MOB_FARM);
        registration.addRecipeCatalyst(itemIcon(Registration.STORAGE_FOUNTAIN_ITEM.get()), JeiRecipeTypes.STORAGE_FOUNTAIN);
        registration.addRecipeCatalyst(itemIcon(Registration.INSTANT_FURNACE_ITEM.get()), JeiRecipeTypes.INSTANT_FURNACE);
        registration.addRecipeCatalyst(itemIcon(Registration.INSTANT_INSCRIBER_ITEM.get()),
                JeiRecipeTypes.INSCRIBER_PRESS, JeiRecipeTypes.INSCRIBER_ASSEMBLY);
    }

    // ==================== 数据收集 ====================

    /** 资源农场：白名单里的每个资源 + 自动扫描出的每棵树，都是"标记物 → 产物" */
    @Nonnull
    private List<MarkerRecipe> resourceFarmRecipes() {
        List<MarkerRecipe> out = new ArrayList<>();
        int count = ResourceData.entryCount();
        for (int i = 0; i < count; i++) {
            Item marker = ResourceData.entryMarker(i);
            if (marker == null || marker == Items.AIR) {
                continue;
            }
            List<MarkerRecipe.MarkerProduct> products = new ArrayList<>();
            for (ResourceData.Product product : ResourceData.entryProducts(i)) {
                if (product.item() == null || product.item() == Items.AIR) {
                    continue;
                }
                products.add(new MarkerRecipe.MarkerProduct(new ItemStack(product.item()),
                        Component.translatable("jei.alltheimbaium.rate",
                                product.weight(), rateAtLevel1(product.weight()))));
            }
            if (products.isEmpty()) {
                continue;
            }
            warnIfTooMany("资源农场", BuiltInRegistries.ITEM.getKey(marker).toString(),
                    products.size(), RESOURCE_FARM_LAYOUT.maxShown());
            // 这一档没有底部说明：省下的高度给了第 4 行产物
            out.add(new MarkerRecipe(List.of(new ItemStack(marker)), products, List.of()));
        }
        return out;
    }

    /**
     * 生物农场：特征物 + 刷怪蛋 → 该生物的白名单产物（另附刷怪蛋兜底）。
     * <p>
     * 白名单之外、只有击杀采样结果的生物不在这里——采样需要 ServerLevel，客户端拿不到。
     */
    @Nonnull
    private List<MarkerRecipe> mobFarmRecipes() {
        List<MarkerRecipe> out = new ArrayList<>();
        for (Map.Entry<Item, EntityType<?>> entry : MobFarmWhitelist.signatureMarkers().entrySet()) {
            Item marker = entry.getKey();
            EntityType<?> type = entry.getValue();
            if (marker == null || marker == Items.AIR || type == null) {
                continue;
            }
            List<MarkerRecipe.MarkerProduct> products = new ArrayList<>();
            for (Item product : MobFarmWhitelist.productItemsFor(type)) {
                if (product != null && product != Items.AIR) {
                    products.add(new MarkerRecipe.MarkerProduct(new ItemStack(product), null));
                }
            }
            // 刷怪蛋兜底：白名单没有产物时，机器会按权重 1 产出该生物的刷怪蛋
            Item egg = MobFarmCatalog.spawnEggOf(type);
            if (egg != null && egg != Items.AIR && products.stream().noneMatch(p -> p.stack().getItem() == egg)) {
                products.add(new MarkerRecipe.MarkerProduct(new ItemStack(egg), null));
            }
            if (products.isEmpty()) {
                continue;
            }
            String mobName = Component.translatable(type.getDescriptionId()).getString();
            warnIfTooMany("生物农场", mobName, products.size(), MOB_FARM_LAYOUT.maxShown());
            // 输入两格：上面是特征物（必定收容），下面是刷怪蛋（有蛋就能尝试收容）
            List<ItemStack> inputs = new ArrayList<>(2);
            inputs.add(new ItemStack(marker));
            inputs.add(egg == null || egg == Items.AIR ? ItemStack.EMPTY : new ItemStack(egg));
            List<Component> notes = List.of(Component.translatable("jei.alltheimbaium.mob_farm.line", mobName));
            out.add(new MarkerRecipe(inputs, products, notes));
        }
        // 按标记物注册名排序，保证 JEI 里的先后顺序稳定
        out.sort(Comparator.comparing(recipe -> BuiltInRegistries.ITEM.getKey(recipe.inputs().get(0).getItem()).toString()));
        return out;
    }

    /**
     * 存储方块制造机：把配置判定为"可复制"的物品按 4×4 分页列出来（没有输入列）。
     * <p>
     * 接受范围由 {@code StorageFountainBlock.isAcceptedItem} 判定（白名单 / 命名空间 / 标签子串），
     * 在大型整合包里可能是上千件，所以按页拆分，每页一条 JEI 配方。
     */
    @Nonnull
    private List<MarkerRecipe> storageFountainRecipes() {
        List<Item> accepted = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            try {
                if (StorageFountainBlock.isAcceptedItem(new ItemStack(item))) {
                    accepted.add(item);
                }
            } catch (Throwable e) {
                log.warn("JEI：判定 {} 是否可复制时出错，已跳过", item, e);
            }
        }
        accepted.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        if (accepted.isEmpty()) {
            log.warn("JEI：存储方块制造机没有解析到可复制的物品（白名单配置是否已同步到客户端？）");
            return List.of();
        }
        int perPage = STORAGE_FOUNTAIN_LAYOUT.maxShown();
        int pages = (accepted.size() + perPage - 1) / perPage;
        if (pages > STORAGE_MAX_PAGES) {
            log.warn("JEI：存储方块制造机可复制的物品 {} 件，超过 {} 页上限，只展示前 {} 件",
                    accepted.size(), STORAGE_MAX_PAGES, STORAGE_MAX_PAGES * perPage);
            pages = STORAGE_MAX_PAGES;
        }
        List<MarkerRecipe> out = new ArrayList<>(pages);
        for (int page = 0; page < pages; page++) {
            List<MarkerRecipe.MarkerProduct> products = new ArrayList<>(perPage);
            for (int i = page * perPage; i < Math.min(accepted.size(), (page + 1) * perPage); i++) {
                products.add(new MarkerRecipe.MarkerProduct(new ItemStack(accepted.get(i)), null));
            }
            List<Component> notes = List.of(
                    Component.translatable("jei.alltheimbaium.storage_fountain.total", accepted.size()),
                    Component.translatable("jei.alltheimbaium.storage_fountain.page", page + 1, pages));
            out.add(new MarkerRecipe(List.of(), products, notes));
        }
        return out;
    }

    /**
     * 零刻熔炉：熔炉 / 高炉 / 烟熏三种配方都能即刻完成。
     * <p>
     * 与机器一致地按 熔炉 → 高炉 → 烟熏 的优先级**按输入去重**，同一种原料只显示最先命中的那条。
     */
    @Nonnull
    private List<ProcessingRecipe> instantFurnaceRecipes() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        RecipeManager manager = level.getRecipeManager();
        List<ProcessingRecipe> out = new ArrayList<>();
        Set<Item> seenInputs = new HashSet<>();
        // 这一档不留任何文字：卡片只画"原料 → 产物"，耗能在机器 GUI 与物品 tooltip 里有
        List<Component> notes = List.of();
        for (RecipeType<? extends AbstractCookingRecipe> type : InstantFurnaceEntity.FURNACE_TYPES) {
            Collection<? extends AbstractCookingRecipe> recipes = manager.getAllRecipesFor(type);
            for (AbstractCookingRecipe recipe : recipes) {
                ItemStack result = recipe.getResultItem(level.registryAccess());
                if (result.isEmpty()) {
                    continue;
                }
                for (Ingredient ingredient : recipe.getIngredients()) {
                    if (ingredient.isEmpty()) {
                        continue;
                    }
                    ItemStack[] candidates = ingredient.getItems();
                    if (candidates.length == 0 || candidates[0].isEmpty()) {
                        continue;
                    }
                    if (!seenInputs.add(candidates[0].getItem())) {
                        continue;
                    }
                    out.add(new ProcessingRecipe(List.of(candidates[0].copy()), List.of(result.copy()), notes));
                }
            }
        }
        return out;
    }

    /**
     * 零刻压印器 · 压板模式：1 份中间原料 → 它支持的全部压板（一个独立类别，只列压板配方）。
     */
    @Nonnull
    private List<ProcessingRecipe> inscriberPressRecipes() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        List<ProcessingRecipe> out = new ArrayList<>();
        for (InstantInscriberEntity.PressSummary summary : InstantInscriberEntity.inscribeSummaries(level)) {
            // 压板模式只消耗中间那格原料：这里只给这一个输入，分类会把它上下居中
            if (summary.inputs().isEmpty()) {
                continue;
            }
            out.add(new ProcessingRecipe(List.of(summary.inputs().get(0)), summary.outputs(), List.of()));
        }
        return out;
    }

    /**
     * 零刻压印器 · 组装模式：上/中/下三格材料 → 1 个产物（一个独立类别，只列组装配方）。
     */
    @Nonnull
    private List<ProcessingRecipe> inscriberAssemblyRecipes() {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return List.of();
        }
        List<ProcessingRecipe> out = new ArrayList<>();
        for (InstantInscriberEntity.RecipeSummary summary : InstantInscriberEntity.assemblySummaries(level)) {
            out.add(new ProcessingRecipe(new ArrayList<>(summary.inputs()), List.of(summary.output()), List.of()));
        }
        return out;
    }

    // ==================== 小工具 ====================

    /** 产物超过卡片能显示的数量时记一条日志（卡片本身不提示，避免误导"还有更多能产出"） */
    private static void warnIfTooMany(@Nonnull String machine, @Nonnull String what, int total, int shown) {
        if (total > shown) {
            log.info("JEI：{} 的「{}」有 {} 项产物，超过卡片槽位数 {}，多出的不会显示在 JEI 里（完整清单见机器 GUI 的 ? 卡片）",
                    machine, what, total, shown);
        }
    }

    /** 收集失败只让该分类空着，不让 JEI 的配方装载整体崩掉 */
    @Nonnull
    private <T> List<T> safe(@Nonnull java.util.function.Supplier<List<T>> supplier, @Nonnull String what) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            log.warn("JEI：{} 配方收集失败，该分类将为空", what, e);
            return List.of();
        }
    }

    /** 权重折算成 Lv1 的产出速率（权重 500 ≈ 1 件/秒） */
    @Nonnull
    private static String rateAtLevel1(long weight) {
        double perSecond = weight / 500.0;
        return perSecond >= 10.0 ? String.format("%.0f", perSecond) : String.format("%.2f", perSecond);
    }

    @Nonnull
    private static Component machineName(@Nonnull String name) {
        return Component.translatable("block.alltheimbaium." + name);
    }

    /** 压印器两个模式各自的类别标题 */
    @Nonnull
    private static Component modeName(@Nonnull String suffix) {
        return Component.translatable("jei.alltheimbaium." + suffix);
    }

    @Nonnull
    private static ItemStack itemIcon(@Nonnull Item item) {
        return new ItemStack(item);
    }
}
