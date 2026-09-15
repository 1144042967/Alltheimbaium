package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * 液体无限制造机内部液体渲染器。
 * <p>
 * 在镂空罐体内部绘制半透明液面，液面高度随存量/阈值比例变化。
 * 液面使用流体的静止纹理与着色颜色，几何范围参考 AE2 SkyStoneTankRenderer
 * （几乎填满整个方块、留 1px 罐壁，避免与方块材质 Z 冲突）。
 * <p>
 * 26.x：{@code IClientFluidTypeExtensions#getStillTexture/getTintColor} 已删除，
 * 流体贴图与颜色改从数据驱动的 {@code FluidStateModelSet}（{@code FluidModel}）取；
 * geometry 走 {@link SubmitNodeCollector#submitCustomGeometry}，渲染类型为 {@link RenderTypes#translucentMovingBlock()}。
 */
public class LiquidFountainRenderer implements BlockEntityRenderer<LiquidFountainEntity, LiquidFountainRenderer.LiquidFountainRenderState> {
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
    /**
     * 方块图集的半透明渲染类型（与液体原版渲染一致）
     */
    private static final RenderType RENDER_TYPE = RenderTypes.translucentMovingBlock();

    public LiquidFountainRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    @Nonnull
    public LiquidFountainRenderState createRenderState() {
        return new LiquidFountainRenderState();
    }

    @Override
    public void extractRenderState(@Nonnull LiquidFountainEntity entity, @Nonnull LiquidFountainRenderState state, float partialTick,
                                   @Nonnull Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.hasFluid = false;
        state.sprite = null;

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

        // 流体静止贴图与着色颜色：26.x 由数据驱动的 FluidModel 提供（未注册模型的模组流体会回落到 missing 模型）
        FluidState fluidState = fluid.getFluid().defaultFluidState();
        FluidModel model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(fluidState);
        int argb = -1;
        if (model.tintSource() != null) {
            Level level = entity.getLevel();
            argb = level instanceof ClientLevel clientLevel
                    ? model.tintSource().colorInWorld(entity.getBlockState(), clientLevel, entity.getBlockPos())
                    : model.tintSource().color(entity.getBlockState());
        }
        // alpha 处理：tint 颜色未指定 alpha 时，用液面比例 + 0.2（参考 Mekanism getColorARGB）
        float alpha;
        int alphaChannel = ARGB.alpha(argb);
        if (alphaChannel == 0) {
            alpha = Math.min(1.0F, scale + 0.2F);
        } else {
            alpha = alphaChannel / 255.0F;
        }

        state.sprite = model.stillMaterial().sprite();
        state.red = ARGB.red(argb) / 255.0F;
        state.green = ARGB.green(argb) / 255.0F;
        state.blue = ARGB.blue(argb) / 255.0F;
        state.alpha = alpha;
        state.topY = BASE_Y + (FULL_Y - BASE_Y) * scale;
        state.hasFluid = true;
    }

    @Override
    public void submit(LiquidFountainRenderState state, @Nonnull PoseStack poseStack, @Nonnull SubmitNodeCollector collector, @Nonnull CameraRenderState camera) {
        TextureAtlasSprite sprite = state.sprite;
        if (!state.hasFluid || sprite == null) {
            return;
        }
        final float y1 = state.topY;
        final float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        final float r = state.red, g = state.green, b = state.blue, a = state.alpha;
        final int light = state.lightCoords;

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
            final Face f = face;
            collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) ->
                    quad(consumer, pose.pose(), f.vs, u0, u1, f.flipV ? v1 : v0, f.flipV ? v0 : v1,
                            f.nx, f.ny, f.nz, r, g, b, a, light));
        }
        // 顶面
        final float[][] top = {{MIN, y1, MAX}, {MAX, y1, MAX}, {MAX, y1, MIN}, {MIN, y1, MIN}};
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) ->
                quad(consumer, pose.pose(), top, u0, u1, v0, v1, 0, 1, 0, r, g, b, a, light));
    }

    /**
     * 绘制一个四边形（逆时针，法线朝外）。
     *
     * @param vs 顶点，依次为左下/右下/右上/左上
     */
    private void quad(VertexConsumer consumer, Matrix4f mat, float[][] vs, float uFrom, float uTo, float vTop, float vBottom,
                      float nx, float ny, float nz, float r, float g, float b, float a, int light) {
        addVertex(consumer, mat, vs[0][0], vs[0][1], vs[0][2], uFrom, vBottom, r, g, b, a, light, nx, ny, nz);
        addVertex(consumer, mat, vs[1][0], vs[1][1], vs[1][2], uTo, vBottom, r, g, b, a, light, nx, ny, nz);
        addVertex(consumer, mat, vs[2][0], vs[2][1], vs[2][2], uTo, vTop, r, g, b, a, light, nx, ny, nz);
        addVertex(consumer, mat, vs[3][0], vs[3][1], vs[3][2], uFrom, vTop, r, g, b, a, light, nx, ny, nz);
    }

    private void addVertex(VertexConsumer consumer, Matrix4f mat, float x, float y, float z, float u, float v,
                           float r, float g, float b, float a, int light, float nx, float ny, float nz) {
        consumer.addVertex(mat, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(nx, ny, nz);
    }

    /**
     * 液面一个面的几何描述：4 个顶点（已按逆时针 CCW 排列、法线朝外）+ 法线 + 是否翻转 V 纹理。
     * 北面纹理上下颠倒，通过 flipV 交换 vTop/vBottom，避免绕序被背面剔除的同时纹理颠倒。
     */
    private record Face(float[][] vs, float nx, float ny, float nz, boolean flipV) {
    }

    /**
     * 渲染状态：液面比例算出的顶面高度 + 静止贴图 + 着色颜色（无液体时 hasFluid 为 false）
     */
    public static class LiquidFountainRenderState extends BlockEntityRenderState {
        public boolean hasFluid = false;
        public TextureAtlasSprite sprite = null;
        public float red = 1.0F;
        public float green = 1.0F;
        public float blue = 1.0F;
        public float alpha = 1.0F;
        public float topY = BASE_Y;
    }
}
