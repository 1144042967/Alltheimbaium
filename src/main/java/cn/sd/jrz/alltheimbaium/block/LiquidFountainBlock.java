package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class LiquidFountainBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(LiquidFountainBlock.class);

    // 从配置文件加载的本地缓存值，由 Config.onConfigLoad() 在配置加载后调用 loadConfig() 填入
    private static long infiniteThreshold;
    private static List<? extends String> autoInfiniteMods;

    /**
     * 由 Config.onConfigLoad() 在配置文件加载完成后调用
     */
    public static void loadConfig() {
        infiniteThreshold = Config.LIQUID_FOUNTAIN_INFINITE_THRESHOLD.get();
        autoInfiniteMods = Config.LIQUID_FOUNTAIN_AUTO_INFINITE_MODS.get();
    }

    public static long getMax() {
        return infiniteThreshold;
    }

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
        // 检查命名空间，配置文件 auto_infinite_mods 列表中的 MOD 流体直接设为无限
        if (generator.stack != FluidStack.EMPTY) {
            String namespace = ForgeRegistries.FLUIDS.getKey(generator.stack.getFluid()).getNamespace();
            for (String mod : autoInfiniteMods) {
                if (namespace.contains(mod)) {
                    generator.stack.setAmount(Integer.MAX_VALUE);
                    break;
                }
            }
        }
        if (generator.stack == FluidStack.EMPTY || generator.stack.getAmount() < LiquidFountainBlock.getMax()) {
            if (generator.stack.getAmount() <= 0) {
                generator.stack = FluidStack.EMPTY;
            }
            return;
        }
        generator.stack.setAmount(Integer.MAX_VALUE);
        BlockPos blockPos = generator.getBlockPos();
        //传输
        for (Direction direction : Direction.values()) {
            BlockEntity entity = level.getBlockEntity(blockPos.relative(direction));
            if (entity == null) {
                continue;
            }
            IFluidHandler handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite()).resolve().orElse(null);
            if (handler == null) {
                continue;
            }
            FluidStack stack = generator.stack;
            stack.setAmount(Integer.MAX_VALUE);
            handler.fill(stack.copy(), IFluidHandler.FluidAction.EXECUTE);
        }
        generator.setChanged();
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            LiquidFountainEntity generator = (LiquidFountainEntity) level.getBlockEntity(pos);
            if (generator == null) {
                return InteractionResult.FAIL;
            }
            FluidStack stack = generator.stack;
            if (stack == FluidStack.EMPTY) {
                player.sendSystemMessage(Component.translatable("screen.alltheimbaium.liquid.fountain.empty"));
                return InteractionResult.SUCCESS;
            }
            if (stack.getAmount() < LiquidFountainBlock.getMax()) {
                player.sendSystemMessage(Component.translatable("screen.alltheimbaium.liquid.fountain.current", stack.getDisplayName(), stack.getAmount(), LiquidFountainBlock.getMax()));
                return InteractionResult.SUCCESS;
            }
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.liquid.fountain.max", stack.getDisplayName()));
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("LiquidFountainBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }
}
