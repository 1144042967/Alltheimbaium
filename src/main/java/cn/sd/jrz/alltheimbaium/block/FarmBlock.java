package cn.sd.jrz.alltheimbaium.block;

import cn.sd.jrz.alltheimbaium.entity.FarmEntity;
import cn.sd.jrz.alltheimbaium.setup.Config;
import cn.sd.jrz.alltheimbaium.setup.DataConfig;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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

public class FarmBlock extends Block implements EntityBlock {
    private static final Logger log = LoggerFactory.getLogger(FarmBlock.class);

    // 从配置文件加载的本地缓存值，由 Config.onConfigLoad() 在配置加载后调用 loadConfig() 填入
    static long carry;
    static long maxLevel;
    static int levelUpIntervalSeconds;

    public static void loadConfig() {
        carry = Config.FARM_CARRY.get();
        maxLevel = Config.FARM_MAX_LEVEL.get();
        levelUpIntervalSeconds = Config.FARM_LEVEL_UP_INTERVAL_SECONDS.get();
    }

    public static long getCarry() {
        return carry;
    }

    public static int getScale() {
        return String.valueOf(carry).length() - 1;
    }

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
        long max = FarmBlock.maxLevel;
        if (generator.level < max) {
            generator.tickCount++;
            if (generator.tickCount >= 20L * levelUpIntervalSeconds) {
                generator.level++;
                generator.tickCount = 0;
            }
        }
        //计算产量
        List<DataConfig.ItemProduct> productList = config.getProductList();
        for (int i = 0; i < productList.size(); i++) {
            DataConfig.ItemProduct product = productList.get(i);
            generator.outputArray[i] = Tool.suit(generator.outputArray[i] + Tool.suit(product.count * generator.level));
            long carry = getCarry();
            if (generator.outputArray[i] > carry) {
                generator.saveArray[i] = Tool.suit(generator.saveArray[i] + generator.outputArray[i] / carry);
                generator.outputArray[i] = generator.outputArray[i] % carry;
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
            IItemHandler handler = entity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).resolve().orElse(null);
            if (handler == null) {
                continue;
            }
            List<Integer> indexList = canTransport(generator);
            if (indexList.isEmpty()) {
                break;
            }
            Collections.shuffle(indexList);
            for (int index : indexList) {
                transport(generator, index, handler);
            }
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

    @SuppressWarnings("deprecation")
    @Override
    public @Nonnull InteractionResult use(@Nonnull BlockState state, @Nonnull Level level, @Nonnull BlockPos pos, @Nonnull Player player, @Nonnull InteractionHand handIn, @Nonnull BlockHitResult hit) {
        try {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            FarmEntity generator = (FarmEntity) level.getBlockEntity(pos);
            if (generator == null) {
                return InteractionResult.FAIL;
            }
            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                // 空手右键 → 随机获得一个有库存的产物
                takeRandomItem(player, generator);
            } else {
                // 手持产物右键 → 获得对应的物品
                takeSpecificItem(player, generator, stack);
            }
            return InteractionResult.SUCCESS;
        } catch (Throwable e) {
            log.error("FarmBlock.use error", e);
        }
        return super.use(state, level, pos, player, handIn, hit);
    }

    /**
     * 从有库存的产物中随机给予玩家一个
     */
    private void takeRandomItem(Player player, FarmEntity generator) {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < generator.saveArray.length; i++) {
            if (generator.saveArray[i] >= 1) {
                available.add(i);
            }
        }
        if (available.isEmpty()) {
            showMessage(player, generator);
            return;
        }
        int index = available.get((int) (Math.random() * available.size()));
        Tool.takeItem(player, new ItemStack(config.getProductList().get(index).item));
        generator.saveArray[index] = Tool.suit(generator.saveArray[index] - 1);
    }

    /**
     * 手持指定产物时给予对应的物品
     */
    private void takeSpecificItem(Player player, FarmEntity generator, ItemStack stackInHand) {
        for (int i = 0; i < config.getProductList().size(); i++) {
            DataConfig.ItemProduct product = config.getProductList().get(i);
            if (!stackInHand.is(product.item)) {
                showMessage(player, generator);
                continue;
            }
            if (generator.saveArray[i] < 1) {
                showMessage(player, generator);
                return;
            }
            Tool.takeItem(player, new ItemStack(product.item));
            generator.saveArray[i] = Tool.suit(generator.saveArray[i] - 1);
            return;
        }
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
            BigDecimal output = new BigDecimal(level * product.count).divide(new BigDecimal(FarmBlock.getCarry()), FarmBlock.getScale(), RoundingMode.HALF_UP);
            player.sendSystemMessage(Component.translatable("screen.alltheimbaium.farm.product", name, current, output));
        }
    }
}