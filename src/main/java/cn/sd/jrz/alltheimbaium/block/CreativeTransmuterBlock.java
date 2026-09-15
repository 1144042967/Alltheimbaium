package cn.sd.jrz.alltheimbaium.block;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootParams;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
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
        if (level.isClientSide()) {
            return;
        }
        if (!(tile instanceof CreativeTransmuterEntity entity)) {
            return;
        }
        entity.serverTick();
    }

    @SuppressWarnings("deprecation")
    @Nonnull
    private InteractionResult doUse(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof CreativeTransmuterEntity entity)) {
                return InteractionResult.FAIL;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(entity, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("CreativeTransmuterBlock.use error", e);
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
    protected @Nonnull InteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        InteractionResult result = doUse(state, level, pos, player, handIn, hit);
        // 26.x：InteractionResult 是 sealed 接口，判定改用 instanceof；
        // 旧的 ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION 对应 TRY_WITH_EMPTY_HAND（表示"再试一次空手交互"）
        if (result instanceof InteractionResult.Success) {
            return InteractionResult.SUCCESS;
        }
        if (result instanceof InteractionResult.Fail) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
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