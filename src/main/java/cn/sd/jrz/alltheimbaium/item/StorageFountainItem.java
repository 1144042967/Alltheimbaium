package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.block.StorageFountainBlock;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class StorageFountainItem extends BlockItem {

    public StorageFountainItem(Block block) {
        super(block, new Properties().fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        long output = 5;
        List<Item> itemList = new ArrayList<>();
        List<Long> blockList = new ArrayList<>();
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("output", Tag.TAG_LONG)) {
                    output = Tool.suit(tag.getLong("output"));
                }
                if (tag.contains("save_stick", Tag.TAG_STRING)) {
                    String json = tag.getString("save_stick");
                    JsonArray array = JsonParser.parseString(json).getAsJsonArray();
                    itemList = Tool.toItemList(array);
                    blockList = Tool.toBlockList(array);
                }
            }
        }
        BigDecimal outputPerTick = new BigDecimal(output).divide(new BigDecimal(StorageFountainBlock.CARRY), 3, RoundingMode.HALF_UP);
        tooltip.add(Component.translatable("screen.alltheimbaium.fountain.output", outputPerTick));
        for (int i = 0; i < Math.min(itemList.size(), blockList.size()); i++) {
            Item item = itemList.get(i);
            Long block = blockList.get(i);
            String name = item.getName(new ItemStack(item)).getString();
            BigDecimal save = new BigDecimal(block).divide(new BigDecimal(StorageFountainBlock.CARRY), 3, RoundingMode.HALF_UP);
            tooltip.add(Component.translatable("screen.alltheimbaium.fountain.current", name, save));
        }
        if (itemList.isEmpty()) {
            tooltip.add(Component.translatable("screen.alltheimbaium.fountain.empty"));
        }
    }
}
