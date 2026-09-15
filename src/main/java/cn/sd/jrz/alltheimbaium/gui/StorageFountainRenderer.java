package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 存储方块制造机的方块实体渲染器：在方块四个侧面（北/南/东/西）按九宫格（3×3）绘制已标记物品的贴图。
 * <p>
 * 参考 auto-resource 的方块生成机渲染器：取物品的粒子贴图用 cutout 画平面四边形（共用方块图集，避免首帧贴图未加载），
 * 强制至少 15 级方块光照。九宫格位置与 GUI 中 1~9 槽一一对应（左上开始）。
 * <p>
 * 26.x：state-based 渲染模型；物品粒子贴图在 extractRenderState 阶段解析好放进渲染状态，
 * submit 里只做几何提交（{@link SubmitNodeCollector#submitCustomGeometry} + {@link RenderTypes#cutoutMovingBlock()}）。
 */
public class StorageFountainRenderer implements BlockEntityRenderer<StorageFountainEntity, StorageFountainRenderer.StorageFountainRenderState> {
    /** 旋转角度倍数（DOWN/UP/NORTH/SOUTH/WEST/EAST）：旋转后目标面朝向 +Z（与 StorageDrawers 一致） */
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    /** 需要显示物品的四个侧面（上下两面除外） */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    /** 侧面贴图边缘的 1 像素边框宽度（16×16 纹理的 1 像素 = 1/16 方块单位，加少量余量） */
    private static final float BORDER = 0.5f / 16f + 0.005f;
    /** 去掉四周边框后的有效区域边长 */
    private static final float AREA = 1f - 2f * BORDER;
    /** 九宫格格子边长 */
    private static final float CELL = AREA / 3f;
    /** 每个格子内贴图的半边长（约占格子的 60%，避免相邻贴图重叠） */
    private static final float INSET = CELL * 0.3f;
    /** 强制的最低方块光照（15 级 = 全亮），避免贴图太暗 */
    private static final int MIN_BLOCK_LIGHT = 15 << 4;
    /** 使用方块图集的 cutout 渲染类型（与方块本体共用方块图集） */
    private static final RenderType RENDER_TYPE = RenderTypes.cutoutMovingBlock();

    private final ItemModelResolver itemModelResolver;

    public StorageFountainRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    @Nonnull
    public StorageFountainRenderState createRenderState() {
        return new StorageFountainRenderState();
    }

    @Override
    public void extractRenderState(@Nonnull StorageFountainEntity entity, @Nonnull StorageFountainRenderState state, float partialTick,
                                   @Nonnull Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.sprites.clear();
        Level level = entity.getLevel();
        for (ItemStack stack : entity.itemList) {
            // 空槽保留占位（null），保证九宫格位置与 GUI 槽位一一对应
            state.sprites.add(stack.isEmpty() ? null : getSprite(stack, level));
        }
    }

    @Override
    public void submit(StorageFountainRenderState state, @Nonnull PoseStack poseStack, @Nonnull SubmitNodeCollector collector, @Nonnull CameraRenderState camera) {
        if (state.sprites.isEmpty()) {
            return;
        }
        // 强制至少 15 级方块光照，保留环境天空光
        int blockLight = Math.max(state.lightCoords & 0xFFFF, MIN_BLOCK_LIGHT);
        int light = (state.lightCoords & 0xFFFF0000) | blockLight;
        for (Direction side : SIDES) {
            renderSide(state.sprites, side, poseStack, collector, light);
        }
    }

    /**
     * 在指定方块面上按九宫格绘制已标记物品贴图
     */
    private void renderSide(List<TextureAtlasSprite> sprites, Direction face, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        // 绕 Y 旋转使目标面朝向 +Z（Y 方向不变，贴图保持竖直）
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPose(new Quaternionf().rotateY(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f));
        poseStack.translate(-0.5f, 0, -0.5f);
        // 在 +Z 面（z=1）按九宫格绘制，略高于面避免 z-fighting
        for (int i = 0; i < sprites.size(); i++) {
            TextureAtlasSprite sprite = sprites.get(i);
            if (sprite == null) {
                continue;
            }
            int col = i % 3;
            int row = i / 3;
            // 从四周边框内缩进 BORDER，避免贴图覆盖侧面的 1 像素边框
            float xCenter = BORDER + (col + 0.5f) * CELL;
            float yCenter = 1f - (BORDER + (row + 0.5f) * CELL); // 顶部开始，与 GUI 槽位顺序一致
            float x1 = xCenter - INSET;
            float x2 = xCenter + INSET;
            float y1 = yCenter - INSET;
            float y2 = yCenter + INSET;
            float z = 1.002f;
            final TextureAtlasSprite sp = sprite;
            final int l = light;
            collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
                Matrix4f mat = pose.pose();
                // 顶点顺序：左下、右下、右上、左上；V0=纹理顶部（对应 +Y），U0=纹理左侧
                addVertex(consumer, mat, x1, y1, z, sp.getU0(), sp.getV1(), l);
                addVertex(consumer, mat, x2, y1, z, sp.getU1(), sp.getV1(), l);
                addVertex(consumer, mat, x2, y2, z, sp.getU1(), sp.getV0(), l);
                addVertex(consumer, mat, x1, y2, z, sp.getU0(), sp.getV0(), l);
            });
        }
        poseStack.popPose();
    }

    /**
     * 取物品的粒子贴图精灵：26.x 走 {@code ItemModelResolver} 建一个 ItemStackRenderState 再挑粒子材质
     * （旧的 {@code ItemRenderer#getModel(...).getParticleIcon(...)} 已删除）。
     */
    @Nullable
    private TextureAtlasSprite getSprite(ItemStack stack, @Nullable Level level) {
        try {
            ItemStackRenderState renderState = new ItemStackRenderState();
            this.itemModelResolver.updateForTopItem(renderState, stack, ItemDisplayContext.NONE, level, null, 0);
            Material.Baked particle = renderState.pickParticleMaterial(RandomSource.create(0L));
            return particle == null ? null : particle.sprite();
        } catch (Throwable e) {
            return null;
        }
    }

    private void addVertex(VertexConsumer consumer, Matrix4f mat, float x, float y, float z, float u, float v, int light) {
        consumer.addVertex(mat, x, y, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 1);
    }

    /**
     * 渲染状态：与 GUI 1~9 槽一一对应的已标记物品粒子贴图（左上开始），解析失败的位置为 null
     */
    public static class StorageFountainRenderState extends BlockEntityRenderState {
        public final List<TextureAtlasSprite> sprites = new ArrayList<>();
    }
}
