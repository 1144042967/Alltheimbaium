package cn.sd.jrz.alltheimbaium.block;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootParams;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import cn.sd.jrz.alltheimbaium.entity.ResourceFarmEntity;
import cn.sd.jrz.alltheimbaium.gui.ResourceFarmMenu;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.ResourceData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
 * 通用资源农场方块。放置后右键（服务端）打开 GUI；标记在 GUI 标记槽完成。
 */
public class ResourceFarmBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(ResourceFarmBlock.class);

    public ResourceFarmBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ResourceFarmEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                if (tile instanceof ResourceFarmEntity machine) {
                    machine.tickServer();
                }
            } catch (Throwable e) {
                log.error("ResourceFarmBlock.getTicker error", e);
            }
        };
    }

    @SuppressWarnings("deprecation")
    @Nonnull
    private InteractionResult doUse(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level.getBlockEntity(pos) instanceof ResourceFarmEntity machine)) {
                return InteractionResult.FAIL;
            }
            if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel) {
                // 随开屏 extraData 下发"标记→产物"帮助行供 GUI 展示。
                // 裁剪用的是解码端同一组常量（见 ResourceFarmMenu.HELP_MAX_*），两端必须一致。
                serverPlayer.openMenu(machine, buf -> {
                    buf.writeBlockPos(pos);
                    int[][] rows = ResourceData.helpRows();
                    int rowCount = Math.min(rows.length, ResourceFarmMenu.HELP_MAX_ROWS);
                    if (rowCount < rows.length) {
                        log.warn("资源农场帮助表 {} 行超过上限 {}，多出的 {} 行不会下发给客户端",
                                rows.length, ResourceFarmMenu.HELP_MAX_ROWS, rows.length - rowCount);
                    }
                    buf.writeVarInt(rowCount);
                    for (int r = 0; r < rowCount; r++) {
                        int[] row = rows[r];
                        int productCount = Math.min(row.length - 1, ResourceFarmMenu.HELP_MAX_PRODUCTS_PER_ROW);
                        buf.writeVarInt(row[0]);            // 标记物 itemId
                        buf.writeVarInt(productCount);      // 产物个数
                        for (int i = 1; i <= productCount; i++) {
                            buf.writeVarInt(row[i]);
                        }
                    }
                });
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("ResourceFarmBlock.use error", e);
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