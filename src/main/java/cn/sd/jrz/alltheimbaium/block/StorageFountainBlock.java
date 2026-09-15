package cn.sd.jrz.alltheimbaium.block;

import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.InteractionResult;
import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
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
    /** 物品白名单：完整注册 ID，命中则无视标签/命名空间规则直接接受 */
    static List<? extends String> acceptedItems;
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
        acceptedItems = Config.STORAGE_FOUNTAIN_ACCEPTED_ITEMS.get();
        maxItemTypes = Config.STORAGE_FOUNTAIN_MAX_ITEM_TYPES.get();
    }

    /** 物品白名单（完整注册 ID），供 GUI 帮助卡展示 */
    @Nonnull
    public static List<? extends String> getAcceptedItems() {
        return acceptedItems;
    }

    /** 接受的 MOD 命名空间，供 GUI 帮助卡展示 */
    @Nonnull
    public static List<? extends String> getAcceptedMods() {
        return acceptedMods;
    }

    /** 接受的标签片段，供 GUI 帮助卡展示 */
    @Nonnull
    public static List<? extends String> getAcceptedTags() {
        return acceptedTags;
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
        if (level.isClientSide()) {
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
                // 能力查询走 level.getCapability（方块实体本身不再能查能力）；
                // 26.x 的物品能力类型是 ResourceHandler<ItemResource>，不再有 IItemHandler 版本的注册点
                ResourceHandler<ItemResource> handler = level.getCapability(Capabilities.Item.BLOCK, pos, direction.getOpposite());
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

    private void transport(StorageFountainEntity generator, List<Integer> indexList, ResourceHandler<ItemResource> handler) {
        if (indexList.size() == 1) {
            transport(generator, indexList.get(0), handler);
            return;
        }
        Collections.shuffle(indexList);
        for (int index : indexList) {
            transport(generator, index, handler);
        }
    }

    private void transport(StorageFountainEntity generator, int index, ResourceHandler<ItemResource> handler) {
        ItemStack stack = generator.itemList.get(index).copy();
        // insertStacking 要求资源非空（空资源会抛异常），这里显式跳过，保持旧版 ItemHandlerHelper 的"空栈即空操作"语义
        if (stack.isEmpty()) {
            return;
        }
        Long block = generator.blockList.get(index);
        long maxOutputCount = block / carry;
        int maxOutput = Tool.suitInt(maxOutputCount);
        // 26.x：ItemHandlerHelper.insertItemStacked 对应 ResourceHandlerUtil.insertStacking，
        // 但返回值语义相反 —— 这里返回的是"已插入数量"，不是剩余数量。物品带组件（ItemResource.of(ItemStack)）
        int count = ResourceHandlerUtil.insertStacking(handler, ItemResource.of(stack), maxOutput, null);
        if (count < 0) {
            count = 0;
        }
        if (count > maxOutput) {
            count = maxOutput;
        }
        generator.blockList.set(index, block - count * carry);
    }

    /**
     * 判断物品是否可被标记复制：物品白名单 → MOD 命名空间 → 标签，命中其一即可
     */
    public static boolean isAcceptedItem(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        // 白名单优先：完整注册 ID 精确匹配，无视后两条规则
        if (acceptedItems.contains(id.toString())) {
            return true;
        }
        String namespace = id.getNamespace();
        for (String mod : acceptedMods) {
            if (namespace.contains(mod)) return true;
        }
        // 26.x：ItemStack#getTags（NeoForge 扩展）已删除，改从物品的 Holder 取标签（TagKey#location 仍是 Identifier）
        return BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()).tags().anyMatch(tag -> {
            String path = tag.location().getPath();
            for (String accepted : acceptedTags) {
                if (path.contains(accepted)) return true;
            }
            return false;
        });
    }

    @SuppressWarnings("deprecation")
    private InteractionResult doUse(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            StorageFountainEntity generator = (StorageFountainEntity) level.getBlockEntity(pos);
            if (generator == null) {
                return InteractionResult.FAIL;
            }
            // 右键打开 GUI
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(generator, pos);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("StorageFountainBlock.use error", e);
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

    /**
     * 1.21：掉落时把方块实体数据写进物品的 block_entity_data 组件（替代 1.20.1 战利品表的 copy_nbt）。
     */
    @Override
    @Nonnull
    public List<ItemStack> getDrops(@Nonnull BlockState state, @Nonnull LootParams.Builder params) {
        return Tool.withBlockEntityData(super.getDrops(state, params), params);
    }
}