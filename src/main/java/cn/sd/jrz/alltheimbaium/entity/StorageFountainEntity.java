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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StorageFountainEntity extends BlockEntity implements ICapabilityProvider {
    private final LazyOptional<StorageFountainConnection> fecOptional = LazyOptional.of(() -> new StorageFountainConnection(this));
    public int findIndex = 0;
    public int tickCount = 0;
    public int level = 1;
    public ItemStack[] saveItems = new ItemStack[6];
    public ItemStack[] saveArray = new ItemStack[6];

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
        nbt.putInt("level", level);
        nbt.putString("save_item", Tool.fromArray(saveItems).toString());
        nbt.putString("save_array", Tool.fromArray(saveArray).toString());
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("level", Tag.TAG_INT)) {
            this.level = Tool.suitInt(nbt.getInt("level"));
        }
        if (nbt.contains("save_item", Tag.TAG_STRING)) {
            String json = nbt.getString("save_item");
            JsonArray a = JsonParser.parseString(json).getAsJsonArray();
            saveItems = Tool.toArray(a);
        }
        if (nbt.contains("save_array", Tag.TAG_STRING)) {
            String json = nbt.getString("save_array");
            JsonArray a = JsonParser.parseString(json).getAsJsonArray();
            saveArray = Tool.toArray(a);
        }
    }
}
