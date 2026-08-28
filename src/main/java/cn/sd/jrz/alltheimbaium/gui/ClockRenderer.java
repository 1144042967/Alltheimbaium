package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.ClockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * 加速时钟的方块实体渲染器：在方块四个侧面（北/南/东/西）的深色显示区居中绘制当前倍速数值（白色纯数字，如 64）。
 * <p>
 * 参考 Create 的 NixieTubeRenderer（Font.drawInBatch 渲染 3D 文字）+ StorageFountainRenderer（侧面旋转）。
 * 文字字号固定为与最宽档位 1024 相同，文字位置可通过下方常量按位数微调。
 */
@OnlyIn(Dist.CLIENT)
public class ClockRenderer implements BlockEntityRenderer<ClockEntity> {
    /**
     * 旋转角度倍数（DOWN/UP/NORTH/SOUTH/WEST/EAST）：旋转后目标面朝向 +Z（与 StorageFountainRenderer 一致）
     */
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    /**
     * 需要显示数值的四个侧面（上下两面除外）
     */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    /**
     * 文字颜色（白色）
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
    public void render(@Nonnull ClockEntity entity, float partialTick, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        for (Direction side : SIDES) {
            renderText(entity, side, poseStack, buffer);
        }
    }

    /**
     * 在指定方块面上居中绘制倍速数值
     */
    private void renderText(ClockEntity entity, Direction face, PoseStack poseStack, MultiBufferSource buffer) {
        poseStack.pushPose();
        // 绕 Y 旋转使目标面朝向 +Z（Y 方向不变，文字保持竖直正立）
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPoseMatrix((new Matrix4f()).rotateYXZ(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f, 0, 0));
        poseStack.translate(-0.5f, 0, -0.5f);

        // 只显示纯数字（不带 x 前缀）
        String text = String.valueOf(entity.speed);
        // 按位数取水平偏移：水平中心 = 面板中心 + 位数偏移
        int digits = text.length();
        poseStack.translate(PANEL_CENTER_X, 0.5f, 1.004f);

        Font font = Minecraft.getInstance().font;
        // 固定字号：以最宽档位 1024 为基准，使文字宽度适配深色显示区宽度（约 8px）
        float scale = 0.4f / font.width("1024");
        // 缩放并翻转 Y（字体 y 向下为正，方块面 y 向上为正），使文字在面上保持正立
        poseStack.scale(scale, -scale, scale);
        // 按文本宽度水平居中（-width/2），-4 垂直居中（文字高约 8px）
        font.drawInBatch(text, -font.width(text) / 2f, -4f, TEXT_COLOR, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        if (buffer instanceof BufferSource bufferSource) {
            // 立即提交文字渲染批，确保文字在后续渲染前显示
            bufferSource.endBatch();
        }
        poseStack.popPose();
    }
}
