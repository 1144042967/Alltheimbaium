package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.setup.DataConfig;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FarmEntity extends BlockEntity {
    public int findIndex = 0;
    public int tickCount = 0;
    public final DataConfig config;
    public long level;
    public long[] outputArray;
    public long[] saveArray;

    public FarmEntity(BlockPos pos, BlockState state, DataConfig config) {
        super(config.getType(), pos, state);
        this.config = config;
        this.level = 1;
        this.outputArray = new long[config.getProductList().size()];
        this.saveArray = new long[this.outputArray.length];
    }

    @Override
    protected void applyImplicitComponents(@NotNull DataComponentInput input) {
        super.applyImplicitComponents(input);
        String blockData = input.getOrDefault(Registration.BLOCK_DATA.get(), "");
        if (blockData.isEmpty()) {
            return;
        }
        String[] dataArray = blockData.split("#,#");
        if (dataArray.length < 3) {
            return;
        }
        this.level = Tool.suit(dataArray[0]);
        String[] tempArray = dataArray[1].split(",");
        for (int i = 0; i < tempArray.length && i < config.getProductList().size(); i++) {
            this.outputArray[i] = Tool.suit(tempArray[i]);
        }
        tempArray = dataArray[2].split(",");
        for (int i = 0; i < tempArray.length && i < config.getProductList().size(); i++) {
            this.saveArray[i] = Tool.suit(tempArray[i]);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.@NotNull Builder builder) {
        super.collectImplicitComponents(builder);
        StringBuilder sb = new StringBuilder();
        sb.append(level).append("#,#");
        for (int i = 0; i < outputArray.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(outputArray[i]);
        }
        sb.append("#,#");
        for (int i = 0; i < saveArray.length; i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(saveArray[i]);
        }
        builder.set(Registration.BLOCK_DATA.get(), sb.toString());
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putLong("level", level);
        nbt.putLongArray("output_array", outputArray);
        nbt.putLongArray("save_array", saveArray);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("level", Tag.TAG_LONG)) {
            this.level = Tool.suit(nbt.getLong("level"));
        }
        if (nbt.contains("output_array", Tag.TAG_LONG_ARRAY)) {
            long[] tempArray = nbt.getLongArray("output_array");
            for (int i = 0; i < tempArray.length && i < config.getProductList().size(); i++) {
                this.outputArray[i] = Tool.suit(tempArray[i]);
            }
        }
        if (nbt.contains("save_array", Tag.TAG_LONG_ARRAY)) {
            long[] tempArray = nbt.getLongArray("save_array");
            for (int i = 0; i < tempArray.length && i < config.getProductList().size(); i++) {
                this.saveArray[i] = Tool.suit(tempArray[i]);
            }
        }
    }
}
