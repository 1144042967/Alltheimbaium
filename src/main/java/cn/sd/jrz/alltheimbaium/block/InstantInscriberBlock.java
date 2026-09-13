package cn.sd.jrz.alltheimbaium.block;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootParams;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import cn.sd.jrz.alltheimbaium.entity.InstantInscriberEntity;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ATI 零刻压印器方块：读取 AE2 压印机配方，压板/组装双模式即时生成（消耗 FE）。
 * 右键打开 GUI；tick 仅在服务端把输出行产物转给启用"推送"的相邻面。
 */
public class InstantInscriberBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(InstantInscriberBlock.class);

    public InstantInscriberBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new InstantInscriberEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                if (tile instanceof InstantInscriberEntity inscriber) {
                    inscriber.serverTick();
                }
            } catch (Throwable e) {
                log.error("InstantInscriberBlock.getTicker error", e);
            }
        };
    }

    @SuppressWarnings("deprecation")
    private InteractionResult doUse(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (!(level.getBlockEntity(pos) instanceof InstantInscriberEntity inscriber)) {
                return InteractionResult.FAIL;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(inscriber, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("InstantInscriberBlock.use error", e);
        }
        return InteractionResult.PASS;
    }

    /**
     * 1.21：原版的 Block#use 拆成了空手的 useWithoutItem 与持物的 useItemOn，
     * 这里两个都覆写并统一转到 doUse，行为与 1.20.1 保持一致。
     */
    @Override
    protected @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        return doUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    }

    @Override
    protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        InteractionResult result = doUse(state, level, pos, player, handIn, hit);
        if (result == InteractionResult.SUCCESS) {
            return ItemInteractionResult.SUCCESS;
        }
        if (result == InteractionResult.FAIL) {
            return ItemInteractionResult.FAIL;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * 1.21：掉落时把方块实体数据写进物品的 block_entity_data 组件（替代 1.20.1 战利品表的 copy_nbt）。
     */
    @Override
    @Nonnull
    public List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder params) {
        return Tool.withBlockEntityData(super.getDrops(state, params), params);
    }
}