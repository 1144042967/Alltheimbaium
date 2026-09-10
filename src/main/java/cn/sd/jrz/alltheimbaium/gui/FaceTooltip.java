package cn.sd.jrz.alltheimbaium.gui;

import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 六面输出按钮的 hover tooltip 统一构建。
 * <p>
 * 所有带六面输出按钮的界面（自动耕地 / 生物农场 / 资源农场 / 存储方块制造机 / 液体无限制造机）
 * 都走这里，保证观感一致：
 * <pre>
 *   §f&lt;要输出的内容&gt;              ① 无标签，有可选项的机器始终显示
 *   §7输出方向：§e&lt;方向&gt;            ②
 *   §7输出目标：§e&lt;指向的方块&gt;       ③ 无目标时显示 §8无
 * </pre>
 * 标签用暗色 {@code §7}、内容用亮色，是这一组提示的统一读法。
 * <p>
 * 拼装一律用字符串拼接，不用 {@code Component.append}：{@code §} 的格式状态不跨兄弟组件传递，
 * 放进独立组件的前缀会被丢弃（同 {@code item/Tip.java} 的处理）。
 */
public final class FaceTooltip {

    /** 内容行没有标签，直接用最亮的白色 */
    private static final String CONTENT_COLOR = "§f";

    private FaceTooltip() {
    }

    /**
     * @param directionName 方向名
     * @param targetName    该方向指向的方块名；无目标传 {@code null}
     * @param outputContent 当前要输出的内容：物品名 / 槽 N / 随机 / 禁用。
     *                      传 {@code null} 表示该机器没有可选的输出内容（如液体机只有一种流体），不显示内容行
     */
    @Nonnull
    public static List<Component> build(@Nonnull String directionName,
                                        @Nullable String targetName,
                                        @Nullable String outputContent) {
        List<Component> lines = new ArrayList<>();
        if (outputContent != null && !outputContent.isEmpty()) {
            lines.add(Component.literal(CONTENT_COLOR + outputContent));
        }
        lines.add(Component.translatable("screen.alltheimbaium.output.direction", directionName));
        lines.add(targetName != null
                ? Component.translatable("screen.alltheimbaium.output.target", targetName)
                : Component.translatable("screen.alltheimbaium.output.no_target"));
        return lines;
    }
}
