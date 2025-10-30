package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.StorageFountainConnection;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import java.util.ArrayList;
import java.util.List;

public class StorageFountainEntity extends BlockEntity implements ICapabilityProvider {
    private final LazyOptional<StorageFountainConnection> fecOptional = LazyOptional.of(() -> new StorageFountainConnection(this));
    public int findIndex = 0;
    public long output = 5;
    public List<ItemStack> itemList = new ArrayList<>();
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
        nbt.put("save_stick", Tool.toJsonArray(itemList, blockList));
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("output", Tag.TAG_LONG)) {
            this.output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("save_stick")) {
            ListTag list = (ListTag) nbt.get("save_stick");
            if (list != null) {
                this.itemList = Tool.toItemList(list);
                this.blockList = Tool.toBlockList(list);
                Tool.sort(itemList, blockList);
            }
        }
    }
}
