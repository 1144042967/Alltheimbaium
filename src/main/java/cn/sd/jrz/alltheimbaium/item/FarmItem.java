package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.FarmBlock;
import cn.sd.jrz.alltheimbaium.setup.DataConfig;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class FarmItem extends BlockItem {
    private final DataConfig config;

    public FarmItem(Block block, DataConfig config) {
        super(block, new Properties().fireResistant());
        this.config = config;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        long level = 1;
        long[] saveArray = new long[config.getProductList().size()];
        String blockData = stack.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (!blockData.isEmpty()) {
            String[] dataArray = blockData.split("#,#");
            level = Tool.suit(dataArray[0]);
            String[] tempArray = dataArray[2].split(",");
            for (int i = 0; i < tempArray.length && i < config.getProductList().size(); i++) {
                saveArray[i] = Tool.suit(tempArray[i]);
            }
        }
        tooltip.add(Component.translatable("screen.alltheimbaium.farm.description", level));
        for (int i = 0; i < config.getProductList().size(); i++) {
            DataConfig.ItemProduct product = config.getProductList().get(i);
            Item item = product.item;
            String name = item.getName(new ItemStack(item)).getString();
            long current = saveArray[i];
            BigDecimal output = new BigDecimal(level * product.count).divide(new BigDecimal(FarmBlock.CARRY), FarmBlock.SCALE, RoundingMode.HALF_UP);
            tooltip.add(Component.translatable("screen.alltheimbaium.farm.product", name, current, output));
        }
    }
}