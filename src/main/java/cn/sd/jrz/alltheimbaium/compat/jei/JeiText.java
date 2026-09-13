package cn.sd.jrz.alltheimbaium.compat.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * JEI 卡片上的文字排版工具（只在客户端配方渲染时用到）。
 * <p>
 * JEI 的 {@code addText} 不会自动折行，长说明会直接画出卡片边界，所以这里先按像素宽拆好。
 * 拆行用的是原版 {@code Font.split}，返回的已排版序列会**丢掉样式**——因此调用方给的说明文字
 * 应当是纯文本（JEI 卡片上的这些小字本来也不需要颜色）。
 */
public final class JeiText {

    private static final String ELLIPSIS = "…";

    private JeiText() {
    }

    /**
     * 把一段文字按像素宽折行，最多取 {@code maxLines} 行；放不下的部分截断并在末行加省略号。
     */
    @Nonnull
    public static List<Component> wrap(@Nonnull Component text, int maxWidth, int maxLines) {
        List<Component> out = new ArrayList<>();
        Font font = Minecraft.getInstance().font;
        if (font == null || maxWidth <= 0 || maxLines <= 0) {
            out.add(text);
            return out;
        }
        List<FormattedCharSequence> lines = font.split(text, maxWidth);
        if (lines.isEmpty()) {
            out.add(text);
            return out;
        }
        int shown = Math.min(lines.size(), maxLines);
        for (int i = 0; i < shown; i++) {
            String line = plain(lines.get(i));
            if (i == shown - 1 && lines.size() > maxLines) {
                line = line + ELLIPSIS;
            }
            out.add(Component.literal(line));
        }
        return out;
    }

    /** 把已排版的字符序列拼回纯文本 */
    @Nonnull
    private static String plain(@Nonnull FormattedCharSequence sequence) {
        StringBuilder sb = new StringBuilder();
        sequence.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }
}
