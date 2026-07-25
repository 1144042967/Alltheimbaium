package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LiquidFountainBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainBlock.class);
    private static final Direction[] DIRECTIONS = Direction.values();
    public static final Integer MAX = 10000 * 1000;

    public LiquidFountainBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new LiquidFountainEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                tick(l, tile);
            } catch (Throwable e) {
                log.error("LiquidFountainBlock.getTicker error", e);
            }
        };
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof LiquidFountainEntity generator)) {
            return;
        }
        // 检查命名空间，MI/EI mod 的流体直接设为无限
        if (!generator.stack.isEmpty()) {
            String namespace = BuiltInRegistries.FLUID.getKey(generator.stack.getFluid()).getNamespace();
            if (namespace.contains("modern_industrialization") || namespace.contains("extended_industrialization")) {
                generator.stack.setAmount(Integer.MAX_VALUE);
            }
        }
        if (generator.stack.isEmpty() || generator.stack.getAmount() < LiquidFountainBlock.MAX) {
            if (!generator.stack.isEmpty() && generator.stack.getAmount() <= 0) {
                generator.stack = FluidStack.EMPTY;
            }
            return;
        }
        generator.stack.setAmount(Integer.MAX_VALUE);
        BlockPos blockPos = generator.getBlockPos();
        // 传输
        for (Direction direction : DIRECTIONS) {
            BlockPos pos = blockPos.relative(direction);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction.getOpposite());
            if (handler == null) {
                continue;
            }
            FluidStack stack = generator.stack.copy();
            stack.setAmount(Integer.MAX_VALUE);
            handler.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        }
        generator.setChanged();
    }

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return use(level, pos, player);
        } catch (Throwable e) {
            log.error("LiquidFountainBlock.useWithoutItem error", e);
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected @Nonnull ItemInteractionResult useItemOn(@Nonnull ItemStack itemStack, @Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return ItemInteractionResult.SUCCESS;
            }
            InteractionResult result = use(level, pos, player);
            return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        } catch (Throwable e) {
            log.error("LiquidFountainBlock.useItemOn error", e);
        }
        return super.useItemOn(itemStack, state, level, pos, player, handIn, hit);
    }

    private @Nonnull InteractionResult use(Level level, @Nonnull BlockPos pos, @Nonnull Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        LiquidFountainEntity generator = (LiquidFountainEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        FluidStack stack = generator.stack;
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.liquid.fountain.empty"));
            return InteractionResult.SUCCESS;
        }
        if (stack.getAmount() < LiquidFountainBlock.MAX) {
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.liquid.fountain.current", stack.getHoverName(), stack.getAmount(), LiquidFountainBlock.MAX));
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(Component.translatable("screen.alltheimbaium.liquid.fountain.max", stack.getHoverName()));
        return InteractionResult.SUCCESS;
    }
}
