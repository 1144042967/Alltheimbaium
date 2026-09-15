package cn.sd.jrz.alltheimbaium.gui;

import cn.sd.jrz.alltheimbaium.entity.MobFarmEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 生物农场 BER：把收容的生物实体（客户端懒加载、不属于世界）缩小居中渲染在玻璃罐内。
 * 参考 Ars-Nouveau MobJarRenderer，本模组方块无朝向，去掉 FACING 旋转。
 * <p>
 * 26.x：实体渲染同样拆成两段——extractRenderState 里用 {@code EntityRenderDispatcher#extractEntity}
 * 抽出（不含世界的）实体渲染状态，submit 里再 {@code EntityRenderDispatcher#submit} 提交。
 */
public class MobFarmRenderer implements BlockEntityRenderer<MobFarmEntity, MobFarmRenderer.MobFarmRenderState> {
    private final EntityRenderDispatcher entityRenderer;

    public MobFarmRenderer(BlockEntityRendererProvider.Context context) {
        this.entityRenderer = context.entityRenderer();
    }

    @Override
    @Nonnull
    public MobFarmRenderState createRenderState() {
        return new MobFarmRenderState();
    }

    @Override
    public void extractRenderState(@Nonnull MobFarmEntity blockEntity, @Nonnull MobFarmRenderState state, float partialTick,
                                   @Nonnull Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumblingOverlay);
        state.entity = null;
        state.passengers.clear();
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
            state.scale = scale;
            state.entity = this.entityRenderer.extractEntity(entity, partialTick);
            for (Entity passenger : entity.getPassengers()) {
                state.passengers.add(this.entityRenderer.extractEntity(passenger, partialTick));
            }
        } catch (Throwable e) {
            // 渲染失败忽略，避免刷屏
            state.entity = null;
            state.passengers.clear();
        }
    }

    @Override
    public void submit(MobFarmRenderState state, @Nonnull PoseStack poseStack, @Nonnull SubmitNodeCollector collector, @Nonnull CameraRenderState camera) {
        if (state.entity == null) {
            return;
        }
        poseStack.pushPose();
        // 水平居中；把脚部落在罐底上方一点，让生物整体待在方块内部
        poseStack.translate(0.5, 0.08, 0.5);
        poseStack.scale(state.scale, state.scale, state.scale);
        this.entityRenderer.submit(state.entity, camera, 0.0, 0.0, 0.0, poseStack, collector);
        for (EntityRenderState passenger : state.passengers) {
            this.entityRenderer.submit(passenger, camera, 0.0, 0.0, 0.0, poseStack, collector);
        }
        poseStack.popPose();
    }

    /**
     * 渲染状态：收容生物（及其乘客）的实体渲染状态 + 缩放
     */
    public static class MobFarmRenderState extends BlockEntityRenderState {
        public @Nullable EntityRenderState entity;
        public final List<EntityRenderState> passengers = new ArrayList<>();
        public float scale = 1.0F;
    }
}
