package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.CommonEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AlltheimbaiumFarmlandBlock extends net.minecraft.world.level.block.FarmBlock implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(AlltheimbaiumFarmlandBlock.class);

    public AlltheimbaiumFarmlandBlock() {
        super(Properties.ofFullCopy(Blocks.FARMLAND));
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
                log.error("AlltheimbaiumFarmlandBlock.getTicker error", e);
            }
        };
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        //noinspection deprecation
        if (!level.hasChunkAt(tile.getBlockPos())) {
            return;
        }
        if (level.isClientSide) {
            return;
        }
        BlockPos pos = tile.getBlockPos().above();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof BonemealableBlock bonemealable) {
            bonemealable.performBonemeal((ServerLevel) level, level.random, pos, state);
        }
        if (!state.hasProperty(CropBlock.AGE)) {
            return;
        }
        if (block instanceof CropBlock crop) {
            int age = crop.getAge(state);
            int maxAge = crop.getMaxAge();
            if (age < maxAge) {
                state = state.setValue(CropBlock.AGE, maxAge);
                level.setBlock(pos, state, 2);
            }
        }
    }

    @Override
    public void tick(@Nonnull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource source) {
    }

    @Override
    public void randomTick(@NotNull BlockState state, @Nonnull ServerLevel level, @Nonnull BlockPos pos, @Nonnull RandomSource random) {
        try {
            if (state.getValue(MOISTURE) < 7) {
                level.setBlock(pos, state.setValue(MOISTURE, 7), 2);
            }
        } catch (Throwable e) {
            log.error("AlltheimbaiumFarmlandBlock.randomTick error", e);
        }
    }

    @Override
    public void fallOn(@NotNull Level level, @Nonnull BlockState state, @Nonnull BlockPos pos, @NotNull Entity entity, float fallDistance) {
        try {
            entity.causeFallDamage(fallDistance, 1.0F, level.damageSources().fall());
        } catch (Throwable e) {
            log.error("AlltheimbaiumFarmlandBlock.fallOn error", e);
        }
    }

    @Override
    public boolean isFertile(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        try {
            return state.getValue(MOISTURE) > 0;
        } catch (Throwable e) {
            log.error("AlltheimbaiumFarmlandBlock.isFertile error", e);
        }
        return super.isFertile(state, level, pos);
    }

    @Override
    public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        try {
            List<ItemStack> drops = new ArrayList<>();
            drops.add(new ItemStack(this));
            return drops;
        } catch (Throwable e) {
            log.error("AlltheimbaiumFarmlandBlock.getDrops error", e);
        }
        return super.getDrops(state, builder);
    }
}
