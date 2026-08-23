package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * 液体无限制造机内部液体渲染器。
 * <p>
 * 在镂空罐体内部绘制半透明液面，液面高度随存量/阈值比例变化。
 * 液面使用流体的静止纹理与着色颜色，几何范围参考 AE2 SkyStoneTankRenderer
 * （几乎填满整个方块、留 1px 罐壁，避免与方块材质 Z 冲突）。
 */
@OnlyIn(Dist.CLIENT)
public class LiquidFountainRenderer implements BlockEntityRenderer<LiquidFountainEntity> {
    /**
     * 罐壁厚度（1px + 0.001 防 Z 冲突），参考 AE2 SkyStoneTankRenderer
     */
    private static final float TANK_W = 1 / 16f + 0.001F;
    /**
     * 液面水平边界（几乎填满整个方块，液体透过玻璃窗可见）
     */
    private static final float MIN = TANK_W;
    private static final float MAX = 1 - TANK_W;
    /**
     * 液面垂直范围
     */
    private static final float BASE_Y = TANK_W;
    private static final float FULL_Y = 1 - TANK_W;
    /**
     * 最低液面比例：只要有液体就至少显示该比例的液面高度，让用户看出有液体
     */
    private static final float MIN_SCALE = 0.2F;

    public LiquidFountainRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@Nonnull LiquidFountainEntity entity, float partialTick, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        FluidStack fluid = entity.getStack();
        if (fluid.isEmpty()) {
            return;
        }
        long max = entity.getMax();
        if (max <= 0) {
            return;
        }
        long amount = fluid.getAmount();
        if (amount <= 0) {
            return;
        }
        float scale = (float) Math.min(1.0, (double) amount / max);
        // 最低液面效果：只要有液体就至少显示一小段液面，避免玻璃窗看起来是空的
        scale = Math.max(MIN_SCALE, scale);
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ext.getStillTexture(fluid));
        int argb = ext.getTintColor(fluid);
        // alpha 处理：tint 颜色未指定 alpha 时，用液面比例 + 0.2（参考 Mekanism getColorARGB）
        float alpha;
        int alphaChannel = FastColor.ARGB32.alpha(argb);
        if (alphaChannel == 0) {
            alpha = Math.min(1.0F, scale + 0.2F);
        } else {
            alpha = alphaChannel / 255.0F;
        }
        float r = FastColor.ARGB32.red(argb) / 255.0F;
        float g = FastColor.ARGB32.green(argb) / 255.0F;
        float b = FastColor.ARGB32.blue(argb) / 255.0F;

        float y1 = BASE_Y + (FULL_Y - BASE_Y) * scale;
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f mat = poseStack.last().pose();
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();

        // 液面立方体的四个侧面 + 底面（顶点已按逆时针 CCW 排列；北面纹理上下颠倒需 flipV）
        // 注意：顶点若为顺时针会被 translucent 背面剔除而不渲染（参考北面/底面的修复）
        Face[] faces = {
                new Face(new float[][]{{MIN, y1, MIN}, {MAX, y1, MIN}, {MAX, BASE_Y, MIN}, {MIN, BASE_Y, MIN}}, 0, 0, -1, true),  // 北
                new Face(new float[][]{{MIN, BASE_Y, MAX}, {MAX, BASE_Y, MAX}, {MAX, y1, MAX}, {MIN, y1, MAX}}, 0, 0, 1, false),  // 南
                new Face(new float[][]{{MIN, BASE_Y, MIN}, {MIN, BASE_Y, MAX}, {MIN, y1, MAX}, {MIN, y1, MIN}}, -1, 0, 0, false), // 西
                new Face(new float[][]{{MAX, BASE_Y, MAX}, {MAX, BASE_Y, MIN}, {MAX, y1, MIN}, {MAX, y1, MAX}}, 1, 0, 0, false),  // 东
                new Face(new float[][]{{MIN, BASE_Y, MIN}, {MAX, BASE_Y, MIN}, {MAX, BASE_Y, MAX}, {MIN, BASE_Y, MAX}}, 0, -1, 0, false), // 底
        };
        for (Face face : faces) {
            quad(consumer, mat, face.vs, u0, u1, face.flipV ? v1 : v0, face.flipV ? v0 : v1,
                    face.nx, face.ny, face.nz, r, g, b, alpha, combinedLight, combinedOverlay);
        }
        // 顶面（液面未满时才渲染，法线朝 +Y）
        if (y1 < FULL_Y - 0.001F) {
            quad(consumer, mat,
                    new float[][]{{MIN, y1, MAX}, {MAX, y1, MAX}, {MAX, y1, MIN}, {MIN, y1, MIN}},
                    u0, u1, v0, v1, 0, 1, 0, r, g, b, alpha, combinedLight, combinedOverlay);
        }
    }

    /**
     * 绘制一个四边形（逆时针，法线朝外）。
     *
     * @param vs 顶点，依次为左下/右下/右上/左上
     */
    private void quad(VertexConsumer consumer, Matrix4f mat, float[][] vs, float uFrom, float uTo, float vTop, float vBottom,
                      float nx, float ny, float nz, float r, float g, float b, float a, int light, int overlay) {
        addVertex(consumer, mat, vs[0][0], vs[0][1], vs[0][2], uFrom, vBottom, r, g, b, a, light, overlay, nx, ny, nz);
        addVertex(consumer, mat, vs[1][0], vs[1][1], vs[1][2], uTo, vBottom, r, g, b, a, light, overlay, nx, ny, nz);
        addVertex(consumer, mat, vs[2][0], vs[2][1], vs[2][2], uTo, vTop, r, g, b, a, light, overlay, nx, ny, nz);
        addVertex(consumer, mat, vs[3][0], vs[3][1], vs[3][2], uFrom, vTop, r, g, b, a, light, overlay, nx, ny, nz);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f mat, float x, float y, float z, float u, float v,
                           float r, float g, float b, float a, int light, int overlay, float nx, float ny, float nz) {
        consumer.vertex(mat, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light).normal(nx, ny, nz).endVertex();
    }

    /**
     * 液面一个面的几何描述：4 个顶点（已按逆时针 CCW 排列、法线朝外）+ 法线 + 是否翻转 V 纹理。
     * 北面纹理上下颠倒，通过 flipV 交换 vTop/vBottom，避免绕序被背面剔除的同时纹理颠倒。
     */
    private record Face(float[][] vs, float nx, float ny, float nz, boolean flipV) {
    }
}
