package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 通用资源农场方块实体渲染器：在方块四个侧面（北/南/东/西）中央绘制当前标记物的大图标（运行时叠加）。
 * 无标记时不绘制。贴图取自物品粒子贴图，画为 +Z 面的一块平面四边形，强制 ≥15 级方块光照。
 * <p>
 * 26.x：state-based 渲染模型；自定义几何走 {@link SubmitNodeCollector#submitCustomGeometry}，
 * 渲染类型为 {@link RenderTypes#cutoutMovingBlock()}，贴图在 extractRenderState 阶段解析好放进渲染状态。
 */
public class ResourceFarmRenderer implements BlockEntityRenderer<ResourceFarmEntity, ResourceFarmRenderer.ResourceFarmRenderState> {
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    /** 图标半边长：占侧面内窗（0.25~0.75）正中央，左右/上下对称居中 */
    private static final float INSET = 0.21f;
    private static final int MIN_BLOCK_LIGHT = 15 << 4;
    /** 使用方块图集的 cutout 渲染类型（与方块本体共用方块图集） */
    private static final RenderType RENDER_TYPE = RenderTypes.cutoutMovingBlock();

    private final ItemModelResolver itemModelResolver;

    public ResourceFarmRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    @Nonnull
    public ResourceFarmRenderState createRenderState() {
        return new ResourceFarmRenderState();
    }

    @Override
    public void extractRenderState(@Nonnull ResourceFarmEntity entity, @Nonnull ResourceFarmRenderState state, float partialTick,
                                   @Nonnull Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(entity, state, crumblingOverlay);
        state.sprite = null;
        Item marker = entity.getMarkerItem();
        if (marker == null) {
            return;
        }
        ItemStack stack = new ItemStack(marker);
        if (stack.isEmpty()) {
            return;
        }
        state.sprite = getSprite(stack, entity.getLevel());
    }

    @Override
    public void submit(ResourceFarmRenderState state, @Nonnull PoseStack poseStack, @Nonnull SubmitNodeCollector collector, @Nonnull CameraRenderState camera) {
        TextureAtlasSprite sprite = state.sprite;
        if (sprite == null) {
            return;
        }
        // 强制至少 15 级方块光照，保留环境天空光
        int blockLight = Math.max(state.lightCoords & 0xFFFF, MIN_BLOCK_LIGHT);
        int light = (state.lightCoords & 0xFFFF0000) | blockLight;
        for (Direction side : SIDES) {
            renderSide(sprite, side, poseStack, collector, light);
        }
    }

    private void renderSide(TextureAtlasSprite sprite, Direction face, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        // 绕 Y 旋转使目标面朝向 +Z（Y 方向不变，贴图保持竖直）
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPose(new Quaternionf().rotateY(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f));
        poseStack.translate(-0.5f, 0, -0.5f);
        float x1 = 0.5f - INSET;
        float x2 = 0.5f + INSET;
        float y1 = 0.5f - INSET;
        float y2 = 0.5f + INSET;
        float z = 1.002f;
        final TextureAtlasSprite sp = sprite;
        final int l = light;
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, consumer) -> {
            Matrix4f mat = pose.pose();
            addVertex(consumer, mat, x1, y1, z, sp.getU0(), sp.getV1(), l);
            addVertex(consumer, mat, x2, y1, z, sp.getU1(), sp.getV1(), l);
            addVertex(consumer, mat, x2, y2, z, sp.getU1(), sp.getV0(), l);
            addVertex(consumer, mat, x1, y2, z, sp.getU0(), sp.getV0(), l);
        });
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
     * 渲染状态：保存标记物的粒子贴图（解析失败为 null，不绘制）
     */
    public static class ResourceFarmRenderState extends BlockEntityRenderState {
        public @Nullable TextureAtlasSprite sprite;
    }
}
