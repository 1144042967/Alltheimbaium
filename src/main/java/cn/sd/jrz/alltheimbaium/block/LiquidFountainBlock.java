package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.world.InteractionResult;
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
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
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
                if (!l.isClientSide() && tile instanceof LiquidFountainEntity generator) {
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
        // 1.21：BE 数据改由 getDrops 写进 block_entity_data 组件（替代 1.20.1 战利品表的 copy_nbt）
        return Tool.withBlockEntityData(drops, builder);
    }

    @SuppressWarnings("deprecation")
    private InteractionResult doUse(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            LiquidFountainEntity generator = (LiquidFountainEntity) level.getBlockEntity(pos);
            if (generator == null) {
                return InteractionResult.FAIL;
            }
            ItemStack held = player.getItemInHand(handIn);
            // 26.x：流体能力改为 ResourceHandler<FluidResource>（旧的 IFluidHandler 已标 forRemoval，
            // 且 NeoForge 只提供"新→旧"的适配器），本机自己的流体能力仍直接从实体取（null 面即"不区分方向"）
            ResourceHandler<FluidResource> machine = generator.getFluidHandler(null);
            if (machine != null && !held.isEmpty()) {
                // 26.x：transfer 版 FluidUtil 把"先装取、再倒入"两步合并，并自行回写玩家手上的容器
                if (FluidUtil.interactWithFluidHandler(player, handIn, pos, machine)) {
                    return InteractionResult.SUCCESS;
                }
            }
            // 其他情况打开 GUI
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(generator, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("LiquidFountainBlock.use error", e);
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

}