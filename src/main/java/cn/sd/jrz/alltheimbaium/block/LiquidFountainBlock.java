package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.LiquidFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 液体无限制造机方块。
 * <p>
 * 外观为流体储罐（内部液体由 {@link cn.sd.jrz.alltheimbaium.gui.LiquidFountainRenderer} 渲染）。
 * 右键打开 GUI；手持空桶直接装一桶、手持带液容器直接倒入。
 * 破坏时 + 槽 / - 槽中的物品掉落。
 */
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

    /**
     * 判断某个流体命名空间是否属于配置的 auto_infinite 列表（支持部分匹配）
     */
    public static boolean isAutoInfiniteMod(String namespace) {
        for (String mod : autoInfiniteMods) {
            if (namespace.contains(mod)) {
                return true;
            }
        }
        return false;
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
                if (!l.isClientSide && tile instanceof LiquidFountainEntity generator) {
                    generator.serverTick();
                }
            } catch (Throwable e) {
                log.error("LiquidFountainBlock.getTicker error", e);
            }
        };
    }

    /**
     * 破坏时，+ 槽与 - 槽中的物品掉落
     */
    @Override
    public @Nonnull List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, builder));
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof LiquidFountainEntity entity) {
            ItemStack input = entity.inputSlot.getStackInSlot(0);
            if (!input.isEmpty()) {
                drops.add(input);
            }
            ItemStack output = entity.outputSlot.getStackInSlot(0);
            if (!output.isEmpty()) {
                drops.add(output);
            }
        }
        return drops;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            LiquidFountainEntity generator = (LiquidFountainEntity) level.getBlockEntity(pos);
            if (generator == null) {
                return InteractionResult.FAIL;
            }
            ItemStack held = player.getItemInHand(handIn);
            IFluidHandler machine = generator.getCapability(ForgeCapabilities.FLUID_HANDLER).resolve().orElse(null);
            if (machine != null && !held.isEmpty()) {
                // 空桶/空容器：从机器装取液体
                FluidActionResult filled = FluidUtil.tryFillContainer(held, machine, 1000, player, true);
                if (filled.isSuccess()) {
                    player.setItemInHand(handIn, filled.getResult());
                    return InteractionResult.SUCCESS;
                }
                // 带液容器/桶：把液体倒入机器
                FluidActionResult emptied = FluidUtil.tryEmptyContainer(held, machine, 1000, player, true);
                if (emptied.isSuccess()) {
                    player.setItemInHand(handIn, emptied.getResult());
                    return InteractionResult.SUCCESS;
                }
            }
            // 其他情况打开 GUI
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, generator, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("LiquidFountainBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }
}
