package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.ClockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;

/**
 * 加速时钟的方块实体渲染器：在方块四个侧面（北/南/东/西）的深色显示区居中绘制当前倍速数值（白色纯数字，如 64）。
 * <p>
 * 26.x 改为 state-based 渲染模型（createRenderState / extractRenderState / submit），
 * 文字改用 {@link SubmitNodeCollector#submitText}（原 {@code Font.drawInBatch} + {@code endBatch} 的写法已不再需要）。
 * 文字字号固定为与最宽档位 1024 相同，文字位置可通过下方常量按位数微调。
 */
public class ClockRenderer implements BlockEntityRenderer<ClockEntity, ClockRenderer.ClockRenderState> {
    /**
     * 旋转角度倍数（DOWN/UP/NORTH/SOUTH/WEST/EAST）：旋转后目标面朝向 +Z（与 StorageFountainRenderer 一致）
     */
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    /**
     * 需要显示数值的四个侧面（上下两面除外）
     */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    /**
     * 文字颜色（白色，ARGB）
     */
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    // ==================== 位置微调参数（按需调整） ====================
    /**
     * 贴图中央深色显示区的水平中心位置（方块单位，面中心为 0.5）。
     * 若所有数字整体偏左/偏右，调整此值（正值右移、负值左移）。
     */
    private static final float PANEL_CENTER_X = 0.5f;
    // ==============================================================

    public ClockRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @Override
    @Nonnull
    public ClockRenderState createRenderState() {
        return new ClockRenderState();
    }

    @Override
    public void extractRenderState(@Nonnull ClockEntity entity, @Nonnull ClockRenderState state, float partialTick,
                                   @Nonnull Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.speed = entity.speed;
    }

    @Override
    public void submit(ClockRenderState state, @Nonnull PoseStack poseStack, @Nonnull SubmitNodeCollector collector, @Nonnull CameraRenderState camera) {
        // 只显示纯数字（不带 x 前缀）
        FormattedCharSequence text = Component.literal(String.valueOf(state.speed)).getVisualOrderText();
        for (Direction side : SIDES) {
            submitText(text, side, poseStack, collector);
        }
    }

    /**
     * 在指定方块面上居中绘制倍速数值
     */
    private void submitText(FormattedCharSequence text, Direction face, PoseStack poseStack, SubmitNodeCollector collector) {
        poseStack.pushPose();
        // 绕 Y 旋转使目标面朝向 +Z（Y 方向不变，文字保持竖直正立）
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPose(new Quaternionf().rotateY(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f));
        poseStack.translate(-0.5f, 0, -0.5f);
        // 面中心：面板水平中心 + 竖直居中 + 略微抬离面表面
        poseStack.translate(PANEL_CENTER_X, 0.5f, 1.004f);

        Font font = Minecraft.getInstance().font;
        // 固定字号：以最宽档位 1024 为基准，使文字宽度适配深色显示区宽度（约 8px）
        float scale = 0.4f / font.width("1024");
        // 缩放并翻转 Y（字体 y 向下为正，方块面 y 向上为正），使文字在面上保持正立
        poseStack.scale(scale, -scale, scale);
        // 按文本宽度水平居中（-width/2），-4 垂直居中（文字高约 8px）
        collector.submitText(poseStack, -font.width(text) / 2f, -4f, text, false,
                Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, TEXT_COLOR, 0, 0);
        poseStack.popPose();
    }

    /**
     * 渲染状态：只保存当前倍速数值（渲染线程不直接访问方块实体）
     */
    public static class ClockRenderState extends BlockEntityRenderState {
        public int speed = 2;
    }
}
