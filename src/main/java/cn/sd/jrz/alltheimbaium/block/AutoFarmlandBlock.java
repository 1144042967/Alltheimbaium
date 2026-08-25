package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.AutoFarmlandEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * ATI 自动耕地。
 * <p>
 * 每 tick 将上方作物催熟并模拟收获（原版掉落），收获物存入 27 槽容器（满则暂停收获）。
 * 从上表面右击不会打开 GUI，只能从侧面或底面打开；容器内物品允许管道抽取。
 */
public class AutoFarmlandBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(AutoFarmlandBlock.class);
    /** 15/16 高度碰撞盒，与耕地一致 */
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 15.0D, 16.0D);
    // 从配置文件加载的本地缓存值，由 Config.onConfigLoad() 在配置加载后调用 loadConfig() 填入
    static int harvestCost;

    /**
     * 由 Config.onConfigLoad() 在配置文件加载完成后调用
     */
    public static void loadConfig() {
        harvestCost = Config.AUTO_FARMLAND_HARVEST_COST.get();
    }

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
                tick(l, tile);
            } catch (Throwable e) {
                log.error("AutoFarmlandBlock.getTicker error", e);
            }
        };
    }

    /** 每 tick：催熟上方作物 + 模拟收获入容器（容器满则暂停收获，作物保持成熟） */
    @SuppressWarnings("deprecation")
    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof AutoFarmlandEntity generator)) {
            return;
        }
        // 向下输出：开启时把容器物品自动推入下方容器（不依赖上方是否有作物）
        generator.pushDownToContainer();
        BlockPos cropPos = generator.getBlockPos().above();
        BlockState cropState = level.getBlockState(cropPos);
        if (!(cropState.getBlock() instanceof CropBlock crop)) {
            return;
        }
        // 催熟到最大阶段
        if (crop.getAge(cropState) < crop.getMaxAge()) {
            cropState = cropState.setValue(CropBlock.AGE, crop.getMaxAge());
            level.setBlock(cropPos, cropState, 2);
        }
        // 成熟后模拟收获：消耗 FE，收获后不降低作物种植阶段（作物保持成熟，持续收获）
        if (crop.getAge(cropState) >= crop.getMaxAge() && level instanceof ServerLevel serverLevel) {
            if (generator.energy.getEnergyStored() >= harvestCost) {
                List<ItemStack> drops = crop.getDrops(cropState, serverLevel, cropPos, null);
                boolean allInserted = true;
                for (ItemStack drop : drops) {
                    ItemStack remain = ItemHandlerHelper.insertItemStacked(generator.storage, drop, false);
                    if (!remain.isEmpty()) {
                        allInserted = false;
                    }
                }
                if (allInserted) {
                    // 收获成功才消耗 FE；容器满则本次不消耗、不收获（作物保持成熟等待下次）
                    generator.energy.extractEnergy(harvestCost, false);
                    generator.setChanged();
                }
            }
        }
    }

    /**
     * 右击：从上表面不打开 GUI，从侧面/底面打开箱子界面。
     */
    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            // 从上表面右击与耕地一致：不打开 GUI，返回 PASS 让物品 useOn 正常放置种子/方块
            if (hit.getDirection() == Direction.UP) {
                return InteractionResult.PASS;
            }
            // 从侧面/底面打开 GUI
            if (level.getBlockEntity(pos) instanceof AutoFarmlandEntity entity && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, entity, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("AutoFarmlandBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }

    /**
     * 允许种植：仅作物与藤蔓，禁止树苗（防止树长大破坏耕地）。
     */
    @Override
    public boolean canSustainPlant(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull Direction facing, @Nonnull IPlantable plantable) {
        try {
            if (plantable instanceof SaplingBlock) {
                return false;
            }
            BlockState plant = plantable.getPlant(level, pos.relative(facing));
            var type = plantable.getPlantType(level, pos.relative(facing));
            return type == PlantType.CROP || plant.getBlock() instanceof StemBlock;
        } catch (Throwable e) {
            log.error("AutoFarmlandBlock.canSustainPlant error", e);
        }
        return super.canSustainPlant(state, level, pos, facing, plantable);
    }

    /**
     * 破坏时掉落自身 + 容器内所有物品。
     */
    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        try {
            if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AutoFarmlandEntity entity) {
                for (int i = 0; i < entity.storage.getSlots(); i++) {
                    ItemStack stack = entity.storage.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        drops.add(stack);
                    }
                }
            }
        } catch (Throwable e) {
            log.error("AutoFarmlandBlock.getDrops error", e);
        }
        return drops;
    }
}
