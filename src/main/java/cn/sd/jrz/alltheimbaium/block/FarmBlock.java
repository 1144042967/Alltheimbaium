package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.FarmEntity;
import cn.sd.jrz.alltheimbaium.setup.DataConfig;
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
import net.minecraft.world.item.Items;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FarmBlock extends Block implements EntityBlock {
    private final DataConfig config;
    private final Direction[] directions = Direction.values();
    private int findIndex = 0;

    public FarmBlock(Properties properties, DataConfig config) {
        super(properties);
        this.config = config;
    }

    @Override
    public BlockEntity newBlockEntity(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        return new FarmEntity(pos, state, config);
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
        if (!(tile instanceof FarmEntity generator)) {
            return;
        }
        //计算产量
        List<DataConfig.ItemProduct> productList = config.getProductList();
        for (int i = 0; i < productList.size(); i++) {
            DataConfig.ItemProduct product = productList.get(i);
            generator.outputArray[i] = Tool.suit(generator.outputArray[i] + product.count * generator.level);
        }
        //传输
        BlockPos blockPos = generator.getBlockPos();
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

    private List<Integer> canTransport(FarmEntity generator) {
        List<Integer> indexList = new ArrayList<>();
        long[] outputArray = generator.outputArray;
        for (int i = 0; i < outputArray.length; i++) {
            long output = outputArray[i];
            if (output >= 1000) {
                indexList.add(i);
            }
        }
        return indexList;
    }

    private void transport(FarmEntity generator, List<Integer> indexList, IItemHandler handler) {
        if (indexList.size() == 1) {
            transport(generator, indexList.get(0), handler);
            return;
        }
        Collections.shuffle(indexList);
        for (int index : indexList) {
            transport(generator, index, handler);
        }
    }

    private void transport(FarmEntity generator, int index, IItemHandler handler) {
        DataConfig.ItemProduct product = config.getProductList().get(index);
        long output = generator.outputArray[index];
        int maxOutput = Tool.suitInt(output / 1000);
        ItemStack result = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(product.item, maxOutput), false);
        int count = result.getCount();
        if (count < 0) {
            count = 0;
        }
        if (count > maxOutput) {
            count = maxOutput;
        }
        generator.outputArray[index] -= (maxOutput - count) * 1000L;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        FarmEntity generator = (FarmEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack == ItemStack.EMPTY || stack.getItem() == Items.AIR) {
            if (takeItem(player, generator)) {
                return InteractionResult.SUCCESS;
            }
            showMessage(player, generator);
            return InteractionResult.SUCCESS;
        }
        if (stack.is(this.asItem())) {
            addLevel(player, generator, stack);
            showMessage(player, generator);
            return InteractionResult.SUCCESS;
        }
        if (takeItem(player, generator, stack)) {
            return InteractionResult.SUCCESS;
        }
        showMessage(player, generator);
        return InteractionResult.SUCCESS;
    }

    private boolean takeItem(Player player, FarmEntity generator) {
        Integer canOutputIndex = getCanOutputIndex(generator);
        if (canOutputIndex == null) {
            return false;
        }
        Tool.takeItem(player, new ItemStack(config.getProductList().get(canOutputIndex).item));
        generator.outputArray[canOutputIndex] -= 1000L;
        return true;
    }

    private Integer getCanOutputIndex(FarmEntity generator) {
        List<Integer> indexList = canTransport(generator);
        if (indexList.isEmpty()) {
            return null;
        } else if (indexList.size() == 1) {
            return indexList.get(0);
        } else {
            return indexList.get((int) (Math.random() * indexList.size()));
        }
    }

    private void addLevel(Player player, FarmEntity generator, ItemStack stackInHand) {
        long count = stackInHand.getCount();
        long level = 1;
        if (stackInHand.hasTag()) {
            CompoundTag tag = stackInHand.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("level", Tag.TAG_LONG)) {
                    level = Tool.suit(tag.getLong("level"));
                }
            }
        }
        long old = generator.level;
        generator.level = Tool.suit(old + count * level);
        count = generator.level - old;
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(stackInHand.getItem(), (int) (stackInHand.getCount() - count)));
    }

    private boolean takeItem(Player player, FarmEntity generator, ItemStack stackInHand) {
        for (int i = 0; i < config.getProductList().size(); i++) {
            DataConfig.ItemProduct product = config.getProductList().get(i);
            if (!stackInHand.is(product.item)) {
                continue;
            }
            if (generator.outputArray[i] < 1000L) {
                return false;
            }
            Tool.takeItem(player, new ItemStack(product.item));
            generator.outputArray[i] -= 1000L;
            return true;
        }
        return false;
    }

    private void showMessage(Player player, FarmEntity generator) {
        long level = generator.level;
        player.sendSystemMessage(Component.translatable("screen.alltheimbaium.farm.description", level));
        List<DataConfig.ItemProduct> productList = config.getProductList();
        for (int i = 0; i < productList.size(); i++) {
            DataConfig.ItemProduct product = productList.get(i);
            Item item = product.item;
            String name = item.getName(new ItemStack(item)).getString();
            long current = generator.outputArray[i] / 1000;
            double output = level * product.count / 1000D;
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.farm.product", name, current, output));
        }
    }
}