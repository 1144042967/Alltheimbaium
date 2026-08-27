package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageFountainBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(StorageFountainBlock.class);

    // 从配置文件加载的本地缓存值，由 Config.onConfigLoad() 在配置加载后调用 loadConfig() 填入
    static long carry;
    static long growthIntervalSeconds;
    static long growthStep;
    static List<? extends String> acceptedMods;
    static List<? extends String> acceptedTags;
    static int maxItemTypes;

    /**
     * 由 Config.onConfigLoad() 在配置文件加载完成后调用
     */
    public static void loadConfig() {
        carry = Config.STORAGE_FOUNTAIN_CARRY.get();
        growthIntervalSeconds = Config.STORAGE_FOUNTAIN_GROWTH_INTERVAL_SECONDS.get();
        growthStep = Config.STORAGE_FOUNTAIN_GROWTH_STEP.get();
        acceptedMods = Config.STORAGE_FOUNTAIN_ACCEPTED_MODS.get();
        acceptedTags = Config.STORAGE_FOUNTAIN_ACCEPTED_TAGS.get();
        maxItemTypes = Config.STORAGE_FOUNTAIN_MAX_ITEM_TYPES.get();
    }

    public static long getCarry() {
        return carry;
    }

    public static long getGrowthIntervalSeconds() {
        return growthIntervalSeconds;
    }

    public static long getStep() {
        return growthStep;
    }

    public static int getMaxItemTypes() {
        return maxItemTypes;
    }

    public final Direction[] directions = Direction.values();

    public StorageFountainBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new StorageFountainEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@Nonnull Level level, @Nonnull BlockState state, @Nonnull BlockEntityType<T> type) {
        return (l, p, s, tile) -> {
            try {
                tick(l, tile);
            } catch (Throwable e) {
                log.error("StorageFountainBlock.getTicker error", e);
            }
        };
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof StorageFountainEntity generator)) {
            return;
        }
        BlockPos blockPos = generator.getBlockPos();
        // 增长等级
        generator.tickCount++;
        if (generator.tickCount >= 20L * growthIntervalSeconds) {
            generator.output += growthStep;
            generator.tickCount = 0;
        }
        // 各已标记物品存量累加产量
        generator.blockList.replaceAll(aLong -> aLong + generator.output);
        // 主动输出（受总开关控制）
        if (generator.outputEnabled) {
            for (int i = 0; i < directions.length; i++) {
                generator.findIndex = (generator.findIndex + 1) % directions.length;
                Direction direction = directions[generator.findIndex];
                if (generator.getDirectionState(direction) == StorageFountainEntity.STATE_DISABLED) {
                    continue;
                }
                BlockPos pos = blockPos.relative(direction);
                BlockEntity entity = level.getBlockEntity(pos);
                if (entity == null) {
                    continue;
                }
                IItemHandler handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve().orElse(null);
                if (handler == null) {
                    continue;
                }
                List<Integer> indexList = canTransport(generator, direction);
                if (indexList.isEmpty()) {
                    continue;
                }
                transport(generator, indexList, handler);
            }
        }
        generator.setChanged();
    }

    /**
     * 计算指定面允许传输的物品索引：
     * 随机 → 所有存量达标；槽 N → 仅该槽；禁用 → 空（调用方已跳过）
     */
    private List<Integer> canTransport(StorageFountainEntity generator, Direction direction) {
        int state = generator.getDirectionState(direction);
        List<Long> blockList = generator.blockList;
        List<Integer> indexList = new ArrayList<>();
        if (state == StorageFountainEntity.STATE_RANDOM) {
            for (int i = 0; i < blockList.size(); i++) {
                if (blockList.get(i) >= carry) {
                    indexList.add(i);
                }
            }
        } else if (state >= StorageFountainEntity.STATE_SLOT_BASE) {
            int idx = state - StorageFountainEntity.STATE_SLOT_BASE;
            if (idx < blockList.size() && blockList.get(idx) >= carry) {
                indexList.add(idx);
            }
        }
        return indexList;
    }

    private void transport(StorageFountainEntity generator, List<Integer> indexList, IItemHandler handler) {
        if (indexList.size() == 1) {
            transport(generator, indexList.get(0), handler);
            return;
        }
        Collections.shuffle(indexList);
        for (int index : indexList) {
            transport(generator, index, handler);
        }
    }

    private void transport(StorageFountainEntity generator, int index, IItemHandler handler) {
        ItemStack stack = generator.itemList.get(index).copy();
        Long block = generator.blockList.get(index);
        long maxOutputCount = block / carry;
        stack.setCount(Tool.suitInt(maxOutputCount));
        ItemStack result = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        int count = result.getCount();
        if (count < 0) {
            count = 0;
        }
        if (count > Tool.suitInt(maxOutputCount)) {
            count = Tool.suitInt(maxOutputCount);
        }
        generator.blockList.set(index, block - (maxOutputCount - count) * carry);
    }

    /**
     * 判断物品是否符合接受的 MOD 命名空间或标签
     */
    public static boolean isAcceptedItem(ItemStack stack) {
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        for (String mod : acceptedMods) {
            if (namespace.contains(mod)) return true;
        }
        return stack.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            for (String accepted : acceptedTags) {
                if (path.contains(accepted)) return true;
            }
            return false;
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            StorageFountainEntity generator = (StorageFountainEntity) level.getBlockEntity(pos);
            if (generator == null) {
                return InteractionResult.FAIL;
            }
            // 右键打开 GUI
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, generator, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("StorageFountainBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }
}
