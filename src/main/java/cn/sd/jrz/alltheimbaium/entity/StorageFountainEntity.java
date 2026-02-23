package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StorageFountainEntity extends BlockEntity {
    public int findIndex = 0;
    public long output = 5;
    public List<ItemStack> itemList = new ArrayList<>();
    public List<Long> blockList = new ArrayList<>();
    public long tickCount = 0;

    public StorageFountainEntity(BlockPos pos, BlockState state) {
        super(Registration.STORAGE_FOUNTAIN_ENTITY.get(), pos, state);
    }

    @Override
    protected void applyImplicitComponents(@NotNull DataComponentInput input) {
        super.applyImplicitComponents(input);
        String blockData = input.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (blockData.isEmpty()) {
            return;
        }
        String[] dataArray = blockData.split("#,#");
        this.output = Tool.suit(dataArray[0]);
        this.itemList = Tool.fromItemString(dataArray[1]);
        this.blockList = Tool.fromBlockString(dataArray[2]);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder builder) {
        super.collectImplicitComponents(builder);
        String sb = output + "#,#" + Tool.toItemString(itemList) + "#,#" + Tool.toBlockString(blockList);
        builder.set(Registration.BLOCK_DATA.get(), sb);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putLong("output", output);
        nbt.putString("item_list", Tool.toItemString(itemList));
        nbt.putString("block_list", Tool.toBlockString(blockList));
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("output", Tag.TAG_LONG)) {
            this.output = Tool.suit(nbt.getLong("output"));
        }
        if (nbt.contains("item_list", Tag.TAG_STRING)) {
            this.itemList = Tool.fromItemString(nbt.getString("item_list"));
        }
        if (nbt.contains("block_list", Tag.TAG_STRING)) {
            this.blockList = Tool.fromBlockString(nbt.getString("block_list"));
        }
    }
}
