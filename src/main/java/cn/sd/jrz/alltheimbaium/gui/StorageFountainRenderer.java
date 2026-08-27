package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;

/**
 * 存储方块制造机的方块实体渲染器：在方块四个侧面（北/南/东/西）按九宫格（3×3）绘制已标记物品的贴图。
 * <p>
 * 参考 auto-resource 的方块生成机渲染器：取物品的粒子贴图用 cutout 画平面四边形（共用方块图集，避免首帧贴图未加载），
 * 强制至少 15 级方块光照。九宫格位置与 GUI 中 1~9 槽一一对应（左上开始）。
 */
@OnlyIn(Dist.CLIENT)
public class StorageFountainRenderer implements BlockEntityRenderer<StorageFountainEntity> {
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

    public StorageFountainRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void render(@Nonnull StorageFountainEntity entity, float partialTick, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (entity.itemList.isEmpty()) {
            return;
        }
        // 显式绑定方块纹理图集（与物品 GUI 渲染 renderGuiItem 一致）
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        // 强制至少 15 级方块光照，保留环境天空光
        int blockLight = Math.max(combinedLight & 0xFFFF, MIN_BLOCK_LIGHT);
        int light = (combinedLight & 0xFFFF0000) | blockLight;
        for (Direction side : SIDES) {
            renderSide(entity, side, poseStack, buffer, light, combinedOverlay);
        }
    }

    /**
     * 在指定方块面上按九宫格绘制已标记物品贴图
     */
    private void renderSide(StorageFountainEntity entity, Direction face, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        poseStack.pushPose();
        // 绕 Y 旋转使目标面朝向 +Z（Y 方向不变，贴图保持竖直）
        poseStack.translate(0.5f, 0, 0.5f);
        poseStack.mulPoseMatrix((new Matrix4f()).rotateYXZ(SIDE_ROT_Y[face.ordinal()] * 90f * (float) Math.PI / 180f, 0, 0));
        poseStack.translate(-0.5f, 0, -0.5f);
        // 在 +Z 面（z=1）按九宫格绘制，略高于面避免 z-fighting
        for (int i = 0; i < entity.itemList.size(); i++) {
            ItemStack stack = entity.itemList.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            TextureAtlasSprite sprite = getSprite(entity, stack);
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
            VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
            Matrix4f mat = poseStack.last().pose();
            // 顶点顺序：左下、右下、右上、左上；V0=纹理顶部（对应 +Y），U0=纹理左侧
            addVertex(consumer, mat, x1, y1, z, sprite.getU0(), sprite.getV1(), light, overlay);
            addVertex(consumer, mat, x2, y1, z, sprite.getU1(), sprite.getV1(), light, overlay);
            addVertex(consumer, mat, x2, y2, z, sprite.getU1(), sprite.getV0(), light, overlay);
            addVertex(consumer, mat, x1, y2, z, sprite.getU0(), sprite.getV0(), light, overlay);
        }
        poseStack.popPose();
    }

    /**
     * 获取物品的粒子贴图（同时支持方块物品与普通物品），并强制从方块图集重新解析确保首帧已加载
     */
    @SuppressWarnings("deprecation")
    private TextureAtlasSprite getSprite(StorageFountainEntity entity, ItemStack stack) {
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
