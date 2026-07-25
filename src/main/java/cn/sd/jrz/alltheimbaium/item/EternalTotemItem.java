package cn.sd.jrz.alltheimbaium.item;

import cn.sd.jrz.alltheimbaium.setup.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.List;

public class EternalTotemItem extends Item {
    // 运行时状态，初始值由 loadConfig() 从配置文件读取
    public static boolean enabled;

    public EternalTotemItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant());
    }

    /**
     * 由 Config.onConfigLoad() 在配置文件加载完成后调用
     */
    public static void loadConfig() {
        enabled = Config.ETERNAL_TOTEM_DEFAULT_ENABLED.get();
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        if (!level.isClientSide) {
            enabled = !enabled;
            player.sendSystemMessage(Component.translatable(
                    enabled ? "chat.alltheimbaium.eternal_totem.enabled" : "chat.alltheimbaium.eternal_totem.disabled"
            ));
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
        tooltip.add(Component.translatable("item.alltheimbaium.eternal_totem.description"));
        super.appendHoverText(stack, context, tooltip, flagIn);
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack stack) {
        return true;
    }
}
