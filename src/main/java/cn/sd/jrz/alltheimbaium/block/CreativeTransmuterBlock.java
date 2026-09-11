package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.CreativeTransmuterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ATI 创造物品质变器：布局参考工作台，3×3 输入栏每格只存 1 个材料，
 * 九格填满同一种材料时自动转化为 1 个产物放进输出栏。输入输出都会留在机器里，
 * 两者都可接管道（输入栏进、输出栏出）。详见 {@link CreativeTransmuterEntity}。
 */
public class CreativeTransmuterBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(CreativeTransmuterBlock.class);

    public CreativeTransmuterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new CreativeTransmuterEntity(pos, state);
    }

    /** 服务端 tick：检查九格输入是否凑齐一份配方；客户端不参与 */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                tick(l, tile);
            } catch (Throwable e) {
                log.error("CreativeTransmuterBlock.getTicker error", e);
            }
        };
    }

    private <T extends BlockEntity> void tick(@Nonnull Level level, @Nonnull T tile) {
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof CreativeTransmuterEntity entity)) {
            return;
        }
        entity.serverTick();
    }

    @SuppressWarnings("deprecation")
    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof CreativeTransmuterEntity entity)) {
                return InteractionResult.FAIL;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, entity, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("CreativeTransmuterBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }
}
