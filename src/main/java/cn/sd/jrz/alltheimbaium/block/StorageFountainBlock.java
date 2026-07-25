package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.StorageFountainEntity;
import cn.sd.jrz.alltheimbaium.setup.Tool;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class StorageFountainBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(StorageFountainBlock.class);
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
        if (generator.tickCount >= 20 * 20) {
            generator.output += 5;
            generator.tickCount = 0;
        }
        //增长数值
        //noinspection Java8ListReplaceAll
        for (int i = 0; i < generator.blockList.size(); i++) {
            generator.blockList.set(i, Tool.suit(generator.blockList.get(i) + generator.output));
        }
        //传输
        for (int i = 0; i < directions.length; i++) {
            generator.findIndex = (generator.findIndex + 1) % directions.length;
            Direction direction = directions[generator.findIndex];
            BlockPos pos = blockPos.relative(direction);
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity == null) {
                continue;
            }
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction.getOpposite());
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

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return use(level, pos, player);
        } catch (Throwable e) {
            log.error("StorageFountainBlock.useWithoutItem error", e);
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
            log.error("StorageFountainBlock.useItemOn error", e);
        }
        return super.useItemOn(itemStack, state, level, pos, player, handIn, hit);
    }

    private @Nonnull InteractionResult use(Level level, @Nonnull BlockPos pos, @Nonnull Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        StorageFountainEntity generator = (StorageFountainEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            takeItem(player, generator);
            showMessage(player, generator);
            return InteractionResult.SUCCESS;
        }
        if (takeItem(player, generator, stack)) {
            showMessage(player, generator);
            return InteractionResult.SUCCESS;
        }
        String namespace = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        boolean modContains = namespace.contains("modern_industrialization") || namespace.contains("extended_industrialization");
        boolean tagContains = stack.getTags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return path.contains("storage_blocks")
                    || path.contains("ores")
                    || path.contains("ingots")
                    || path.contains("dusts")
                    || path.contains("gems")
                    || path.contains("alloys")
                    || path.contains("plates")
                    || path.contains("enriched")
                    || path.contains("circuits")
                    || path.contains("pellets");
        });
        if (modContains || tagContains) {
            addOutputByBlock(generator, stack);
            showMessage(player, generator);
            return InteractionResult.SUCCESS;
        }
        showMessage(player, generator);
        return InteractionResult.SUCCESS;
    }

    private void takeItem(Player player, StorageFountainEntity generator) {
        List<Integer> indexList = canTransport(generator);
        int index;
        if (indexList.isEmpty()) {
            return;
        } else if (indexList.size() == 1) {
            index = indexList.getFirst();
        } else {
            index = indexList.get((int) (Math.random() * indexList.size()));
        }
        ItemStack stack = generator.itemList.get(index).copy();
        Long count = generator.blockList.get(index);
        stack.setCount(1);
        Tool.takeItem(player, stack);
        generator.blockList.set(index, count - StorageFountainBlock.CARRY);
    }

    private void addOutputByBlock(StorageFountainEntity generator, ItemStack stackInHand) {
        List<ItemStack> itemList = generator.itemList;
        for (ItemStack stack : itemList) {
            if (ItemStack.isSameItemSameComponents(stackInHand, stack)) {
                return;
            }
        }
        if (generator.itemList.size() >= 9) {
            return;
        }
        stackInHand = stackInHand.copy();
        stackInHand.setCount(1);
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
            if (stackList.get(i).getItem() == stackInHand.getItem()) {
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
        String outputPerTick = String.format("%.3f", (double) output / StorageFountainBlock.CARRY);
        player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.description", outputPerTick, 100D * tickCount / 20 / 20));
        for (int i = 0; i < Math.min(stackList.size(), blockList.size()); i++) {
            ItemStack item = stackList.get(i);
            Long block = blockList.get(i);
            String name = item.getItem().getDescription().getString();
            String save = String.format("%.3f", (double) block / StorageFountainBlock.CARRY);
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.current", name, save));
        }
        if (stackList.isEmpty()) {
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.fountain.empty"));
        }
    }
}
