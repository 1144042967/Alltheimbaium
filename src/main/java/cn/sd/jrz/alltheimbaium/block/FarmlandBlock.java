package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.CommonEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.PlantType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class FarmlandBlock extends net.minecraft.world.level.block.FarmBlock implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(FarmlandBlock.class);

    // 从配置文件加载的本地缓存值，由 Config.onConfigLoad() 在配置加载后调用 loadConfig() 填入
    static int tickInterval;
    static int growthAmount;
    static boolean bonemealEnabled;
    static int bonemealInterval;

    public static void loadConfig() {
        tickInterval = Config.FARMLAND_TICK_INTERVAL.get();
        growthAmount = Config.FARMLAND_GROWTH_AMOUNT.get();
        bonemealEnabled = Config.FARMLAND_BONEMEAL_ENABLED.get();
        bonemealInterval = Config.FARMLAND_BONEMEAL_INTERVAL.get();
    }

    public FarmlandBlock() {
        super(Properties.copy(Blocks.FARMLAND));
    }

    @Override
    public boolean canSurvive(@Nonnull BlockState state, @Nonnull LevelReader reader, @Nonnull BlockPos pos) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CommonEntity(pos, state, Registration.FARMLAND_ENTITY::get);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                tick(l, tile);
            } catch (Throwable e) {
                log.error("FarmlandBlock.getTicker error", e);
            }
        };
    }

    private int tickCounter;

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (!level.hasChunkAt(tile.getBlockPos())) {
            return;
        }
        if (level.isClientSide) {
            return;
        }

        int interval = tickInterval;
        tickCounter++;
        if (tickCounter % interval != 0) {
            return;
        }

        BlockPos pos = tile.getBlockPos().above();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        // 骨粉效果
        if (bonemealEnabled && tickCounter % bonemealInterval == 0) {
            if (block instanceof BonemealableBlock bonemealable) {
                bonemealable.performBonemeal((ServerLevel) level, level.random, pos, state);
            }
        }

        // 作物生长
        if (!state.hasProperty(CropBlock.AGE)) {
            return;
        }
        if (block instanceof CropBlock crop) {
            int age = crop.getAge(state);
            int maxAge = crop.getMaxAge();
            if (age < maxAge) {
                int growthAmount = FarmlandBlock.growthAmount;
                int newAge;
                if (growthAmount == -1) {
                    newAge = maxAge;
                } else {
                    newAge = Math.min(age + growthAmount, maxAge);
                }
                state = state.setValue(CropBlock.AGE, newAge);
                level.setBlock(pos, state, 2);
                ForgeHooks.onCropsGrowPost(level, pos, state);
            }
        }
    }

    @Override
    public void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource source) {
    }

    @Override
    public void randomTick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        try {
            if (state.getValue(MOISTURE) < 7) {
                level.setBlock(pos, state.setValue(MOISTURE, 7), 2);
            }
        } catch (Throwable e) {
            log.error("FarmlandBlock.randomTick error", e);
        }
    }

    @Override
    public void fallOn(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockPos pos, @Nonnull Entity entity, float fallDistance) {
        try {
            entity.causeFallDamage(fallDistance, 1.0F, level.damageSources().fall());
        } catch (Throwable e) {
            log.error("FarmlandBlock.fallOn error", e);
        }
    }

    @Override
    public boolean canSustainPlant(@Nonnull BlockState state, @Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nonnull Direction facing, @Nonnull IPlantable plantable) {
        try {
            BlockState plant = plantable.getPlant(level, pos.relative(facing));
            var type = plantable.getPlantType(level, pos.relative(facing));
            return type == PlantType.CROP || plant.getBlock() instanceof StemBlock;
        } catch (Throwable e) {
            log.error("FarmlandBlock.canSustainPlant error", e);
        }
        return super.canSustainPlant(state, level, pos, facing, plantable);
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        try {
            return state.getValue(MOISTURE) > 0;
        } catch (Throwable e) {
            log.error("FarmlandBlock.isFertile error", e);
        }
        return super.isFertile(state, level, pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        try {
            List<ItemStack> drops = new ArrayList<>();
            drops.add(new ItemStack(this));
            return drops;
        } catch (Throwable e) {
            log.error("FarmlandBlock.getDrops error", e);
        }
        return super.getDrops(state, builder);
    }
}
