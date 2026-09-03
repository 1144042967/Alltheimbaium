package cn.sd.jrz.alltheimbaium.setup;

import net.minecraft.world.entity.player.Player;

import javax.annotation.Nonnull;

/**
 * 玩家补给点数据（最大补给点 / 已用补给点 / 累计游玩秒数）。
 * <p>
 * 数据存入玩家 persistent data（随玩家存档持久化，世界内有效）：
 * 每累计游玩 30 分钟最大补给点 +1（跨会话累计）；每获得一个成就最大补给点 +5。
 * 已用补给点由兑换(3)/刷新(1)消耗。
 */
public class SupplyData {
    /** 每累计游玩多少秒增加 1 点最大补给点（30 分钟） */
    public static final int SECONDS_PER_MAX_POINT = 30 * 60;
    /** 兑换一个选中物品消耗的补给点数 */
    public static final int COST_REDEEM = 3;
    /** 刷新一次（重新随机 10 个物品）消耗的补给点数 */
    public static final int COST_REFRESH = 1;

    private static final String KEY_MAX = "atiSupplyMax";
    private static final String KEY_USED = "atiSupplyUsed";
    private static final String KEY_SEC = "atiSupplySeconds";

    private SupplyData() {
    }

    public static int getMax(@Nonnull Player player) {
        return player.getPersistentData().getInt(KEY_MAX);
    }

    public static int getUsed(@Nonnull Player player) {
        return player.getPersistentData().getInt(KEY_USED);
    }

    /** 当前剩余可用的补给点 */
    public static int getRemaining(@Nonnull Player player) {
        return Math.max(0, getMax(player) - getUsed(player));
    }

    public static int getSeconds(@Nonnull Player player) {
        return player.getPersistentData().getInt(KEY_SEC);
    }

    public static void setMax(@Nonnull Player player, int value) {
        player.getPersistentData().putInt(KEY_MAX, Math.max(0, value));
    }

    public static void addMax(@Nonnull Player player, int add) {
        setMax(player, getMax(player) + add);
    }

    public static void setUsed(@Nonnull Player player, int value) {
        player.getPersistentData().putInt(KEY_USED, Math.max(0, value));
    }

    public static void addUsed(@Nonnull Player player, int add) {
        setUsed(player, getUsed(player) + add);
    }

    public static void setSeconds(@Nonnull Player player, int value) {
        player.getPersistentData().putInt(KEY_SEC, Math.max(0, value));
    }

    /**
     * 累计游玩秒数推进 1 秒；每当累计满 {@link #SECONDS_PER_MAX_POINT} 秒，最大补给点 +1 并扣除。
     * 供服务端游玩计时事件调用。
     */
    public static void tickSecond(@Nonnull Player player) {
        int seconds = getSeconds(player) + 1;
        while (seconds >= SECONDS_PER_MAX_POINT) {
            seconds -= SECONDS_PER_MAX_POINT;
            addMax(player, 1);
        }
        setSeconds(player, seconds);
    }
}
