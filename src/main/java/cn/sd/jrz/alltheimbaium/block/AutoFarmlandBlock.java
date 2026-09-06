package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * ATI 自动耕地（资源农场式）。
 * <p>
 * 无能量、无标记槽：上方种作物后每 tick 由方块实体催熟并“收获×等级效率”；
 * 侧/底面空手右键打开 GUI（顶面右击仍用于种植）。保留 15/16 低块与可种 CROP/藤蔓。
 */
public class AutoFarmlandBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(AutoFarmlandBlock.class);
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);

    public AutoFarmlandBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public VoxelShape getShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override
    @Nonnull
    public VoxelShape getCollisionShape(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new AutoFarmlandEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                if (tile instanceof AutoFarmlandEntity machine) {
                    machine.tickServer();
                }
            } catch (Throwable e) {
                log.error("AutoFarmlandBlock.getTicker error", e);
            }
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    @Nonnull
    public InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (hit.getDirection() == Direction.UP) {
                return InteractionResult.PASS; // 顶面留给种植
            }
            if (level.getBlockEntity(pos) instanceof AutoFarmlandEntity entity && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, entity, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("AutoFarmlandBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }

    @Override
    public boolean canSustainPlant(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull Direction facing, @Nonnull IPlantable plantable) {
        try {
            if (plantable instanceof SaplingBlock) {
                return false;
            }
            var type = plantable.getPlantType(level, pos.relative(facing));
            return type == PlantType.CROP || plantable.getPlant(level, pos.relative(facing)).getBlock() instanceof StemBlock;
        } catch (Throwable e) {
            log.error("AutoFarmlandBlock.canSustainPlant error", e);
        }
        return super.canSustainPlant(state, level, pos, facing, plantable);
    }
}
