package cn.sd.jrz.alltheimbaium.item;

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
import java.util.List;

public class StorageFountainItem extends BlockItem {

    public StorageFountainItem(Block block) {
        super(block, new Properties().fireResistant());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        int level = 1;
        ItemStack[] saveItems = new ItemStack[6];
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag != null) {
                if (tag.contains("level", Tag.TAG_INT)) {
                    level = Tool.suitInt(tag.getInt("level"));
                }
                if (tag.contains("save_item", Tag.TAG_STRING)) {
                    String json = tag.getString("save_item");
                    JsonArray a = JsonParser.parseString(json).getAsJsonArray();
                    saveItems = Tool.toArray(a);
                }
            }
        }
        tooltip.add(Component.translatable("screen.alltheimbaium.farm.description", level));
        int oldSize = tooltip.size();
        for (ItemStack temp : saveItems) {
            if (temp == null || temp.isEmpty()) {
                continue;
            }
            Item item = temp.getItem();
            String name = item.getName(new ItemStack(item)).getString();
            int current = temp.getCount();
            tooltip.add(Component.translatable("screen.alltheimbaium.fountain.current", name, current));
        }
        if (oldSize == tooltip.size()) {
            tooltip.add(Component.translatable("screen.alltheimbaium.fountain.empty"));
        }
    }
}
