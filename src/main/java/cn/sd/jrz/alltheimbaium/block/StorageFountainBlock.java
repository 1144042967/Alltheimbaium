package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** 由 Config.onConfigLoad() 在配置文件加载完成后调用 */
    public static void loadConfig() {
        carry = Config.STORAGE_FOUNTAIN_CARRY.get();
        growthIntervalSeconds = Config.STORAGE_FOUNTAIN_GROWTH_INTERVAL_SECONDS.get();
        growthStep = Config.STORAGE_FOUNTAIN_GROWTH_STEP.get();
        acceptedMods = Config.STORAGE_FOUNTAIN_ACCEPTED_MODS.get();
        acceptedTags = Config.STORAGE_FOUNTAIN_ACCEPTED_TAGS.get();
        maxItemTypes = maxItemTypes;
    }

    public static long getCarry() { return carry; }
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
        //增加等级
        generator.tickCount++;
        if (generator.tickCount >= 20L * growthIntervalSeconds) {
            generator.output += growthStep;
            generator.tickCount = 0;
        }
        //增长数值
        generator.blockList.replaceAll(aLong -> aLong + generator.output);
        //传输
        for (int i = 0; i < directions.length; i++) {
            generator.findIndex = (generator.findIndex + 1) % directions.length;
            Direction direction = directions[generator.findIndex];
            BlockPos pos = blockPos.relative(direction);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            IItemHandler handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve().orElse(null);
            if (handler == null) {
                continue;
            }
            List<Integer> indexList = canTransport(generator);
            if (indexList.isEmpty()) {
                break;
            }
            transport(generator, indexList, handler);
        }
        generator.setChanged();
    }

    private List<Integer> canTransport(StorageFountainEntity generator) {
        List<Long> blockList = generator.blockList;
        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < blockList.size(); i++) {
            Long count = blockList.get(i);
            if (count < StorageFountainBlock.getCarry()) {
                continue;
            }
            indexList.add(i);
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
        long maxOutputCount = block / StorageFountainBlock.getCarry();
        stack.setCount(Tool.suitInt(maxOutputCount));
        ItemStack result = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        int count = result.getCount();
        if (count < 0) {
            count = 0;
        }
        if (count > Tool.suitInt(maxOutputCount)) {
            count = Tool.suitInt(maxOutputCount);
        }
        generator.blockList.set(index, block - (maxOutputCount - count) * StorageFountainBlock.getCarry());
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
            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                // 空手 → 随机取出一个物品；如果无法取出，只打印内容
                if (!takeRandomItem(player, generator)) {
                    showMessage(player, generator);
                }
            } else if (takeSpecificItem(player, generator, stack)) {
                // 主手物品在存储列表中 → 尝试取出；成功则不额外打印
            } else if (isAcceptedItem(stack)) {
                // 主手物品不在存储列表中，但在接受的 MOD/标签中 → 添加到生成列表
                addOutputByBlock(generator, stack);
                showMessage(player, generator);
            } else {
                // 主手物品不在存储列表中，也不在接受范围内 → 只打印内容
                showMessage(player, generator);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("StorageFountainBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }

    /**
     * 判断物品是否符合接受的 MOD 命名空间或标签
     */
    private static boolean isAcceptedItem(ItemStack stack) {
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

    /**
     * 从有库存的产物中随机给予玩家一个，成功返回 true
     */
    private boolean takeRandomItem(Player player, StorageFountainEntity generator) {
        List<Integer> indexList = canTransport(generator);
        if (indexList.isEmpty()) {
            return false;
        }
        int index = indexList.get((int) (Math.random() * indexList.size()));
        ItemStack stack = generator.itemList.get(index).copy();
        stack.setCount(1);
        Tool.takeItem(player, stack);
        generator.blockList.set(index, generator.blockList.get(index) - StorageFountainBlock.getCarry());
        return true;
    }

    /**
     * 手持指定产物时给予对应的物品，成功返回 true
     */
    private static boolean takeSpecificItem(Player player, StorageFountainEntity generator, ItemStack stackInHand) {
        ItemStack single = stackInHand.copy();
        single.setCount(1);
        List<ItemStack> stackList = generator.itemList;
        List<Long> blockList = generator.blockList;
        for (int i = 0; i < Math.min(stackList.size(), blockList.size()); i++) {
            if (stackList.get(i).equals(single, true)) {
                if (blockList.get(i) >= StorageFountainBlock.getCarry()) {
                    Tool.takeItem(player, stackList.get(i).copy());
                    blockList.set(i, blockList.get(i) - StorageFountainBlock.getCarry());
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    /**
     * 将接受的物品添加到生成列表中
     */
    private static void addOutputByBlock(StorageFountainEntity generator, ItemStack stackInHand) {
        // 已存在则跳过
        for (ItemStack existing : generator.itemList) {
            if (stackInHand.equals(existing, true)) {
                return;
            }
        }
        if (generator.itemList.size() >= maxItemTypes) {
            return;
        }
        ItemStack single = stackInHand.copy();
        single.setCount(1);
        generator.itemList.add(single);
        generator.blockList.add(0L);
        Tool.sort(generator.itemList, generator.blockList);
    }

    private void showMessage(Player player, StorageFountainEntity generator) {
        long output = generator.output;
        long tickCount = generator.tickCount;
        List<ItemStack> stackList = generator.itemList;
        List<Long> blockList = generator.blockList;
        BigDecimal outputPerTick = new BigDecimal(output).divide(new BigDecimal(StorageFountainBlock.getCarry()), 3, RoundingMode.HALF_UP);
        player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.description", outputPerTick, 100D * tickCount / 20 / 20));
        for (int i = 0; i < Math.min(stackList.size(), blockList.size()); i++) {
            ItemStack item = stackList.get(i);
            Long block = blockList.get(i);
            String name = item.getItem().getName(item).getString();
            BigDecimal save = new BigDecimal(block).divide(new BigDecimal(StorageFountainBlock.getCarry()), 3, RoundingMode.HALF_UP);
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.current", name, save));
        }
        if (stackList.isEmpty()) {
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.empty"));
        }
    }
}
