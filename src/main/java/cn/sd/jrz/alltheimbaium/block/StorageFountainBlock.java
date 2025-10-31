package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StorageFountainBlock extends Block implements EntityBlock {
    public static final long CARRY = 1000;
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
        generator.tickCount++;
        if (generator.tickCount >= 20 * 20) {
            generator.output += 5;
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
            if (count < StorageFountainBlock.CARRY) {
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
        long maxOutputCount = block / StorageFountainBlock.CARRY;
        stack.setCount(Tool.suitInt(maxOutputCount));
        ItemStack result = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        int count = result.getCount();
        if (count < 0) {
            count = 0;
        }
        if (count > Tool.suitInt(maxOutputCount)) {
            count = Tool.suitInt(maxOutputCount);
        }
        generator.blockList.set(index, block - (maxOutputCount - count) * StorageFountainBlock.CARRY);
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
                showMessage(player, generator);
                return InteractionResult.SUCCESS;
            }
            showMessage(player, generator);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(this.asItem())) {
            addOutputByThis(player, generator, stack);
            showMessage(player, generator);
            return InteractionResult.SUCCESS;
        }
        if (takeItem(player, generator, stack)) {
            showMessage(player, generator);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Tags.Items.STORAGE_BLOCKS) || stack.is(Tags.Items.ORES) || stack.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.contains("storage_blocks/") || path.contains("ores/");
        })) {
            addOutputByBlock(generator, stack);
            showMessage(player, generator);
            return InteractionResult.SUCCESS;
        }
        showMessage(player, generator);
        return InteractionResult.SUCCESS;
    }

    private boolean takeItem(Player player, StorageFountainEntity generator) {
        List<Integer> indexList = canTransport(generator);
        int index;
        if (indexList.isEmpty()) {
            return false;
        } else if (indexList.size() == 1) {
            index = indexList.get(0);
        } else {
            index = indexList.get((int) (Math.random() * indexList.size()));
        }
        ItemStack stack = generator.itemList.get(index).copy();
        Long count = generator.blockList.get(index);
        stack.setCount(1);
        Tool.takeItem(player, stack);
        generator.blockList.set(index, count - StorageFountainBlock.CARRY);
        return true;
    }

    private void addOutputByThis(Player player, StorageFountainEntity generator, ItemStack stackInHand) {
        long count = stackInHand.getCount();
        long output = 0;
        if (stackInHand.hasTag()) {
            CompoundTag tag = stackInHand.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("output", Tag.TAG_LONG)) {
                    output = Tool.suit(tag.getLong("output"));
                }
                if (tag.contains("save_stick")) {
                    ListTag array = (ListTag) tag.get("save_stick");
                    if (array != null) {
                        List<ItemStack> tempItemList = Tool.toItemList(array);
                        List<Long> tempBlockList = Tool.toBlockList(array);
                        nextItem:
                        for (int i = 0; i < Math.min(tempItemList.size(), tempBlockList.size()); i++) {
                            ItemStack stack = tempItemList.get(i);
                            Long block = tempBlockList.get(i);
                            if (stack == null || block == null) {
                                continue;
                            }
                            stack.setCount(1);
                            for (int index = 0; index < Math.min(generator.itemList.size(), generator.blockList.size()); i++) {
                                if (generator.itemList.get(index).equals(stack, true)) {
                                    generator.blockList.set(index, generator.blockList.get(index) + block * count);
                                    continue nextItem;
                                }
                            }
                            if (generator.itemList.size() < 9) {
                                generator.itemList.add(stack);
                                generator.blockList.add(block * count);
                            }
                        }
                        Tool.sort(generator.itemList, generator.blockList);
                    }
                }
            }
        }
        generator.output = Tool.suit(generator.output + output * count);
        player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    private void addOutputByBlock(StorageFountainEntity generator, ItemStack stackInHand) {
        List<ItemStack> itemList = generator.itemList;
        for (ItemStack stack : itemList) {
            if (stackInHand.equals(stack, true)) {
                return;
            }
        }
        if (generator.itemList.size() >= 9) {
            return;
        }
        generator.itemList.add(stackInHand);
        generator.blockList.add(0L);
        Tool.sort(generator.itemList, generator.blockList);
    }

    private boolean takeItem(Player player, StorageFountainEntity generator, ItemStack stackInHand) {
        stackInHand = stackInHand.copy();
        stackInHand.setCount(1);
        List<ItemStack> stackList = generator.itemList;
        List<Long> blockList = generator.blockList;
        for (int i = 0; i < Math.min(stackList.size(), blockList.size()); i++) {
            if (stackList.get(i).equals(stackInHand, true)) {
                if (blockList.get(i) >= StorageFountainBlock.CARRY) {
                    Tool.takeItem(player, stackInHand);
                    blockList.set(i, blockList.get(i) - StorageFountainBlock.CARRY);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    private void showMessage(Player player, StorageFountainEntity generator) {
        long output = generator.output;
        long tickCount = generator.tickCount;
        List<ItemStack> stackList = generator.itemList;
        List<Long> blockList = generator.blockList;
        BigDecimal outputPerTick = new BigDecimal(output).divide(new BigDecimal(StorageFountainBlock.CARRY), 3, RoundingMode.HALF_UP);
        player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.description", outputPerTick, 100D * tickCount / 20 / 20));
        for (int i = 0; i < Math.min(stackList.size(), blockList.size()); i++) {
            ItemStack item = stackList.get(i);
            Long block = blockList.get(i);
            String name = item.getItem().getName(item).getString();
            BigDecimal save = new BigDecimal(block).divide(new BigDecimal(StorageFountainBlock.CARRY), 3, RoundingMode.HALF_UP);
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.current", name, save));
        }
        if (stackList.isEmpty()) {
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.empty"));
        }
    }
}
