package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.ClockEntity;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import java.util.function.Supplier;

public class ClockBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(ClockBlock.class);
    private final Supplier<BlockEntityType<?>> entityTypeSupplier;
    private final int speedMultiplier;

    public ClockBlock(Properties properties, Supplier<BlockEntityType<?>> entityTypeSupplier, int speedMultiplier) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
        this.speedMultiplier = speedMultiplier;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new ClockEntity(pos, state, entityTypeSupplier, speedMultiplier);
    }

    @Nullable
    @Override
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
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof ClockEntity clock)) {
            return;
        }
        clock.tick(level);
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            ClockEntity clock = (ClockEntity) level.getBlockEntity(pos);
            if (clock == null) {
                return InteractionResult.FAIL;
            }
            boolean isActive = clock.isActive();
            clock.setActive(!isActive);
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.clock." + (isActive ? "disabled" : "enabled"), clock.getSpeedMultiplier()));
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("ClockBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }
}
