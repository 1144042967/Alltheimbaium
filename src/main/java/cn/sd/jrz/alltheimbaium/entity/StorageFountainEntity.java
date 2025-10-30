package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.StorageFountainConnection;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class StorageFountainEntity extends BlockEntity implements ICapabilityProvider {
    private final LazyOptional<StorageFountainConnection> fecOptional = LazyOptional.of(() -> new StorageFountainConnection(this));
    public int findIndex = 0;
    public long output = 5;
    public List<Item> itemList = new ArrayList<>();
    public List<Long> blockList = new ArrayList<>();
    public long tickCount = 0;

    public StorageFountainEntity(BlockPos pos, BlockState state) {
        super(Registration.STORAGE_FOUNTAIN_ENTITY.get(), pos, state);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        return capability == ForgeCapabilities.ITEM_HANDLER ? fecOptional.cast() : super.getCapability(capability, direction);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.putLong("output", output);
        nbt.putString("save_stick", Tool.toJsonArray(itemList, blockList).toString());
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("output", Tag.TAG_LONG)) {
            this.output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("save_stick", Tag.TAG_STRING)) {
            String json = nbt.getString("save_stick");
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            List<Item> tempItemList = Tool.toItemList(array);
            List<Long> tempBlockList = Tool.toBlockList(array);
            nextItem:
            for (int i = 0; i < Math.min(tempItemList.size(), tempBlockList.size()) && i < 9; i++) {
                Item item = tempItemList.get(i);
                Long block = tempBlockList.get(i);
                if (item == null || block == null) {
                    continue;
                }
                for (int index = 0; index < Math.min(itemList.size(), blockList.size()); index++) {
                    if (itemList.get(index) == item) {
                        blockList.set(index, blockList.get(index) + block);
                        continue nextItem;
                    }
                }
                itemList.add(item);
                blockList.add(block);
            }
            Tool.sort(itemList, blockList);
        }
    }
}
