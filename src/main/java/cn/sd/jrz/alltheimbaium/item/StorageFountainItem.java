package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.setup.Config;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class StorageFountainItem extends BlockItem {
    private static final Logger log = LoggerFactory.getLogger(StorageFountainItem.class);

    public StorageFountainItem(Block block) {
        super(block, new Properties().fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, context, tooltip, flagIn);
        try {
            long output = 5;
            List<ItemStack> itemList = new ArrayList<>();
            List<Long> blockList = new ArrayList<>();
            String blockData = stack.getOrDefault(Registration.BLOCK_DATA.get(), "");
            if (!blockData.isEmpty()) {
                String[] dataArray = blockData.split("#,#");
                if (dataArray.length >= 3) {
                    output = Tool.suit(dataArray[0]);
                    itemList = Tool.fromItemString(dataArray[1]);
                    blockList = Tool.fromBlockString(dataArray[2]);
                }
            }
            String outputPerTick = String.format("%.4f", (double) output / StorageFountainBlock.CARRY);
            tooltip.add(Component.translatable("screen.alltheimbaium.fountain.output", outputPerTick));
            for (int i = 0; i < Math.min(itemList.size(), blockList.size()); i++) {
                ItemStack itemStack = itemList.get(i);
                Long block = blockList.get(i);
                String name = itemStack.getItem().getDescription().getString();
                String save = String.format("%.4f", (double) block / StorageFountainBlock.CARRY);
                tooltip.add(Component.translatable("screen.alltheimbaium.fountain.current", name, save));
            }
            if (itemList.isEmpty()) {
                String mods = String.join(", ", Config.STORAGE_FOUNTAIN_ACCEPTED_MODS.get());
                String tags = String.join(", ", Config.STORAGE_FOUNTAIN_ACCEPTED_TAGS.get());
                tooltip.add(Component.translatable("screen.alltheimbaium.fountain.empty"));
                tooltip.add(Component.literal("§7MOD: §e" + mods));
                tooltip.add(Component.literal("§7Tags: §e" + tags));
            }
        } catch (Throwable e) {
            log.error("StorageFountainItem.appendHoverText error", e);
        }
    }
}
