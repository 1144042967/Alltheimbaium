package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.CommonEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ClockBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(ClockBlock.class);
    private final Supplier<BlockEntityType<?>> entityTypeSupplier;
    private static boolean active = false;
    private final int speedMultiplier;

    public ClockBlock(Properties properties, Supplier<BlockEntityType<?>> entityTypeSupplier, int speedMultiplier) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
        this.speedMultiplier = speedMultiplier;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new CommonEntity(pos, state, entityTypeSupplier);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                tick(l, tile);
            } catch (Throwable e) {
                log.error("ClockBlock.getTicker error", e);
            }
        };
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (!active || level.isClientSide) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos pos = tile.getBlockPos().relative(direction);
            BlockState blockState = level.getBlockState(pos);
            Block block = blockState.getBlock();
            if (level instanceof ServerLevel && blockState.isRandomlyTicking()) {
                blockState.randomTick((ServerLevel) level, pos, level.getRandom());
            }
            if (block instanceof ClockBlock || block instanceof AlltheimbaiumFarmlandBlock) {
                continue;
            }
            if (!(block instanceof EntityBlock entityBlock)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                //noinspection rawtypes
                BlockEntityTicker ticker = entityBlock.getTicker(level, blockState, blockEntity.getType());
                if (blockEntity.isRemoved() || ticker == null) {
                    continue;
                }
                for (int i = 1; i < speedMultiplier; i++) {
                    if (blockEntity.isRemoved()) {
                        break;
                    }
                    //noinspection unchecked
                    ticker.tick(level, pos, blockState, blockEntity);
                }
            }
            BlockEntity aboveEntity = level.getBlockEntity(pos);
            if (aboveEntity != null) {
                aboveEntity.setChanged();
            }
        }
    }

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            ClockBlock.active = !ClockBlock.active;
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.clock." + (ClockBlock.active ? "enabled" : "disabled"), speedMultiplier));
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("ClockBlock.useWithoutItem error", e);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack stack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand hand, @Nonnull BlockHitResult hitResult) {
        try {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            ClockBlock.active = !ClockBlock.active;
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.clock." + (ClockBlock.active ? "enabled" : "disabled"), speedMultiplier));
            return ItemInteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("ClockBlock.useItemOn error", e);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        try {
            List<ItemStack> drops = new ArrayList<>();
            drops.add(new ItemStack(this));
            return drops;
        } catch (Throwable e) {
            log.error("ClockBlock.getDrops error", e);
        }
        return super.getDrops(state, builder);
    }
}
