package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * 补给箱随机逻辑：从创造物品栏 10 个分类中各随机取一个物品。
 * <p>
 * 10 个分类顺序与 GUI 两排按钮对应：建筑、染色、自然、功能、红石、
 * 工具与实用物品、战斗、食物与饮品、原材料、刷怪蛋。
 * <p>
 * 随机结果由 世界种子 | 当前世界游戏小时数 | 玩家已用补给点 三值拼接作随机种子，确定生成；
 * 每次打开 GUI / 兑换 / 刷新都会按当时的状态重新生成一次。物品黑名单见配置。
 */
public class SupplyRoll {
    /**
     * 分类键（原版注册名），索引即 GUI 槽位。
     * <p>
     * 26.x：{@code CreativeModeTabs} 的分类键字段全部改成了 private（只剩 {@code getDefaultTab()}），
     * 不能再直接引用，这里按原版注册名自行构造——{@code CreativeModeTabs.createKey} 用的就是同一个表达式。
     */
    public static final ResourceKey<CreativeModeTab>[] CATEGORIES = new ResourceKey[]{
            tabKey("building_blocks"),      // 0 建筑方块
            tabKey("colored_blocks"),       // 1 染色方块
            tabKey("natural_blocks"),       // 2 自然方块
            tabKey("functional_blocks"),    // 3 功能方块
            tabKey("redstone_blocks"),      // 4 红石方块
            tabKey("tools_and_utilities"),  // 5 工具与实用物品
            tabKey("combat"),               // 6 战斗用品
            tabKey("food_and_drinks"),      // 7 食物与饮品
            tabKey("ingredients"),          // 8 原材料
            tabKey("spawn_eggs")            // 9 刷怪蛋
    };

    /** 创造物品栏分类键（与原版 {@code CreativeModeTabs.createKey} 等价） */
    private static ResourceKey<CreativeModeTab> tabKey(@Nonnull String id) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace(id));
    }

    /** 各分类 tooltip 用语言键后缀（与 CATEGORIES 同序） */
    public static final String[] CATEGORY_TOKENS = {
            "building", "colored", "natural", "functional", "redstone",
            "tools", "combat", "food", "ingredients", "spawn"
    };

    /** 黑名单物品注册 ID（由配置加载，默认基岩/末地传送门框架） */
    private static List<? extends String> blacklist = List.of();

    private SupplyRoll() {
    }

    /**
     * 由 Config.onConfigLoad() 在配置加载完成后调用
     */
    public static void loadConfig() {
        blacklist = Config.SUPPLY_CRATE_BLACKLIST.get();
    }

    public static boolean isBlacklisted(@Nonnull Item item) {
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        for (String entry : blacklist) {
            if (id.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按当前状态生成 10 个随机物品（每分类一个）。分类无可用物品时对应项为空。
     */
    public static ItemStack[] roll(@Nonnull ServerLevel level, @Nonnull Player player) {
        // 世界累计真实小时数（72000 tick = 1 真实小时），GUI 不动时每真实小时自动刷新一次
        long hours = level.getGameTime() / (20L * 60 * 60);
        String seedStr = level.getSeed() + "|" + hours + "|" + SupplyData.getUsed(player);
        Random random = new Random((long) seedStr.hashCode());

        ItemStack[] result = new ItemStack[CATEGORIES.length];
        for (int i = 0; i < CATEGORIES.length; i++) {
            List<ItemStack> candidates = collectCandidates(level, player, CATEGORIES[i]);
            if (candidates.isEmpty()) {
                result[i] = ItemStack.EMPTY;
                continue;
            }
            ItemStack picked = candidates.get(random.nextInt(candidates.size()));
            ItemStack copy = picked.copy();
            copy.setCount(1);
            result[i] = copy;
        }
        return result;
    }

    /**
     * 收集某创造分类里非空且不在黑名单的物品
     */
    private static List<ItemStack> collectCandidates(ServerLevel level, Player player, ResourceKey<CreativeModeTab> key) {
        List<ItemStack> list = new ArrayList<>();
        // 26.x：registryOrThrow -> lookupOrThrow；ResourceKey#location -> #identifier；
        // Registry#get(Identifier) 改成了 Optional<Holder>，取实体要走 getValue(可空)
        CreativeModeTab tab = level.registryAccess().lookupOrThrow(Registries.CREATIVE_MODE_TAB).getValue(key.identifier());
        if (tab == null) {
            return list;
        }
        Collection<ItemStack> items = tab.getDisplayItems();
        if (items.isEmpty()) {
            // 创造物品栏内容尚未构建：强制构建一次
            // 26.x：Player#hasPermissions(int) 已删除，权限改走 PermissionSet（等级 2 == GAMEMASTERS）
            CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(),
                    player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER), level.registryAccess());
            items = tab.getDisplayItems();
        }
        if (items == null) {
            return list;
        }
        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty() || stack.getItem() == Items.AIR) {
                continue;
            }
            if (isBlacklisted(stack.getItem())) {
                continue;
            }
            list.add(stack);
        }
        return list;
    }
}
