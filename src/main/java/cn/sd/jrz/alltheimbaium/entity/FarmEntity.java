package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.FarmConnection;
import cn.sd.jrz.alltheimbaium.setup.DataConfig;
import cn.sd.jrz.alltheimbaium.setup.Tool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FarmEntity extends BlockEntity implements ICapabilityProvider {
    private static final Logger log = LoggerFactory.getLogger(FarmEntity.class);
    private final LazyOptional<FarmConnection> fecOptional = LazyOptional.of(() -> new FarmConnection(this));
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
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        try {
            return capability == ForgeCapabilities.ITEM_HANDLER ? fecOptional.cast() : super.getCapability(capability, direction);
        } catch (Throwable e) {
            log.error("FarmEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        try {
            nbt.putLong("level", level);
            nbt.putLongArray("output_array", outputArray);
            nbt.putLongArray("save_array", saveArray);
        } catch (Throwable e) {
            log.error("FarmEntity.saveAdditional error", e);
        }
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        try {
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
        } catch (Throwable e) {
            log.error("FarmEntity.load error", e);
        }
    }
}
