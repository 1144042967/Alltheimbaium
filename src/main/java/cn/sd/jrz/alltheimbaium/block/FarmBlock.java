package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.FarmEntity;
import cn.sd.jrz.alltheimbaium.setup.DataConfig;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FarmBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(FarmBlock.class);
    public static final long CARRY = 10000;
    public static final int SCALE = String.valueOf(CARRY).length() - 1;
    private final DataConfig config;
    public final Direction[] directions = Direction.values();

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
        return (l, p, s, tile) -> {
            try {
                tick(l, tile);
            } catch (Throwable e) {
                log.error("FarmBlock.getTicker error", e);
            }
        };
    }

    private <T extends BlockEntity> void tick(Level level, T tile) {
        if (level.isClientSide) {
            return;
        }
        if (!(tile instanceof FarmEntity generator)) {
            return;
        }
        //增加等级
        generator.tickCount++;
        if (generator.tickCount >= 20 * 20) {
            generator.level++;
            generator.tickCount = 0;
        }
        //计算产量
        List<DataConfig.ItemProduct> productList = config.getProductList();
        for (int i = 0; i < productList.size(); i++) {
            DataConfig.ItemProduct product = productList.get(i);
            generator.outputArray[i] = Tool.suit(generator.outputArray[i] + Tool.suit(product.count * generator.level));
            if (generator.outputArray[i] > CARRY) {
                generator.saveArray[i] = Tool.suit(generator.saveArray[i] + generator.outputArray[i] / CARRY);
                generator.outputArray[i] = generator.outputArray[i] % CARRY;
            }
        }
        //传输
        BlockPos blockPos = generator.getBlockPos();
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

    private List<Integer> canTransport(FarmEntity generator) {
        List<Integer> indexList = new ArrayList<>();
        long[] saveArray = generator.saveArray;
        for (int i = 0; i < saveArray.length; i++) {
            long save = saveArray[i];
            if (save >= 1) {
                indexList.add(i);
            }
        }
        return indexList;
    }

    private void transport(FarmEntity generator, List<Integer> indexList, IItemHandler handler) {
        if (indexList.size() == 1) {
            transport(generator, indexList.getFirst(), handler);
            return;
        }
        Collections.shuffle(indexList);
        for (int index : indexList) {
            transport(generator, index, handler);
        }
    }

    private void transport(FarmEntity generator, int index, IItemHandler handler) {
        DataConfig.ItemProduct product = config.getProductList().get(index);
        long save = generator.saveArray[index];
        int maxSave = Tool.suitInt(save);
        ItemStack result = ItemHandlerHelper.insertItemStacked(handler, new ItemStack(product.item, maxSave), false);
        int count = result.getCount();
        if (count < 0) {
            count = 0;
        }
        if (count > maxSave) {
            count = maxSave;
        }
        generator.saveArray[index] = Tool.suit(generator.saveArray[index] - (maxSave - count));
    }

    @Override
    public @Nonnull InteractionResult useWithoutItem(@Nonnull BlockState state, @NotNull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            return use(level, pos, player);
        } catch (Throwable e) {
            log.error("FarmBlock.useWithoutItem error", e);
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
            log.error("FarmBlock.useItemOn error", e);
        }
        return super.useItemOn(itemStack, state, level, pos, player, handIn, hit);
    }

    private @Nonnull InteractionResult use(Level level, @Nonnull BlockPos pos, @Nonnull Player player) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        FarmEntity generator = (FarmEntity) level.getBlockEntity(pos);
        if (generator == null) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = player.getMainHandItem();
        if (takeItem(player, generator, stack)) {
            return InteractionResult.SUCCESS;
        }
        showMessage(player, generator);
        return InteractionResult.SUCCESS;
    }

    private boolean takeItem(Player player, FarmEntity generator, ItemStack stackInHand) {
        for (int i = 0; i < config.getProductList().size(); i++) {
            DataConfig.ItemProduct product = config.getProductList().get(i);
            if (!stackInHand.is(product.item)) {
                continue;
            }
            if (generator.saveArray[i] < 1) {
                return false;
            }
            Tool.takeItem(player, new ItemStack(product.item));
            generator.saveArray[i] = Tool.suitInt(generator.saveArray[i] - 1);
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
            long current = generator.saveArray[i];
            BigDecimal output = new BigDecimal(level * product.count).divide(new BigDecimal(FarmBlock.CARRY), SCALE, RoundingMode.HALF_UP);
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.farm.product", name, current, output));
        }
    }
}