package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * 通用资源农场方块实体渲染器：在方块四个侧面（北/南/东/西）中央绘制当前标记物的大图标（运行时叠加）。
 * 无标记时不绘制。贴图取自物品粒子贴图，画为 +Z 面的一块平面四边形，强制 ≥15 级方块光照。
 */
@OnlyIn(Dist.CLIENT)
public class ResourceFarmRenderer implements BlockEntityRenderer<ResourceFarmEntity> {
    private static final float[] SIDE_ROT_Y = {0, 0, 2, 0, 3, 1};
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    /** 图标半边长：占侧面内窗（0.25~0.75）正中央，左右/上下对称居中 */
    private static final float INSET = 0.21f;
    private static final int MIN_BLOCK_LIGHT = 15 << 4;

    public ResourceFarmRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(@Nonnull ResourceFarmEntity entity, float partialTick, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        Item marker = entity.getMarkerItem();
        if (marker == null) {
            return;
        }
        ItemStack stack = new ItemStack(marker);
        if (stack.isEmpty()) {
            return;
        }
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        int blockLight = Math.max(combinedLight & 0xFFFF, MIN_BLOCK_LIGHT);
        int light = (combinedLight & 0xFFFF0000) | blockLight;
        for (Direction side : SIDES) {
            renderSide(entity, stack, side, poseStack, buffer, light, combinedOverlay);
        }
    }

    private void renderSide(ResourceFarmEntity entity, ItemStack stack, Direction face, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPoseMatrix((new Matrix4f()).rotateYXZ(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f, 0, 0));
        poseStack.translate(-0.5f, 0, -0.5f);
        TextureAtlasSprite sprite = getSprite(entity, stack);
        if (sprite == null) {
            poseStack.popPose();
            return;
        }
        float x1 = 0.5f - INSET;
        float x2 = 0.5f + INSET;
        float y1 = 0.5f - INSET;
        float y2 = 0.5f + INSET;
        float z = 1.002f;
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        Matrix4f mat = poseStack.last().pose();
        addVertex(consumer, mat, x1, y1, z, sprite.getU0(), sprite.getV1(), light, overlay);
        addVertex(consumer, mat, x2, y1, z, sprite.getU1(), sprite.getV1(), light, overlay);
        addVertex(consumer, mat, x2, y2, z, sprite.getU1(), sprite.getV0(), light, overlay);
        addVertex(consumer, mat, x1, y2, z, sprite.getU0(), sprite.getV0(), light, overlay);
        poseStack.popPose();
    }

    @SuppressWarnings("deprecation")
    private TextureAtlasSprite getSprite(ResourceFarmEntity entity, ItemStack stack) {
        try {
            BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, entity.getLevel(), null, 0);
            TextureAtlasSprite sprite = model.getParticleIcon(ModelData.EMPTY);
            if (sprite == null) {
                return null;
            }
            return Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).getSprite(sprite.contents().name());
        } catch (Throwable e) {
            return null;
        }
    }

    private void addVertex(VertexConsumer consumer, Matrix4f mat, float x, float y, float z, float u, float v, int light, int overlay) {
        consumer.vertex(mat, x, y, z).color(1.0f, 1.0f, 1.0f, 1.0f).uv(u, v).overlayCoords(overlay).uv2(light).normal(0, 0, 1).endVertex();
    }
}
