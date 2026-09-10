package cn.sd.jrz.alltheimbaium.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 物品 tooltip 统一规范构建器。
 * <p>
 * 本模组所有物品的 hover 提示都由本类生成，保证观感一致。结构自上而下：
 * <pre>
 *   §8&lt;类型&gt; · &lt;品级&gt;       ← {@link #head}，品级由物品自身的 Rarity 推导
 *   (空行)
 *   §7&lt;一句话概述&gt;              ← {@link #summary}
 *   (空行)
 *   §7· &lt;键&gt;：§e&lt;值&gt;           ← {@link #state}，动态状态，可省略
 *   (空行)
 *   §6■ 用法                     ← {@link #usage}
 *   §7· …
 *   (空行)
 *   §6■ 参数                     ← {@link #params}，有数值时才出现
 *   §7· &lt;键&gt;：§e&lt;数值&gt;
 *   (空行)
 *   §c■ 注意                     ← {@link #warn}，有限制或风险时才出现
 *   §c· …
 * </pre>
 * 规范约定：
 * <ul>
 *     <li>不写"右击打开 GUI"这类与原版方块一致的通用操作，也不写显而易见的操作；
 *         只写非显然的用法、配置方式、限制与数值。</li>
 *     <li>正文一律 {@code §7}，参数值一律 {@code §e}，警告一律 {@code §c}。</li>
 *     <li>中英文语言文件的段落数与顺序逐行对齐。</li>
 * </ul>
 * 调用顺序即输出顺序；各段之间由 {@link #separator()} 自动补空行，
 * 因此 {@code state()} 连续调用多次也只会产生一个空行。
 */
public final class Tip {

    // ==================== 规范样式常量 ====================
    /** 正文前缀（灰色），用于概述行 */
    private static final String BODY = "§7";
    /** 要点前缀（灰色） */
    private static final String BULLET = "§7· ";
    /** 警告要点前缀（红色） */
    private static final String WARN_BULLET = "§c· ";
    /** 段标题前缀（金色） */
    private static final String SECTION = "§6■ ";
    /** 注意段标题前缀（红色） */
    private static final String SECTION_WARN = "§c■ ";
    /** 类型/品级行前缀（深灰） */
    private static final String HEAD = "§8";
    /** {@link #inline} 单行最多展示的条目数 */
    private static final int MAX_INLINE_ENTRIES = 4;

    // ==================== 固定翻译键 ====================
    private static final String KEY_SECTION_USAGE = "tip.alltheimbaium.section.usage";
    private static final String KEY_SECTION_PARAMS = "tip.alltheimbaium.section.params";
    private static final String KEY_SECTION_NOTES = "tip.alltheimbaium.section.notes";
    private static final String KEY_TIER_COMMON = "tip.alltheimbaium.tier.common";
    private static final String KEY_TIER_UNCOMMON = "tip.alltheimbaium.tier.uncommon";
    private static final String KEY_TIER_RARE = "tip.alltheimbaium.tier.rare";
    private static final String KEY_TIER_EPIC = "tip.alltheimbaium.tier.epic";

    private final List<Component> tooltip;
    /** 是否已经写过第一段；第一段不补空行，让它紧跟物品名 */
    private boolean started;

    private Tip(@Nonnull List<Component> tooltip) {
        this.tooltip = tooltip;
    }

    /**
     * 以现有 tooltip 列表创建构建器
     */
    @Nonnull
    public static Tip of(@Nonnull List<Component> tooltip) {
        return new Tip(tooltip);
    }

    // ==================== 各段 ====================

    /**
     * ① 品级行：{@code §8<类型> · <品级>}。
     * <p>
     * 品级由物品自身的 {@link ItemStack#getRarity()} 推导，因此名称颜色与品级标签
     * 永远来自同一处真值——改 Rarity 就同时改了颜色和标签。
     *
     * @param typeKey 类型翻译键，见 {@code tip.alltheimbaium.type.*}
     */
    @Nonnull
    public Tip head(@Nonnull ItemStack stack, @Nonnull String typeKey) {
        separator();
        tooltip.add(Component.literal(HEAD + translate(typeKey) + " · " + translate(tierKey(stack.getRarity()))));
        return this;
    }

    /**
     * ② 概述：一句话说清物品做什么、作用范围多大
     */
    @Nonnull
    public Tip summary(@Nonnull String key, Object... args) {
        separator();
        tooltip.add(Component.literal(BODY + translate(key, args)));
        return this;
    }

    /**
     * ③ 动态状态行：{@code §7· <键>：§e<值>}。
     * 语言值内自行嵌入 {@code §e} 包裹数值。需在 {@link #usage} 等段之前调用。
     */
    @Nonnull
    public Tip state(@Nonnull String key, Object... args) {
        separator();
        tooltip.add(Component.literal(BULLET + translate(key, args)));
        return this;
    }

    /**
     * ④ 用法段：只列非显然的操作，不写"右击打开 GUI"这类通用交互
     */
    @Nonnull
    public Tip usage(@Nonnull String... keys) {
        section(KEY_SECTION_USAGE, false);
        for (String key : keys) {
            tooltip.add(Component.literal(BULLET + translate(key)));
        }
        return this;
    }

    /**
     * ⑤ 参数段：仅在确有数值可列时调用，语言值内自行嵌入 {@code §e} 包裹数值
     */
    @Nonnull
    public Tip params(@Nonnull String... keys) {
        section(KEY_SECTION_PARAMS, false);
        for (String key : keys) {
            tooltip.add(Component.literal(BULLET + translate(key)));
        }
        return this;
    }

    /**
     * ⑤ 参数段（只写标题）：随后用 {@link #bullet} 追加带参数的条目
     */
    @Nonnull
    public Tip params() {
        section(KEY_SECTION_PARAMS, false);
        return this;
    }

    /**
     * ⑥ 注意段：仅在存在限制或风险时调用
     */
    @Nonnull
    public Tip warn(@Nonnull String... keys) {
        section(KEY_SECTION_NOTES, true);
        for (String key : keys) {
            tooltip.add(Component.literal(WARN_BULLET + translate(key)));
        }
        return this;
    }

    /**
     * 追加一行自定义内容（产物列表、已标记物列表等无法用固定格式表达的行）。
     * 不补空行，紧接当前段落。
     */
    @Nonnull
    public Tip raw(@Nonnull Component line) {
        tooltip.add(line);
        return this;
    }

    /**
     * 追加一行自定义内容，语言键 + 参数形式
     */
    @Nonnull
    public Tip raw(@Nonnull String key, Object... args) {
        return raw(Component.translatable(key, args));
    }

    /**
     * 追加一条要点行，内容由语言键 + 参数给出（自动加 {@code §7·} 前缀）。
     * 用于需要自行控制条目上限，或参数本身就是一长串拼接文本的场景。
     * 不补空行，紧接当前段落。
     */
    @Nonnull
    public Tip bullet(@Nonnull String key, Object... args) {
        tooltip.add(Component.literal(BULLET + translate(key, args)));
        return this;
    }

    /**
     * 把条目列表压成一行要点，超出部分折叠成"…等 N 项"。
     * 农场类物品的产物表最多有 27 行，逐行铺开会把 tooltip 撑爆，故统一走这里。
     *
     * @param entries 已带颜色码的条目文本
     * @param moreKey 折叠提示的翻译键，接收一个参数：被隐藏的条目数
     */
    @Nonnull
    public static Component inline(@Nonnull List<String> entries, @Nonnull String moreKey) {
        return Component.literal(BULLET + join(entries, MAX_INLINE_ENTRIES, moreKey));
    }

    /**
     * 用 {@code §7 · } 连接条目，超出上限的部分折叠成"…等 N 项"。
     * 供需要自行控制条目上限、或把结果嵌进语言键参数时使用。
     */
    @Nonnull
    public static String join(@Nonnull List<String> entries, int max, @Nonnull String moreKey) {
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(entries.size(), max);
        for (int i = 0; i < shown; i++) {
            if (i > 0) {
                sb.append("§7 · ");
            }
            sb.append(entries.get(i));
        }
        if (entries.size() > shown) {
            sb.append("§7 · ").append(Component.translatable(moreKey, entries.size() - shown).getString());
        }
        return sb.toString();
    }

    // ==================== 内部 ====================

    /**
     * 段标题：自动补空行后写入 {@code §6■ <标题>}（注意段为红色）
     */
    private void section(@Nonnull String titleKey, boolean warnStyle) {
        separator();
        tooltip.add(Component.literal((warnStyle ? SECTION_WARN : SECTION) + translate(titleKey)));
    }

    /**
     * 段间空行：第一段不补，此后仅当末行不是空行时才插入，避免连续调用产生多个空行
     */
    private void separator() {
        if (!started) {
            started = true;
            return;
        }
        if (!tooltip.isEmpty() && !tooltip.get(tooltip.size() - 1).getString().isEmpty()) {
            tooltip.add(Component.empty());
        }
    }

    // ==================== 品级颜色（供 GUI 等复用） ====================

    /**
     * 物品稀有度对应的颜色，与物品名颜色同源。
     * <p>
     * GUI 标题等处直接给组件套上这个颜色，界面标题就会和物品名完全一致；
     * 组件自带的 Style 颜色优先级高于 {@code drawString} 传入的颜色参数，无需改动各屏幕的绘制代码。
     */
    @Nonnull
    public static ChatFormatting rarityColor(@Nonnull ItemStack stack) {
        return switch (stack.getRarity()) {
            case UNCOMMON -> ChatFormatting.YELLOW;
            case RARE -> ChatFormatting.AQUA;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.WHITE;
        };
    }

    /**
     * 取某物品默认实例的稀有度颜色
     */
    @Nonnull
    public static ChatFormatting rarityColor(@Nonnull Item item) {
        return rarityColor(new ItemStack(item));
    }

    /**
     * 品级翻译键，按物品稀有度映射
     */
    @Nonnull
    private static String tierKey(@Nonnull Rarity rarity) {
        return switch (rarity) {
            case UNCOMMON -> KEY_TIER_UNCOMMON;
            case RARE -> KEY_TIER_RARE;
            case EPIC -> KEY_TIER_EPIC;
            default -> KEY_TIER_COMMON;
        };
    }

    /**
     * 翻译为纯文本；键缺失时返回键名本身，便于定位漏翻
     * <p>
     * <b>必须先把译文取成字符串再拼接，不能写成
     * {@code Component.literal("§7").append(Component.translatable(key))}。</b>
     * Minecraft 的 {@code StringDecomposer} 是逐组件解析 {@code §} 的，格式状态不会跨兄弟组件
     * 传递——前缀组件里的 {@code §7} 被吃掉后样式随即丢弃，紧随其后的译文组件仍以默认色（白）
     * 渲染，于是出现"染色前是白色、染色后才是灰色"的断层。
     * 把前缀和译文放进同一条字符串，{@code §} 才会对后续文字生效。
     */
    @Nonnull
    private static String translate(@Nonnull String key, Object... args) {
        return args.length == 0
                ? Component.translatable(key).getString()
                : Component.translatable(key, args).getString();
    }
}
