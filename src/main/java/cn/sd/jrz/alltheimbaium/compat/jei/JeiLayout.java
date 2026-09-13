package cn.sd.jrz.alltheimbaium.compat.jei;

/**
 * JEI 卡片布局参数（同一套版面给不同机器用）。
 *
 * @param inputSlots 左侧输入列的**容量**（用于算卡片尺寸）：熔炉 1、压印器 3、生物农场 2；0 = 不要输入列
 * @param cols       产物网格列数
 * @param rows       产物网格行数
 * @param hasNote    是否在卡片底部留出说明文字的位置
 */
public record JeiLayout(int inputSlots, int cols, int rows, boolean hasNote) {

    /** 单个槽位边长（JEI 的 slot.png 是 18×18；产物格也必须用同一尺寸，见 CLAUDE.md） */
    public static final int SLOT = 18;
    /** 卡片四周留白 */
    public static final int PAD = 4;
    /** JEI 自带箭头的尺寸 */
    public static final int ARROW_W = 22;
    public static final int ARROW_H = 16;
    /** 说明文字最多几行、行高 */
    public static final int NOTE_LINES = 2;
    public static final int LINE_H = 10;

    /** 一页网格的槽位数（产物格**始终**把这么多槽位画出来，没产物的也画背景） */
    public int maxShown() {
        return cols * rows;
    }
}
