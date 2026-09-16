package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * 配方访问的统一入口：抹平"服务端有完整配方表、客户端只有同步子集"这一差异。
 * <p>
 * <b>为什么需要它</b>：26.x 起客户端不再同步完整配方表——{@code ClientLevel.recipeAccess()}
 * 只返回 {@code ClientRecipeContainer}（配方属性集 + 切石机配方），既没有烧炼配方也没有 AE2 压印配方。
 * 若各处直接判 {@code recipeAccess() instanceof RecipeManager}，客户端会一律拿空表，
 * 表现为"零刻熔炉的 JEI 分类空的""压印器的 ? 帮助卡显示未安装 AE2"。
 * <p>
 * NeoForge 为此提供了官方的子集同步机制，两端各出一半：
 * <ul>
 *     <li>服务端在 {@code OnDatapackSyncEvent} 里用 {@code sendRecipes(...)} 声明要同步哪些配方类型
 *         （见 {@link RecipeSync}）；</li>
 *     <li>客户端在 {@code RecipesReceivedEvent} 里收到这些类型的完整配方（{@link RecipeMap}），
 *         存进本类（见 {@code ClientHandler}）。</li>
 * </ul>
 * 于是取配方的代码只需要认这一处：服务端走 {@link RecipeManager}，客户端走同步下来的缓存。
 * <p>
 * <b>时序上是安全的</b>：JEI 在 {@code RecipesReceivedEvent}（EventPriority.LOWEST）之后才启动配方装载，
 * 本模组的监听器用的是默认优先级，必然先于 JEI 填好缓存。
 */
public final class RecipeSource {
    private static final Logger log = LoggerFactory.getLogger(RecipeSource.class);

    /** AE2 压印机配方类型：现行 id + 1.20 之前的旧 id */
    private static final String[] INSCRIBER_TYPE_IDS = {"ae2:inscriber", "appliedenergistics2:inscriber"};

    /**
     * 客户端配方缓存：由 {@code RecipesReceivedEvent} 填充、登出时清空。
     * <p>
     * 只在客户端读写；集成服务器下客户端线程写、服务端分支不读它（服务端有自己的 RecipeManager）。
     */
    @Nullable
    private static volatile RecipeMap clientRecipes;

    private RecipeSource() {
    }

    /** 客户端收到服务端同步的配方子集（{@code RecipesReceivedEvent}） */
    public static void setClientRecipes(@Nullable RecipeMap recipes) {
        clientRecipes = recipes;
    }

    /**
     * 客户端是否已拿到服务端同步的配方子集。
     * <p>
     * 供客户端侧做"结果缓存"的调用方判定：同步还没到达时先别缓存空结果，
     * 否则那一份空数据会在整个会话里一直沿用下去。
     */
    public static boolean clientRecipesReady() {
        return clientRecipes != null;
    }

    /**
     * 当前配方表的版本标识，用于判定"配方是否变了、要不要重建缓存"。
     * <p>
     * 服务端给 {@link RecipeManager} 实例，客户端给同步下来的 {@link RecipeMap} 实例——
     * 两者都在配方重载/重新同步时换成新对象。返回 null 表示当前取不到任何配方。
     */
    @Nullable
    public static Object stamp(@Nonnull Level level) {
        if (level.isClientSide()) {
            return clientRecipes;
        }
        return level.recipeAccess() instanceof RecipeManager manager ? manager : null;
    }

    /**
     * 当前可用的全部配方（服务端全量、客户端为服务端同步来的子集）。
     * <p>
     * 取不到时返回空表而不是 null：调用方普遍是"遍历筛选"，空表天然安全。
     */
    @Nonnull
    public static Collection<RecipeHolder<?>> all(@Nonnull Level level) {
        if (level.isClientSide()) {
            RecipeMap recipes = clientRecipes;
            return recipes == null ? List.of() : recipes.values();
        }
        return level.recipeAccess() instanceof RecipeManager manager ? manager.getRecipes() : List.of();
    }

    /**
     * 查找 AE2 压印机配方类型；未装 AE2 返回 null。
     * <p>
     * 只查注册名，不做编译期依赖——AE2 对本模组始终是可选联动。
     *
     * @param level 可空；给定时代码还会去该 level 的注册表里兜底查一次
     */
    @Nullable
    public static RecipeType<?> findAe2InscriberType(@Nullable Level level) {
        for (String id : INSCRIBER_TYPE_IDS) {
            try {
                Identifier key = Identifier.tryParse(id);
                if (key == null) {
                    continue;
                }
                // RECIPE_TYPE 不是"带默认值"的注册表，未注册时 getValue 返回 null
                RecipeType<?> type = BuiltInRegistries.RECIPE_TYPE.getValue(key);
                if (type != null) {
                    return type;
                }
                if (level != null) {
                    // 兜底：部分环境下 RECIPE_TYPE 只出现在 level 的 registryAccess 里
                    var registry = level.registryAccess().lookupOrThrow(Registries.RECIPE_TYPE);
                    if (registry != null) {
                        type = registry.getValue(key);
                        if (type != null) {
                            return type;
                        }
                    }
                }
            } catch (Throwable e) {
                log.error("RecipeSource.findAe2InscriberType error for {}", id, e);
            }
        }
        return null;
    }
}
