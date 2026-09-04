package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 生物农场 BER：把收容的生物实体（客户端懒加载、不属于世界）缩小居中渲染在玻璃罐内。
 * 参考 Ars-Nouveau MobJarRenderer，本模组方块无朝向，去掉 FACING 旋转。
 */
@OnlyIn(Dist.CLIENT)
public class MobFarmRenderer implements BlockEntityRenderer<MobFarmEntity> {
    private final EntityRenderDispatcher entityRenderer;

    public MobFarmRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.getEntityRenderer();
    }

    @Override
    public void render(MobFarmEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Entity entity = blockEntity.getClientRenderedEntity();
        if (entity == null) {
            return;
        }
        try {
            // 基础缩放：罐内容积约 0.7 格，超出 1 格的生物按体型继续缩小
            float scale = 0.53125F;
            float size = Math.max(entity.getBbWidth(), entity.getBbHeight());
            if (size > 1.0F) {
                scale /= size;
            }
            poseStack.pushPose();
            // 居中 + 微微抬离罐底
            poseStack.translate(0.5, 0.42, 0.5);
            poseStack.scale(scale, scale, scale);
            entity.setDeltaMovement(0, 0, 0);
            // 冻结插值，避免在罐内抖动
            entity.xo = entity.getX();
            entity.yo = entity.getY();
            entity.zo = entity.getZ();
            entity.xRotO = entity.getXRot();
            entity.yRotO = entity.getYRot();
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                living.yBodyRotO = living.yBodyRot;
                living.yHeadRotO = living.yHeadRot;
            }
            this.entityRenderer.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, poseStack, bufferSource, packedLight);
            for (Entity passenger : entity.getPassengers()) {
                this.entityRenderer.render(passenger, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, poseStack, bufferSource, packedLight);
            }
            poseStack.popPose();
        } catch (Throwable e) {
            // 渲染失败忽略，避免刷屏
        }
    }
}
