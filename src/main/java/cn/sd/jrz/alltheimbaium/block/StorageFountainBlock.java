package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class StorageFountainBlock extends Block implements EntityBlock {
    public static final long CARRY = 10000;
    private final Direction[] directions = Direction.values();
    private int findIndex = 0;
    private int tickCount = 0;

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
        return (l, p, s, tile) -> tick(l, tile);
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
        tickCount++;
        if (tickCount >= 20 * 20) {
            generator.level++;
            tickCount = 0;
        }
        //计算产量
        Map<Item, Integer> outputMap = calcOutputMap(blockPos, level);
        //清除无用
        clearNotContains(outputMap, generator);
        //生成缓存
        calcCache(outputMap, generator);
        //生成物品
        calcItem(generator);
        //传输
        for (int i = 0; i < directions.length; i++) {
            findIndex = (findIndex + 1) % directions.length;
            Direction direction = directions[findIndex];
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

    private Map<Item, Integer> calcOutputMap(BlockPos blockPos, Level level) {
        Map<Item, Integer> outputMap = new LinkedHashMap<>();
        for (Direction direction : directions) {
            BlockPos pos = blockPos.relative(direction);
            BlockState state = level.getBlockState(pos);
            if (state.is(Tags.Blocks.STORAGE_BLOCKS)) {
                Item item = state.getBlock().asItem();
                if (outputMap.containsKey(item)) {
                    outputMap.put(item, Math.max(outputMap.get(item) * 2, outputMap.get(item) * outputMap.get(item)));
                } else {
                    outputMap.put(item, 1);
                }
            }
        }
        return outputMap;
    }

    private void clearNotContains(Map<Item, Integer> outputMap, StorageFountainEntity generator) {
        for (int i = 0; i < generator.saveArray.length; i++) {
            ItemStack stack = generator.saveArray[i];
            if (stack == null || stack.isEmpty() || !outputMap.containsKey(stack.getItem())) {
                generator.saveArray[i] = null;
            }
        }
    }

    private void calcCache(Map<Item, Integer> outputMap, StorageFountainEntity generator) {
        nextOutput:
        for (Map.Entry<Item, Integer> output : outputMap.entrySet()) {
            Item item = output.getKey();
            int add = Tool.suitInt(output.getValue().longValue() * generator.level);
            for (ItemStack stack : generator.saveArray) {
                if (stack != null && stack.is(item)) {
                    stack.setCount(Tool.suitInt(stack.getCount() + add));
                    continue nextOutput;
                }
            }
            for (int i = 0; i < generator.saveArray.length; i++) {
                ItemStack stack = generator.saveArray[i];
                if (stack == null || stack.isEmpty()) {
                    generator.saveArray[i] = new ItemStack(item, add);
                    continue nextOutput;
                }
            }
        }
    }

    private void calcItem(StorageFountainEntity generator) {
        nextSource:
        for (ItemStack source : generator.saveArray) {
            if (source == null || source.isEmpty()) {
                continue;
            }
            if (source.getCount() < StorageFountainBlock.CARRY) {
                continue;
            }
            Item item = source.getItem();
            int count = (int) (source.getCount() / StorageFountainBlock.CARRY);
            for (ItemStack target : generator.saveItems) {
                if (target != null && target.is(item)) {
                    target.setCount(Tool.suitInt(target.getCount() + count));
                    source.setCount(Tool.suitInt(source.getCount() - count * StorageFountainBlock.CARRY));
                    continue nextSource;
                }
            }
            for (int i = 0; i < generator.saveItems.length; i++) {
                ItemStack stack = generator.saveItems[i];
                if (stack == null || stack.isEmpty()) {
                    generator.saveItems[i] = new ItemStack(item, count);
                    source.setCount(Tool.suitInt(source.getCount() - count * StorageFountainBlock.CARRY));
                    continue nextSource;
                }
            }
            source.setCount((int) StorageFountainBlock.CARRY);
        }
    }

    private List<Integer> canTransport(StorageFountainEntity generator) {
        List<Integer> indexList = new ArrayList<>();
        for (int i = 0; i < generator.saveItems.length; i++) {
            ItemStack stack = generator.saveItems[i];
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (stack.getCount() >= 1) {
                indexList.add(i);
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
        ItemStack stack = generator.saveItems[index];
        ItemStack result = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
        int count = result.getCount();
        if (count < 0) {
            count = 0;
        }
        if (count > stack.getCount()) {
            count = stack.getCount();
        }
        stack.setCount(count);
        if (stack.getCount() <= 0) {
            generator.saveItems[index] = null;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        StorageFountainEntity generator = (StorageFountainEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            if (takeItem(player, generator)) {
                return InteractionResult.SUCCESS;
            }
            showMessage(player, level, generator);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(this.asItem())) {
            addLevel(player, generator, stack);
            showMessage(player, level, generator);
            return InteractionResult.SUCCESS;
        }
        if (takeItem(player, generator, stack)) {
            return InteractionResult.SUCCESS;
        }
        showMessage(player, level, generator);
        return InteractionResult.SUCCESS;
    }

    private boolean takeItem(Player player, StorageFountainEntity generator) {
        Integer canOutputIndex = getCanOutputIndex(generator);
        if (canOutputIndex == null) {
            return false;
        }
        ItemStack stack = generator.saveItems[canOutputIndex];
        Tool.takeItem(player, new ItemStack(stack.getItem()));
        stack.grow(-1);
        if (stack.getCount() <= 0) {
            generator.saveItems[canOutputIndex] = null;
        }
        return true;
    }

    private Integer getCanOutputIndex(StorageFountainEntity generator) {
        List<Integer> indexList = canTransport(generator);
        if (indexList.isEmpty()) {
            return null;
        } else if (indexList.size() == 1) {
            return indexList.get(0);
        } else {
            return indexList.get((int) (Math.random() * indexList.size()));
        }
    }

    private void addLevel(Player player, StorageFountainEntity generator, ItemStack stackInHand) {
        long count = stackInHand.getCount();
        int level = 1;
        if (stackInHand.hasTag()) {
            CompoundTag tag = stackInHand.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("level", Tag.TAG_INT)) {
                    level = Tool.suitInt(tag.getInt("level"));
                }
            }
        }
        generator.level = Tool.suitInt(generator.level + Tool.suit(count * level));
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    private boolean takeItem(Player player, StorageFountainEntity generator, ItemStack stackInHand) {
        for (int i = 0; i < generator.saveItems.length; i++) {
            ItemStack stack = generator.saveItems[i];
            if (stack == null || !stackInHand.is(stack.getItem())) {
                continue;
            }
            if (stack.getCount() < 1) {
                return false;
            }
            Tool.takeItem(player, new ItemStack(stack.getItem()));
            stack.grow(-1);
            if (stack.getCount() <= 0) {
                generator.saveItems[i] = null;
            }
            return true;
        }
        return false;
    }

    private void showMessage(Player player, Level wroldLevel, StorageFountainEntity generator) {
        long level = generator.level;
        player.sendSystemMessage(Component.translatable("screen.alltheimbaium.farm.description", level));
        Map<Item, Integer> map = calcOutputMap(generator.getBlockPos(), wroldLevel);
        for (Map.Entry<Item, Integer> entry : map.entrySet()) {
            Item item = entry.getKey();
            int count = Tool.suitInt(entry.getValue() * level);
            String name = item.getName(new ItemStack(item)).getString();
            BigDecimal output = new BigDecimal(count).divide(new BigDecimal(StorageFountainBlock.CARRY), 4, RoundingMode.HALF_UP);
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.output", name, output));
        }
        for (ItemStack temp : generator.saveItems) {
            if (temp == null || temp.isEmpty()) {
                continue;
            }
            Item item = temp.getItem();
            String name = item.getName(new ItemStack(item)).getString();
            int current = temp.getCount();
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.current", name, current));
        }
    }
}
