package cn.sd.jrz.alltheimbaium.entity;

import cn.sd.jrz.alltheimbaium.connection.ExtractionInterfaceConnection;
import cn.sd.jrz.alltheimbaium.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

/**
 * ATI 取出接口实体：无持久字段、无主动逻辑，仅向相邻方向暴露
 * {@link ExtractionInterfaceConnection}（只读 IItemHandler + IFluidHandler，聚合相邻本 MOD 机器）。
 * 方向无关：任意面访问能力返回同一聚合实例。
 */
public class ExtractionInterfaceEntity extends BlockEntity implements ICapabilityProvider {
    private static final Logger log = LoggerFactory.getLogger(ExtractionInterfaceEntity.class);

    private final LazyOptional<ExtractionInterfaceConnection> capability =
            LazyOptional.of(() -> new ExtractionInterfaceConnection(this));

    public ExtractionInterfaceEntity(BlockPos pos, BlockState state) {
        super(Registration.EXTRACTION_INTERFACE_ENTITY.get(), pos, state);
    }

    @Override
    @Nonnull
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction direction) {
        try {
            if (capability == ForgeCapabilities.ITEM_HANDLER || capability == ForgeCapabilities.FLUID_HANDLER) {
                return this.capability.cast();
            }
            return super.getCapability(capability, direction);
        } catch (Throwable e) {
            log.error("ExtractionInterfaceEntity.getCapability error", e);
        }
        return super.getCapability(capability, direction);
    }

    // ==================== NBT（无持久字段） ====================

    @Override
    public void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
    }

    // ==================== 客户端同步 ====================

    @Override
    @Nonnull
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(@Nonnull CompoundTag tag) {
        this.load(tag);
    }

    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(@Nonnull Connection net, @Nonnull ClientboundBlockEntityDataPacket pkt) {
        this.load(pkt.getTag());
    }
}
